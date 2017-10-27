package com.vesperin.reflects;

import com.vesperin.utils.Immutable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Classpath {

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
    return Immutable.setOf(MethodDefinition.allMethods(eachClass)
      .filter(MethodDefinition.isRelevantMethodDefinition()));
  }

  public static Classpath getClasspath(){
    return new Classpath(Installer.CLASSES);
  }

  public Map<ClassDefinition, Set<MethodDefinition>> getClassToMethodsIndex(){
    return classToMethodsIndex;
  }

  public Map<ClassDefinition, Set<ClassDefinition>> getClassToSuperDefinitions(){
    return classToSuperDefinitions;
  }

  public Map<ClassDefinition, Set<ClassDefinition>> getClassToSubDefinitions(){
    return classToSubDefinitions;
  }

  public Map<String, Set<ClassDefinition>> getClassNameToDefinitionIndex(){
    return classNameToDefinitionIndex;
  }

  public Map<String, PackageDefinition> getPackageNameIndex(){
    return packageNameIndex;
  }

  public Map<PackageDefinition, Set<ClassDefinition>> getPackageToClassesIndex(){
    return packageToClassesIndex;
  }

  public Map<ClassDefinition, Set<PackageDefinition>> getClassToPackagesIndex(){
    return classToPackagesIndex;
  }

  public Map<String, ClassDefinition> getCanonicalNameToDefinition(){
    return canonicalNameToDefinition;
  }

  public synchronized Classpath clearClasspath(){
    getClassNameToDefinitionIndex().clear();
    getClassToMethodsIndex().clear();
    getPackageNameIndex().clear();
    getPackageToClassesIndex().clear();
    getClassToPackagesIndex().clear();

    return this;
  }



  // lazy loaded singleton
  static class Installer {
    static List<Class<?>> CLASSES = ClassCatcher.getClasspath();
  }

}
