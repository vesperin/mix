package com.vesperin.reflects;

import com.vesperin.utils.Immutable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar A. Sanchez
 */
public class ClassCatcher {

  private static final Set<String> ALLOWED_PACKAGES;
  static {
    final Set<String> pkgs = new LinkedHashSet<>(
        Arrays.asList(
          "java.io", "java.math", "java.net", "java.nio",
          "java.text", "java.util", "java.sql", "org.w3c.dom",
          "java.lang", "javax.print", "javax.sound", "javax.imageio",
          "javax.swing", "java.awt", "javax.accessibility",
          "javax.xml", "javax.security", "javax.crypto",
          "java.security", "javax.script", "javax.sql",
          "org.xml.sax", "javax.jws", "java.applet",
          "javax.tools", "javax.management", "javax.transaction",
          "javax.net", "java.rmi", "javax.naming", "javax.activity",
          "java.beans", "javax.activation", "com.google.common",
          "com.google.common.annotations", "com.google.common.base",
          "com.google.common.collect", "com.google.common.cache",
          "com.google.common.escape", "com.google.common.eventbus",
          "com.google.common.hash", "com.google.common.html",
          "com.google.common.io", "com.google.common.math",
          "com.google.common.net", "com.google.common.primitives",
          "com.google.common.util.concurrent", "com.google.common.xml",
          "com.google.gson",
          "net.objecthunter.exp4j.tokenizer",
          "no.uib.cipr.matrix",
          "org.jscience.mathematics.number",
          "org.la4j",
          "org.nd4j.linalg.api.memory.enums",
          "org.nd4j.linalg.api.ndarray",
          "org.nd4j.linalg.api.ops.impl.accum",
          "org.nd4j.linalg.api.ops.impl.accum.distances",
          "org.nd4j.linalg.api.ops.impl.broadcast",
          "org.nd4j.linalg.api.ops.impl.transforms",
          "org.nd4j.linalg.api.ops.random.impl",
          "org.nd4j.linalg.dataset.api.preprocessor",
          "org.nd4j.linalg.indexing",
          "org.nd4j.linalg.learning.config",
          "org.nd4j.shade.jackson.databind",
          "org.ojalgo.matrix"
        )
    );

    ALLOWED_PACKAGES = Immutable.setOf(pkgs);
  }

  private List<Class<?>>  listOfClasses;
  private Set<String>     allowedPackages;


  private ClassCatcher(Set<String> allowedPackages){
    this.listOfClasses    = new LinkedList<>();
    this.allowedPackages  = Immutable.setOf(allowedPackages);
  }

  /**
   * Search for the resource with the extension in the classpath. Method
   * self-instantiate factory for every call to ensure thread safety.
   *
   * are required extension should be empty string. Null extension is not
   * allowed and will cause method to fail.
   *
   * @return List of all resources with specified extension.
   */
  private static List<Class<?>> catchClassesInClassPath(Set<String> allowedPackages) {
    ClassCatcher factory    = new ClassCatcher(allowedPackages);
    return factory.searchForAllowedClasses();
  }

  private static List<Class<?>> catchClassesInPath(Path jarsLocation) {
    ClassCatcher factory    = new ClassCatcher(Immutable.set());
    return factory.searchForClasses(jarsLocation);
  }

  public static List<Class<?>> getClasspath(){
    return ClassCatcher.getClasspath(ALLOWED_PACKAGES);
  }

  private static List<Class<?>> getClasspath(Set<String> allowedPackages){
    return ClassCatcher.catchClassesInClassPath(allowedPackages);
  }

  public static List<Class<?>> getClasspath(Path jarsLocation){
    return ClassCatcher.catchClassesInPath(jarsLocation);
  }

  private List<Class<?>> searchForClasses(Path jarsLocation){
    this.listOfClasses = ClasspathStream.javaClasses(jarsLocation);
    return listOfClasses;
  }

  /**
   * Search for the allowed classes in the classpath.
   *
   * @return List of all classes with specified extension.
   */
  private List<Class<?>> searchForAllowedClasses() {
    this.listOfClasses = Immutable.listOf(ClasspathStream.javaClasses(allowedPackages)
      .stream()
      .filter(JdkPredicates.inJdk()));

    return this.listOfClasses;
  }
}
