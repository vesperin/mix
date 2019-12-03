package com.vesperin.reflects;

import com.vesperin.utils.Immutable;
import com.vesperin.utils.Strings;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class JarFinder {

  /**
   * Returns a set of jar files in Java's runtime
   */
  static Set<Jar> findJREJarFiles() throws IOException {
    return findJarFiles(jreLibPath());
  }

  /**
   * Returns a set of jar files in a given {@link Path java path}.
   */
  public static Set<Jar> findJarFiles(Path path) throws IOException {
    final boolean isDirectory = Files.isDirectory(path);
    final Path localPath = isDirectory ? path.toAbsolutePath() : path.toAbsolutePath().getParent();

    final ClassLoader classLoader = (localPath.startsWith(jreLibPath())
        ? ClassLoaders.inJdkJars() : ClassLoaders.externalJar(path.toFile())
    );

    final Walker walker = new Walker();
    final Map<File, ClassLoader> map = new LinkedHashMap<>();

    Files.walk(path).filter(ReflectConst.IS_JAR_FILE).map(Path::toFile).forEach(f -> map.put(f, classLoader));

    return findJarFiles(map, walker);
  }

  /**
   * Returns a list of jar files reachable from the given class loaders. A file and jar file are
   * reachable files if they are in URLs available from {@link URLClassLoader} instances
   * or the {@linkplain ClassLoader#getSystemClassLoader() system class loader}.
   *
   * @throws IOException if the attempt to read class path resources (jar files or directories)
   *     failed.
   */
  public static Set<Jar> findJarFiles(ClassLoader first, ClassLoader... rest) throws IOException {
    final Walker walker = new Walker();
    final Map<File, ClassLoader> map = new LinkedHashMap<>();

    final List<ClassLoader> loaders = Immutable.listOf(
        Stream.concat(Stream.of(first), Arrays.stream(rest)));

    for (ClassLoader each : loaders){
      map.putAll(getClassPathEntries(each));
    }

    return findJarFiles(map, walker);
  }

  static Set<Jar> findJarFiles(Map<File, ClassLoader> map,  Walker walker) throws IOException {
    for (Map.Entry<File, ClassLoader> entry : map.entrySet()) {
      walker.walk(entry.getKey(), entry.getValue());
    }

    return walker.jarFiles();
  }


  static Map<File, ClassLoader> getClassPathEntries(ClassLoader classloader) {
    Map<File, ClassLoader> entries = new LinkedHashMap<>();
    // Search parent first, since it's the order ClassLoader#loadClass() uses.
    ClassLoader parent = classloader.getParent();
    if (parent != null) {
      entries.putAll(getClassPathEntries(parent));
    }
    for (URL url : getClassLoaderUrls(classloader)) {
      if (url.getProtocol().equals("file")) {
        File file = toFile(url);
        if (!entries.containsKey(file)) {
          entries.put(file, classloader);
        }
      }
    }
    return Immutable.mapOf(entries);
  }

  private static List<URL> getClassLoaderUrls(ClassLoader classloader) {
    if (classloader instanceof URLClassLoader) {
      return Immutable.listOf(Arrays.stream(((URLClassLoader) classloader).getURLs()));
    }

    if (classloader.equals(ClassLoader.getSystemClassLoader())) {
      return parseJavaClassPath();
    }

    return Immutable.list();
  }

  /**
   * Returns the URLs in the class path specified by the {@code java.class.path} {@linkplain
   * System#getProperty system property}.
   */
  private static List<URL> parseJavaClassPath() {
    List<URL> urls = new ArrayList<>();
    for (String entry : SysInfo.JAVA_CLASS_PATH.value().split(SysInfo.PATH_SEPARATOR.value())) {
      try {
        try {
          urls.add(new File(entry).toURI().toURL());
        } catch (SecurityException e) { // File.toURI checks to see if the file is a directory
          urls.add(new URL("file", null, new File(entry).getAbsolutePath()));
        }
      } catch (MalformedURLException e) {
        // TODO(has) use proper logger
        System.out.println("malformed classpath entry: " + entry);
      }
    }
    return Immutable.listOf(urls);
  }

  private static File toFile(URL url) {
    final URL valid = Optional.ofNullable(url)
        .filter(e -> e.getProtocol().equals("file"))
        .orElseThrow(IllegalArgumentException::new);
    try {
      return new File(valid.toURI()); // Accepts escaped characters like %20.
    } catch (URISyntaxException e) {  // URL.toURI() doesn't escape chars.
      return new File(url.getPath()); // Accepts non-escaped chars like space.
    }
  }

  static Path jreLibPath(){
    final String jar = SysInfo.SUN_BOOT_PATH.value().split(SysInfo.PATH_SEPARATOR.value())[0];
    final String fileSeparator  = SysInfo.FILE_SEPARATOR.value();

    // Windows
    final String escapedFileSeparator = (fileSeparator.equals("\\")
        ? "\\\\"
        : fileSeparator
    );

    final String lib = jar.replaceFirst(
        escapedFileSeparator + "[^" + escapedFileSeparator + "]+$", ""
    );

    if(lib.isEmpty()){
      throw new IllegalArgumentException("Failed to get JRE lib path");
    }

    return Paths.get(lib);
  }


  private static Set<File> getClassPathFromManifest(File file, Manifest manifest) throws IOException {
    if (!Optional.ofNullable(manifest).isPresent()) return Immutable.set();
    if (!Strings.getFileExtension(file).filter(e -> !e.equals("zip")).isPresent()) return Immutable.set();

    final Set<File> files = new HashSet<>();

    String classpathAttribute = manifest.getMainAttributes()
        .getValue(Attributes.Name.CLASS_PATH.toString());

    if (classpathAttribute != null){
      for (String each : omitEmptyStrings(classpathAttribute.split(
          ReflectConst.CLASS_PATH_ATTRIBUTE_SEPARATOR))) {
        URL url;

        try {
          url = getClassPathEntry(file, each);
        } catch (MalformedURLException ignored){
          continue;
        }

        if ("file".equals(url.getProtocol())){
          files.add(toFile(url));
        }

      }
    }

    return Immutable.setOf(files);
  }

  /**
   * Returns the absolute URI of the Class-Path entry value as specified in <a
   * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
   * File Specification</a>. Both absolute URLs and relative URLs are supported.
   */
  private static URL getClassPathEntry(File jarFile, String path) throws MalformedURLException {
    return new URL(jarFile.toURI().toURL(), path);
  }

  private static List<String> omitEmptyStrings(String[] entries){
    return Immutable.listOf(Arrays.stream(entries).filter(s -> !s.isEmpty()));
  }

  static class Walker {
    private final Set<Jar> jarFiles = new HashSet<>();
    private final Set<File> seenFiles = new HashSet<>();

    Set<Jar> jarFiles(){
      return Immutable.setOf(jarFiles);
    }

    void walk(File file, ClassLoader classloader) throws IOException {
      if (seenFiles.add(file)/*this is False if file already exists in set*/){
        walkFile(file, classloader);
      }
    }

    private void walkFile(File file, ClassLoader classLoader) throws IOException {
      if (!file.exists()) return;

      if (file.isDirectory()){
        walkDirectory(file, classLoader);
      } else {
        walkJar(file, classLoader);
      }

    }

    private void walkDirectory(File directory, ClassLoader classLoader) throws IOException {
      walkDirectory(directory, classLoader, "");
    }

    private void walkDirectory(File directory, ClassLoader classLoader, String packPrefix) throws IOException {
      File[] files = Optional.ofNullable(directory.listFiles()).orElse(new File[0]);
      for (File each : files){
        final String name = each.getName();
        if (each.isDirectory()){
          walkDirectory(each, classLoader, packPrefix + name + "/");
        } else {
          walkJar(each, classLoader);
        }
      }
    }

    private void walkJar(File file, ClassLoader classLoader) throws IOException {
      JarFile jar;
      try {
        jar = new JarFile(file);
      } catch (IOException io){ // Not a jar file
        return;
      }

      final Jar made = makeJar(jar, file, classLoader);
      if (made.classes().isEmpty()) return;

      jarFiles.add(made);

      try {
        for (File path : getClassPathFromManifest(file, jar.getManifest())) {
          walk(path, classLoader);
        }
      } catch (IllegalStateException | IOException ignored) {}
    }

    private static Jar makeJar(final JarFile jar, File file, final ClassLoader classLoader) throws IOException {
      final Supplier<Stream<Class<?>>> lazyLoad = () -> jar.stream()
          .map(e -> jarEntryAsClass(e, classLoader))
          .flatMap(Walker::optionalToStream)
          .onClose(() -> {
            try {
              jar.close();
            } catch (IllegalStateException | IOException ignored) {}
          });
      return new Jar(file, Immutable.setOf(supplyStream(lazyLoad)));
    }

    private static Stream<Class<?>> supplyStream(Supplier<Stream<Class<?>>> supplier){
      // generate once and flat
      return Stream.generate(supplier)
          .limit(1)
          .flatMap(Function.identity());
    }


    private static <T, R extends Optional<T>> Stream<T> optionalToStream(R optional) {
      return optional.map(Stream::of).orElseGet(Stream::empty);
    }

    private static Optional<Class<?>> jarEntryAsClass(final JarEntry entry, final ClassLoader classLoader) {
      final JarEntry nonNullEntry = Objects.requireNonNull(entry);
      final ClassLoader nonNullClassloader = Objects.requireNonNull(classLoader);

      if (nonNullEntry.getName().endsWith(".class")) {
        try {

          final Class<?> aClass = Class.forName(pathToCanonicalName(nonNullEntry),
              false /*CLASS INIT NOT REQUIRED*/,
              nonNullClassloader);

          // We care deeply only about public classes
          if (JavaClass.isPublic(aClass)){
            return Optional.of(aClass);
          }

          return Optional.empty();
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
      }

      return Optional.empty();
    }

    private static String pathToCanonicalName(final JarEntry entry) {
      final String directoryPath = entry.getName();
      // path/to/Foo.class -> path.to.Foo
      return directoryPath.substring(0, directoryPath.length() - 6).replace('/', '.');
    }
  }

  static class Jar {
    File file;
    Set<Class<?>> classes;

    Jar(File file, Set<Class<?>> classes){
      this.file = file;
      this.classes = classes;
    }

    public File file(){
      return file;
    }

    public Set<Class<?>> classes(){
      return Immutable.setOf(classes);
    }
  }
}
