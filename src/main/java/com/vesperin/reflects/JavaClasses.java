package com.vesperin.reflects;

import com.vesperin.reflects.JarFinder.Jar;
import com.vesperin.utils.Immutable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

public class JavaClasses {

  public static Set<Class<?>> publicClassesKnownInJRE() throws IOException {
    return publicClasses(JarFinder.findJREJarFiles());
  }

  public static Set<Class<?>> publicClasses(Path jarPath) throws IOException {
    return publicClasses(JarFinder.findJarFiles(jarPath));
  }

  public static Set<Class<?>> publicClasses(ClassLoader first, ClassLoader... rest) throws IOException {
    return publicClasses(JarFinder.findJarFiles(first, rest));
  }

  public static Set<Class<?>> publicClasses(Set<Jar> jars){
    return publicClasses(jars, packagesKnownToCallersClassloader());
  }

  public static Set<Class<?>> publicClasses(Set<Jar> jars, Set<String> packs){
    Stream<Class<?>> classStream = jars.stream().flatMap(j -> j.classes.stream());
    if (packs.isEmpty()) return Immutable.setOf(classStream);

    return Immutable.setOf(classStream.filter(c -> packs.contains(c.getPackage().getName())));
  }

  public static Set<String> packagesKnownToCallersClassloader() {
    return Immutable.setOf(Arrays.stream(Package.getPackages())
        .map(Package::getName).filter(JdkPredicates::inJdk));
  }
}
