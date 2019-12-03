package com.vesperin.reflects;

import com.vesperin.utils.Immutable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Classpath holds classes needed for type resolution.
 */
public class Classpath {

  private static final List<Class<?>> PRIMITIVES = new ArrayList<>();
  static {
    PRIMITIVES.add(byte.class);
    PRIMITIVES.add(short.class);
    PRIMITIVES.add(int.class);
    PRIMITIVES.add(long.class);
    PRIMITIVES.add(float.class);
    PRIMITIVES.add(double.class);
    PRIMITIVES.add(boolean.class);
    PRIMITIVES.add(char.class);
  }


  private final Map<String, JavaClass> canonicalNameToDefinition;
  private final Map<String, Set<JavaClass>> classNameToDefinitionIndex;
  private final Map<JavaClass, Set<JavaMethod>> classToMethodsIndex;
  private final Map<String, JavaPack> packageNameIndex;
  private final Map<JavaPack, Set<JavaClass>> packageToClassesIndex;
  private final Map<JavaClass, Set<JavaPack>> classToPackagesIndex;
  private final Map<JavaClass, Set<JavaClass>> classToSuperDefinitions;
  private final Map<JavaClass, Set<JavaClass>> classToSubDefinitions;

  /**
   * Creates a new Classpath object given a list of Java classes.
   *
   * @param classes Java classes
   */
  private Classpath(Collection<Class<?>> classes){
    this.classNameToDefinitionIndex = new HashMap<>();
    this.canonicalNameToDefinition  = new HashMap<>();
    this.classToMethodsIndex        = new HashMap<>();
    this.packageNameIndex           = new HashMap<>();
    this.packageToClassesIndex      = new HashMap<>();
    this.classToPackagesIndex       = new HashMap<>();
    this.classToSuperDefinitions    = new HashMap<>();
    this.classToSubDefinitions      = new HashMap<>();

    if(classes != null && !classes.isEmpty()){
      buildIndices(classes);
    }

    // add primitives
    buildIndices(PRIMITIVES);
  }

  /**
   * @return a new and empty classpath.
   */
  public static Classpath emptyClasspath(){
    return new Classpath(Immutable.list());
  }

  /**
   * Creates a new (local) classpath.
   *
   * @return a new classpath object containing all JDK classes,
   *    as well as other classes that are in one's local classpath.
   */
  public static Classpath newClasspath(){
    return new Classpath(new ArrayList<>());
  }

  /**
   * Creates a new classpath from the classes of some jar files
   * located at the given path.
   *
   * @param jarLocation path to one or many Jar files.
   * @return a new class path.
   */
  public static Classpath newClasspath(Path jarLocation){
    Classpath classpath;
    try {
      classpath = new Classpath(JavaClasses.publicClasses(jarLocation));
    } catch (IOException ignored){
      classpath = emptyClasspath();
    }

    return classpath;
  }

  /**
   * Concatenates one or more classpaths.
   *
   * @param first classpath to be merged.
   * @param rest other classpaths to be merged.
   * @return a new classpath
   */
  public static Classpath concat(Classpath first, Classpath... rest){
    final Classpath result = new Classpath(new ArrayList<>());

    final Stream<Classpath> classpaths = Stream.concat(
        Stream.of(first), Arrays.stream(rest)).filter(Objects::nonNull);

    classpaths.forEach(cp -> {
      indexClassToSuperDefs(result, cp);
      indexClassToSubDefs(result, cp);
      indexQualNameToClassDef(result, cp);
      indexNameToClassDef(result, cp);
      indexClassToMethodDefs(result, cp);
      indexNameToPackDef(result, cp);
      indexPackToClassDefs(result, cp);
      indexClassToPackDefs(result, cp);
    });

    return result;
  }

