package com.vesperin.reflects;

import com.vesperin.utils.Immutable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
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


  private final Map<String, ClassDefinition>                  canonicalNameToDefinition;
  private final Map<String, Set<ClassDefinition>>             classNameToDefinitionIndex;
  private final Map<ClassDefinition, Set<MethodDefinition>>   classToMethodsIndex;
  private final Map<String, PackageDefinition>                packageNameIndex;
  private final Map<PackageDefinition, Set<ClassDefinition>>  packageToClassesIndex;
  private final Map<ClassDefinition, Set<PackageDefinition>>  classToPackagesIndex;
  private final Map<ClassDefinition, Set<ClassDefinition>>    classToSuperDefinitions;
  private final Map<ClassDefinition, Set<ClassDefinition>>    classToSubDefinitions;

  /**
   * Creates a new Classpath object given a list of Java classes.
   *
   * @param classes Java classes
   */
  private Classpath(List<Class<?>> classes){
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
    return new Classpath(Installer.CLASSES);
  }

  /**
   * Creates a new classpath from the classes of some jar files
   * located at the given path.
   *
   * @param jarLocation path to one or many Jar files.
   * @return a new class path.
   */
  public static Classpath newClasspath(Path jarLocation){
    return new Classpath(ClassCatcher.getClasspath(jarLocation));
  }

  /**
   * Performs a union of two classpaths.
   *
   * @param paths the classpaths to be merged.
   * @return a new classpath
   */
  public static Classpath concat(Classpath... paths){
    final Classpath result = new Classpath(new ArrayList<>());

    for(Classpath each : paths){
      if(Objects.isNull(each)) continue;

      for(ClassDefinition eachDefinition : each.classToSuperDefinitions.keySet()){
        if(!result.classToSuperDefinitions.containsKey(eachDefinition)){
          result.classToSuperDefinitions.put(
            eachDefinition, each.classToSuperDefinitions.get(eachDefinition));
        }
      }

      for(ClassDefinition eachDefinition : each.classToSubDefinitions.keySet()){
        if(!result.classToSubDefinitions.containsKey(eachDefinition)){
          result.classToSubDefinitions.put(
            eachDefinition, each.classToSubDefinitions.get(eachDefinition));
        }
      }

      for(String canonicalName : each.canonicalNameToDefinition.keySet()){
        if(!result.canonicalNameToDefinition.containsKey(canonicalName)){
          result.canonicalNameToDefinition.put(
            canonicalName, each.canonicalNameToDefinition.get(canonicalName)
          );
        }
      }


      for(String typeName : each.classNameToDefinitionIndex.keySet()){
        final Set<ClassDefinition> touchedClasses = each.classNameToDefinitionIndex.get(typeName);
        if(result.classNameToDefinitionIndex.containsKey(typeName)){
          result.classNameToDefinitionIndex.get(typeName).addAll(touchedClasses);
        } else {
          result.classNameToDefinitionIndex.put(typeName, new HashSet<>(touchedClasses));
        }
      }

      for(ClassDefinition classDef : each.classToMethodsIndex.keySet()){
        final Set<MethodDefinition> touchedMethods = each.classToMethodsIndex.get(classDef);
        if(result.classToMethodsIndex.containsKey(classDef)){
          result.classToMethodsIndex.get(classDef).addAll(touchedMethods);
        } else {
          result.classToMethodsIndex.put(classDef, new HashSet<>(touchedMethods));
        }
      }


      for(String pkgDef : each.packageNameIndex.keySet()){
        if(!result.packageNameIndex.containsKey(pkgDef)){
          result.packageNameIndex.put(pkgDef, each.packageNameIndex.get(pkgDef));
        }
      }

      for(PackageDefinition pkgDef : each.packageToClassesIndex.keySet()){
        final Set<ClassDefinition> touchedImports = each.packageToClassesIndex.get(pkgDef);
        if(result.packageToClassesIndex.containsKey(pkgDef)){
          result.packageToClassesIndex.get(pkgDef).addAll(touchedImports);
        } else {
          result.packageToClassesIndex.put(pkgDef, new HashSet<>(touchedImports));
        }
      }

      for(ClassDefinition clsDef : each.classToPackagesIndex.keySet()){
        final Set<PackageDefinition> touchedClasses = each.classToPackagesIndex.get(clsDef);
        if(result.classToPackagesIndex.containsKey(clsDef)){
          result.classToPackagesIndex.get(clsDef).addAll(touchedClasses);
        } else {
          result.classToPackagesIndex.put(clsDef, new HashSet<>(touchedClasses));
        }
      }

    }


    return result;
  }

  /**
   * @return true if the classpath is empty; false otherwise.
   */
  public boolean isEmpty(){
    return getClassNameToDefinitionIndex().isEmpty();
  }

  private void buildIndices(List<Class<?>> classes){

    for(Class<?> eachClass : classes){
      final ClassDefinition definition = ClassDefinition.forceGeneric(eachClass);

      canonicalNameToDefinition.put(definition.getCanonicalName(), definition);

      if(!classNameToDefinitionIndex.containsKey(definition.getClassName())){
        final Set<ClassDefinition> setOfDefinitions = new HashSet<>();
        setOfDefinitions.add(definition);

        this.classNameToDefinitionIndex.put(
          definition.getClassName(),
          setOfDefinitions
        );

        classToMethodsIndex.put(definition, methodDefinitions(eachClass));
      } else {
        this.classNameToDefinitionIndex
          .get(definition.getClassName())
          .add(definition);

        if(!classToMethodsIndex.containsKey(definition)){
          classToMethodsIndex.put(definition, methodDefinitions(eachClass));
        }
      }

      final PackageDefinition pkgDef = definition.getPackageDefinition();

      if(!packageNameIndex.containsKey(pkgDef.getName())){
        packageNameIndex.put(pkgDef.getName(), pkgDef);

        final Set<ClassDefinition> first = new HashSet<>();
        first.add(definition);

        packageToClassesIndex.put(pkgDef, first);
      } else {

        packageToClassesIndex.get(pkgDef).add(definition);
      }

      if(!classToPackagesIndex.containsKey(definition)){

        final Set<PackageDefinition> pkgDefs = new HashSet<>();
        pkgDefs.add(pkgDef);

        classToPackagesIndex.put(definition, pkgDefs);
      } else {
        classToPackagesIndex.get(definition).add(pkgDef);
      }

      if(!classToSuperDefinitions.containsKey(definition)){
        final Set<ClassDefinition> supers = ClassDefinition.getSuperClassDefinitions(eachClass);

        classToSuperDefinitions.put(definition, new HashSet<>(supers));


        for(ClassDefinition eachSuper : supers){
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

  private static Set<MethodDefinition> methodDefinitions(Class<?> eachClass){
    return MethodDefinition.allMethods(eachClass)
      .filter(MethodDefinition.isRelevantMethodDefinition())
      .collect(Collectors.toSet());
  }

  /**
   * Recalls a set of method definitions contained in a class definition.
   *
   * @param classDefinition input class definition
   * @return a set of method definitions. This set can be an empty set.
   */
  public Set<MethodDefinition> methodSet(ClassDefinition classDefinition){
    if (!getClassToMethodsIndex().containsKey(classDefinition)) return Immutable.set();
    return getClassToMethodsIndex().get(classDefinition);
  }

  /**
   * @return all the method definitions in this classpath.
   */
  public Set<MethodDefinition> methodSet(){
    final Set<MethodDefinition> methodDefinitions = new HashSet<>();

    getClassToMethodsIndex()
      .values()
      .forEach(methodDefinitions::addAll);

    return methodDefinitions;
  }

  private Map<ClassDefinition, Set<MethodDefinition>> getClassToMethodsIndex(){
    return classToMethodsIndex;
  }

  /**
   * Recalls a set of super class definitions of a class definition.
   *
   * @param classDefinition input class definition
   * @return set of super class definitions
   */
  public Set<ClassDefinition> superClassSet(ClassDefinition classDefinition){
    if (!getClassToSuperDefinitions().containsKey(classDefinition)) return Immutable.set();
    return getClassToSuperDefinitions().get(classDefinition);
  }


  private Map<ClassDefinition, Set<ClassDefinition>> getClassToSuperDefinitions(){
    return classToSuperDefinitions;
  }

  /**
   * Recalls a set of sub class definitions of a class definition.
   *
   * @param classDefinition input class definition
   * @return set of sub class definitions
   */
  public Set<ClassDefinition> subClassSet(ClassDefinition classDefinition){
    if (!getClassToSubDefinitions().containsKey(classDefinition)) return Immutable.set();
    return getClassToSubDefinitions().get(classDefinition);
  }

  private Map<ClassDefinition, Set<ClassDefinition>> getClassToSubDefinitions(){
    return classToSubDefinitions;
  }

  /**
   * Tests for class name membership.
   *
   * @param classname name to check
   * @return true if the classname is tracked by this classpath; false otherwise.
   */
  public boolean containsClassname(String classname){
    return getClassNameToDefinitionIndex().containsKey(classname);
  }

  /**
   * Recalls a set of class definitions mapped to a class name
   *
   * @param className input class name
   * @return set of class definitions
   */
  public Set<ClassDefinition> classDefinitionSet(String className){
    if (!containsClassname(className)) return Immutable.set();
    return getClassNameToDefinitionIndex().get(className);
  }

  /**
   * @return all the class definitions in this classpath.
   */
  public Set<ClassDefinition> classDefinitionSet(){
    final Set<ClassDefinition> classDefinitions = new HashSet<>();

    getClassNameToDefinitionIndex()
      .values()
      .forEach(classDefinitions::addAll);

    return classDefinitions;
  }

  private Map<String, Set<ClassDefinition>> getClassNameToDefinitionIndex(){
    return classNameToDefinitionIndex;
  }

  /**
   * Recalls the package definition of an import (in String form)
   *
   * @param importName input import
   * @return a package definition matching the import (in String form)
   */
  public PackageDefinition importDefinition(String importName){
    if (!getPackageNameIndex().containsKey(importName)) return PackageDefinition.emptyPackage();
    return getPackageNameIndex().get(importName);
  }

  private Map<String, PackageDefinition> getPackageNameIndex(){
    return packageNameIndex;
  }


  /**
   * Recalls a set of class definitions contained in a package definition
   *
   * @param packageDefinition input package definition
   * @return set of class definitions
   */
  public Set<ClassDefinition> classDefinitionSet(PackageDefinition packageDefinition){
    if (!getPackageToClassesIndex().containsKey(packageDefinition)) return Immutable.set();
    return getPackageToClassesIndex().get(packageDefinition);
  }

  private Map<PackageDefinition, Set<ClassDefinition>> getPackageToClassesIndex(){
    return packageToClassesIndex;
  }

  /**
   * Inverted index between a class definition and the possible namespaces.
   *
   * @param classDefinition input class definition
   * @return set of class definitions
   */
  public Set<PackageDefinition> packageDefinitionSet(ClassDefinition classDefinition){
    if (!getClassToPackagesIndex().containsKey(classDefinition)) return Immutable.set();
    return getClassToPackagesIndex().get(classDefinition);
  }

  private Map<ClassDefinition, Set<PackageDefinition>> getClassToPackagesIndex(){
    return classToPackagesIndex;
  }


  /**
   * Recalls the class definition of a classname
   *
   * @param className input class name
   * @return a class definition matching the classname (in String form); or
   *    null if the definition is not found.
   */
  public ClassDefinition classDefinition(String className){
    if (!containsCanonicalClassname(className)) return null;
    return getCanonicalNameToDefinition().get(className);
  }

  /**
   * @return all the canonical class definitions in this classpath.
   */
  public Set<ClassDefinition> canonicalClassDefinitionSet(){
    return new HashSet<>(getCanonicalNameToDefinition()
      .values());
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

  private Map<String, ClassDefinition> getCanonicalNameToDefinition(){
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

  // lazy loaded singleton
  static class Installer {
    static List<Class<?>> CLASSES = ClassCatcher.getClasspath();
  }

}
