package com.vesperin.reflects;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Huascar Sanchez
 */
public class Loaders {
  private Loaders(){}

  /**
   * Creates a new Classloader for the given external file.
   *
   * @param jarFile external jar file
   * @return a new URLClassLoader object
   * @throws MalformedURLException malformed protocol detected.
   */
  public static ClassLoader externalJar(File jarFile) throws MalformedURLException {
    return new URLClassLoader(new URL[]{new URL("file:" + jarFile.toString())});
  }

  /**
   * @return a new ClassLoader for jars that are part of the JDK.
   */
  public static ClassLoader inJdkJars(){
    return ClassLoader.getSystemClassLoader();
  }
}