  private static void indexClassToPackDefs(Classpath result, Classpath cp) {
    for(JavaClass clsDef : cp.classToPackagesIndex.keySet()){
      final Set<JavaPack> touchedClasses = cp.classToPackagesIndex.get(clsDef);
      if(result.classToPackagesIndex.containsKey(clsDef)){
        result.classToPackagesIndex.get(clsDef).addAll(touchedClasses);
      } else {
        result.classToPackagesIndex.put(clsDef, new HashSet<>(touchedClasses));
      }
    }
  }

  private static void indexPackToClassDefs(Classpath result, Classpath cp) {
    for(JavaPack pkgDef : cp.packageToClassesIndex.keySet()){
      final Set<JavaClass> touchedImports = cp.packageToClassesIndex.get(pkgDef);
      if(result.packageToClassesIndex.containsKey(pkgDef)){
        result.packageToClassesIndex.get(pkgDef).addAll(touchedImports);
      } else {
        result.packageToClassesIndex.put(pkgDef, new HashSet<>(touchedImports));
      }
    }
  }

  private static void indexNameToPackDef(Classpath result, Classpath cp) {
    for(String pkgDef : cp.packageNameIndex.keySet()){
      if(!result.packageNameIndex.containsKey(pkgDef)){
        result.packageNameIndex.put(pkgDef, cp.packageNameIndex.get(pkgDef));
      }
    }
  }

  private static void indexClassToMethodDefs(Classpath result, Classpath cp) {
    for(JavaClass javaClass : cp.classToMethodsIndex.keySet()){
      final Set<JavaMethod> touchedMethods = cp.classToMethodsIndex.get(javaClass);
      if(result.classToMethodsIndex.containsKey(javaClass)){
        result.classToMethodsIndex.get(javaClass).addAll(touchedMethods);
      } else {
        result.classToMethodsIndex.put(javaClass, new HashSet<>(touchedMethods));
      }
    }
  }

  private static void indexNameToClassDef(Classpath result, Classpath cp) {
    for(String typeName : cp.classNameToDefinitionIndex.keySet()){
      final Set<JavaClass> touchedClasses = cp.classNameToDefinitionIndex.get(typeName);
      if(result.classNameToDefinitionIndex.containsKey(typeName)){
        result.classNameToDefinitionIndex.get(typeName).addAll(touchedClasses);
      } else {
        result.classNameToDefinitionIndex.put(typeName, new HashSet<>(touchedClasses));
      }
    }
  }

  private static void indexQualNameToClassDef(Classpath result, Classpath cp) {
    for (String canonicalName : cp.canonicalNameToDefinition.keySet()) {
      if (!result.canonicalNameToDefinition.containsKey(canonicalName)) {
        result.canonicalNameToDefinition.put(
            canonicalName, cp.canonicalNameToDefinition.get(canonicalName)
        );
      }
    }
  }

  private static void indexClassToSubDefs(Classpath current, Classpath cp) {
    for(JavaClass eachDefinition : cp.classToSubDefinitions.keySet()){
      if(!current.classToSubDefinitions.containsKey(eachDefinition)){
        current.classToSubDefinitions.put(
            eachDefinition, cp.classToSubDefinitions.get(eachDefinition));
      }
    }
  }

  private static void indexClassToSuperDefs(Classpath current, Classpath cp) {
    for(JavaClass eachDefinition : cp.classToSuperDefinitions.keySet()){
      if(!current.classToSuperDefinitions.containsKey(eachDefinition)){
        current.classToSuperDefinitions.put(
            eachDefinition, cp.classToSuperDefinitions.get(eachDefinition));
      }
    }
  }

  /**
   * @return true if the classpath is empty; false otherwise.
   */
  public boolean isEmpty(){
    return getClassNameToDefinitionIndex().isEmpty();
  }

  private void buildIndices(Collection<Class<?>> classes){

    for(Class<?> eachClass : classes){
      final JavaClass definition = JavaClass.forceGeneric(eachClass);

      canonicalNameToDefinition.put(definition.getCanonicalName(), definition);

      if(!classNameToDefinitionIndex.containsKey(definition.getClassName())){
        final Set<JavaClass> setOfDefinitions = new HashSet<>();
        setOfDefinitions.add(definition);

        this.classNameToDefinitionIndex.put(
          definition.getClassName(),
          setOfDefinitions
        );

        classToMethodsIndex.put(definition, JavaMethod.declaredMethodDefinitions(eachClass));
      } else {
        this.classNameToDefinitionIndex
          .get(definition.getClassName())
          .add(definition);

        if(!classToMethodsIndex.containsKey(definition)){
          classToMethodsIndex.put(definition, JavaMethod.declaredMethodDefinitions(eachClass));
        }
      }

      final JavaPack pkgDef = definition.getJavaPack();

      if(!packageNameIndex.containsKey(pkgDef.getName())){
        packageNameIndex.put(pkgDef.getName(), pkgDef);

        final Set<JavaClass> first = new HashSet<>();
        first.add(definition);

        packageToClassesIndex.put(pkgDef, first);
      } else {

        packageToClassesIndex.get(pkgDef).add(definition);
      }

      if(!classToPackagesIndex.containsKey(definition)){

        final Set<JavaPack> pkgDefs = new HashSet<>();
        pkgDefs.add(pkgDef);

        classToPackagesIndex.put(definition, pkgDefs);
      } else {
        classToPackagesIndex.get(definition).add(pkgDef);
      }

      if(!classToSuperDefinitions.containsKey(definition)){
        final Set<JavaClass> supers = JavaClass.getSuperClassDefinitions(eachClass);

        classToSuperDefinitions.put(definition, new HashSet<>(supers));


        for(JavaClass eachSuper : supers){
          if(classToSubDefinitions.containsKey(eachSuper)){
            classToSubDefinitions.get(eachSuper).add(definition);
          } else {
            classToSubDefinitions.put(
              eachSuper, new HashSet<>(Collections.singleton(definition))
            );
          }
        }

      }
    }
  }

  /**
   * Recalls a set of method definitions contained in a class definition.
   *
   * @param javaClass input class definition
   * @return a set of method definitions. This set can be an empty set.
   */
  public Set<JavaMethod> methodSet(JavaClass javaClass){
    if (!getClassToMethodsIndex().containsKey(javaClass)) return Immutable.set();
    return getClassToMethodsIndex().get(javaClass);
  }

  /**
   * @return all the method definitions in this classpath.
   */
  public Set<JavaMethod> methodSet(){
    final Set<JavaMethod> javaMethods = new HashSet<>();

    getClassToMethodsIndex()
      .values()
      .forEach(javaMethods::addAll);

    return javaMethods;
  }

  private Map<JavaClass, Set<JavaMethod>> getClassToMethodsIndex(){
    return classToMethodsIndex;
  }

  /**
   * Recalls a set of super class definitions of a class definition.
   *
   * @param javaClass input class definition
   * @return set of super class definitions
   */
  public Set<JavaClass> superClassSet(JavaClass javaClass){
    if (!getClassToSuperDefinitions().containsKey(javaClass)) return Immutable.set();
    return getClassToSuperDefinitions().get(javaClass);
  }


  private Map<JavaClass, Set<JavaClass>> getClassToSuperDefinitions(){
    return classToSuperDefinitions;
  }

  /**
   * Recalls a set of sub class definitions of a class definition.
   *
   * @param javaClass input class definition
   * @return set of sub class definitions
   */
  public Set<JavaClass> subClassSet(JavaClass javaClass){
    if (!getClassToSubDefinitions().containsKey(javaClass)) return Immutable.set();
    return getClassToSubDefinitions().get(javaClass);
  }

  private Map<JavaClass, Set<JavaClass>> getClassToSubDefinitions(){
    return classToSubDefinitions;
  }

  /**
   * Tests for class name membership.
   *
   * @param classname name to check
   * @return true if the classname is tracked by this classpath; false otherwise.
   */
  public boolean containsJavaClass(String classname){
    return getClassNameToDefinitionIndex().containsKey(classname);
  }

  /**
   * Recalls a set of class definitions mapped to a class name
   *
   * @param className input class name
   * @return set of class definitions
   */
  public Set<JavaClass> classDefinitionSet(String className){
    if (!containsJavaClass(className)) return Immutable.set();
    return getClassNameToDefinitionIndex().get(className);
  }

  /**
   * @return all the class definitions in this classpath.
   */
  public Set<JavaClass> classDefinitionSet(){
    final Set<JavaClass> javaClasses = new HashSet<>();

    getClassNameToDefinitionIndex()
      .values()
      .forEach(javaClasses::addAll);

    return javaClasses;
  }

  private Map<String, Set<JavaClass>> getClassNameToDefinitionIndex(){
    return classNameToDefinitionIndex;
  }

  /**
   * Recalls the package definition of an import (in String form)
   *
   * @param importName input import
   * @return a package definition matching the import (in String form)
   */
  public JavaPack importDefinition(String importName){
    if (!getPackageNameIndex().containsKey(importName)) return JavaPack.emptyPackage();
    return getPackageNameIndex().get(importName);
  }

  private Map<String, JavaPack> getPackageNameIndex(){
    return packageNameIndex;
  }


  /**
   * Recalls a set of class definitions contained in a package definition
   *
   * @param javaPack input package definition
   * @return set of class definitions
   */
  public Set<JavaClass> classDefinitionSet(JavaPack javaPack){
    if (!getPackageToClassesIndex().containsKey(javaPack)) return Immutable.set();
    return getPackageToClassesIndex().get(javaPack);
  }

  private Map<JavaPack, Set<JavaClass>> getPackageToClassesIndex(){
    return packageToClassesIndex;
  }

  /**
   * Inverted index between a class definition and the possible namespaces.
   *
   * @param javaClass input class definition
   * @return set of class definitions
   */
  public Set<JavaPack> packageDefinitionSet(JavaClass javaClass){
    if (!getClassToPackagesIndex().containsKey(javaClass)) return Immutable.set();
    return getClassToPackagesIndex().get(javaClass);
  }

  private Map<JavaClass, Set<JavaPack>> getClassToPackagesIndex(){
    return classToPackagesIndex;
  }


  /**
   * Recalls the class definition of a classname
   *
   * @param className input class name
   * @return a class definition matching the classname (in String form); or
   *    null if the definition is not found.
   */
  public JavaClass classDefinition(String className){
    if (!containsCanonicalClassname(className)) return null;
    return getCanonicalNameToDefinition().get(className);
  }

  /**
   * @return all the canonical class definitions in this classpath.
   */
  public Set<JavaClass> canonicalClassDefinitionSet(){
    return Immutable.setOf(getCanonicalNameToDefinition().values());
  }

  /**
   * Tests for class name membership.
   *
   * @param classname name to check
   * @return true if the classname is tracked by this classpath; false otherwise.
   */
  public boolean containsCanonicalClassname(String classname){
    return getCanonicalNameToDefinition().containsKey(classname);
  }

  private Map<String, JavaClass> getCanonicalNameToDefinition(){
    return canonicalNameToDefinition;
  }

  public synchronized Classpath clear(){
    getClassNameToDefinitionIndex().clear();
    getClassToMethodsIndex().clear();
    getPackageNameIndex().clear();
    getPackageToClassesIndex().clear();
    getClassToPackagesIndex().clear();

    return this;
  }

  /**
   * @return the number of classes tracked by this classpath.
   */
  public int size(){
    return getCanonicalNameToDefinition().size();
  }

  @Override public String toString() {
    return "Classpath(" + size() + " classes)";
  }
}
