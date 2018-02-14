package com.vesperin.reflects;

import com.vesperin.utils.Expect;
import com.vesperin.utils.Immutable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author Huascar A. Sanchez
 */
public class ClasspathStream implements Stream<Class<?>> {

  private static final boolean CLASS_INITIALIZATION_NOT_REQUIRED = false;

  private static final Path JRE_LIB;
  private static final String SUN_BOOT_PATH = "sun.boot.class.path";
  private static final Predicate<Path> IS_JAR_FILE = (
    path -> path.toFile().getName().endsWith(".jar")
  );

  static {
    final String value = System.getProperty(SUN_BOOT_PATH);
    Objects.requireNonNull(value, "JRE lib directory not found");

    final String jar = value.split(System.getProperty("path.separator"))[0];
    final String fileSeparator  = System.getProperty("file.separator");

    // Windows
    final String escapedFileSeparator = (fileSeparator.equals("\\")
      ? "\\\\"
      : fileSeparator
    );

    // Remove last path such as "/rt.jar"
    final String lib = jar.replaceFirst(
      escapedFileSeparator + "[^" + escapedFileSeparator + "]+$", ""
    );

    if(Objects.isNull(lib) || lib.isEmpty()){
      throw new IllegalArgumentException("Failed to get JRE lib path");
    }

    JRE_LIB = Paths.get(lib);
  }

  private final Stream<Class<?>> source;

  /**
   * Construct a class stream object given a stream of classes (aka, the source).
   *
   * @param source stream of classes.
   */
  private ClasspathStream(Stream<Class<?>> source){
    Expect.validArgument(!Objects.isNull(source));
    this.source = source;
  }

  /**
   * Factory method that creates a new instance of a class stream given
   * a stream of classes as input.
   *
   * @param source stream of classes.
   * @return a new class stream object.
   */
  private static ClasspathStream from(final Stream<Class<?>> source) {
    return new ClasspathStream(source);
  }

  /**
   * Shortcut to {@code ClasspathStream.from(path.toFile())}.
   *
   * @param path the path represents a file as sources of {@code Class}es.
   * @return {@code ClasspathStream} instance to load {@code Class}es from the given {@code JarFile}.
   */
  private static ClasspathStream from(final Path path) {
    return from(path.toFile());
  }

  /**
   * Shortcut to {@code ClasspathStream.from(new JarFile(file))}.
   *
   * @param file the file as sources of {@code Class}es.
   * @return {@code ClasspathStream} instance to load {@code Class}es from the given {@code JarFile}.
   * @throws RuntimeException unexpected IOException has occurred. Wrapped in a RuntimeException.
   */
  private static ClasspathStream from(final File file) {
    Expect.nonNull(file);

    try {

      final boolean isDirectory = Files.isDirectory(file.toPath());
      final Path localPath = isDirectory ? file.toPath().toAbsolutePath() : file.toPath().toAbsolutePath().getParent();

      final ClassLoader classLoader = (localPath.startsWith(getJreLibPath())
        ? Loaders.inJdkJars() : Loaders.externalJar(file)
      );

      return from(new JarFile(file), classLoader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Path getJreLibPath() {
    return JRE_LIB;
  }

  static List<Class<?>> javaClasses(Path location){
    return Immutable.listOf(ClasspathStream.publicClasses(location));
  }

  static List<Class<?>> javaClasses(Set<String> allowedPackages) {

    final Path jreLibPath = ClasspathStream.getJreLibPath();
    if(jreLibPath == null) return Collections.emptyList();

    return ClasspathStream.publicClasses(jreLibPath)
      .filter(s -> allowedPackages.contains(s.getPackage().getName()))
      .collect(Collectors.toList());
  }

  private static Stream<Class<?>> publicClasses(final Path jreLibPath) {
    try {
      return Files.walk(jreLibPath)
          .filter(IS_JAR_FILE)
          .flatMap(ClasspathStream::from)
          .filter(ClassDefinition::isPublic);
    } catch (IOException e) {
      return Stream.empty();
    }
  }

  private static Stream<Class<?>> allClasses(final Path jreLibPath) {
    try {

      return Files.walk(jreLibPath)
        .filter(IS_JAR_FILE)
        .flatMap(ClasspathStream::from);

    } catch (IOException e) {
      return Stream.empty();
    }
  }

  /**
   * Create an instance from {@code JarFile} instance.
   * The returned instance closes the given {@code JarFile} on {@link Stream#close()}.
   *
   * @param jar the jar file as sources of {@code Class}es.
   * @return {@code ClasspathStream} instance to load {@code Class}es from the given {@code JarFile}.
   */
  private static ClasspathStream from(final JarFile jar, final ClassLoader classloader) {
    final Supplier<Stream<Class<?>>> lazyLoad = () -> jar.stream()
        .map(e -> ClasspathStream.jarEntryAsClass(e, classloader))
        .flatMap(ClasspathStream::optionalToStream)
        .onClose(() -> {
          try {
            jar.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

    return new ClasspathStream(supplyStream(lazyLoad));
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
    Expect.nonNull(entry);
    Expect.nonNull(classLoader);

    if (entry.getName().endsWith(".class")) {
      try {

        final Class<?> aClass = Class.forName(pathToCanonicalName(entry),
            CLASS_INITIALIZATION_NOT_REQUIRED,
            classLoader);
        return Optional.of(aClass);
      } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
    }

    return Optional.empty();
  }

  private static String pathToCanonicalName(final JarEntry entry) {
    final String directoryPath = entry.getName();
    // path/to/Foo.class -> path.to.Foo
    return directoryPath.substring(0, directoryPath.length() - 6).replace('/', '.');
  }

  @Override public ClasspathStream filter(final Predicate<? super Class<?>> predicate) {
    return ClasspathStream.from(source.filter(predicate));
  }

  @Override public <R> Stream<R> map(final Function<? super Class<?>, ? extends R> mapper) {
    return source.map(mapper);
  }

  @Override public IntStream mapToInt(final ToIntFunction<? super Class<?>> mapper) {
    return source.mapToInt(mapper);
  }

  @Override public LongStream mapToLong(final ToLongFunction<? super Class<?>> mapper) {
    return source.mapToLong(mapper);
  }

  @Override public DoubleStream mapToDouble(final ToDoubleFunction<? super Class<?>> mapper) {
    return source.mapToDouble(mapper);
  }

  @Override public <R> Stream<R> flatMap(final Function<? super Class<?>, ? extends Stream<? extends R>> mapper) {
    return source.flatMap(mapper);
  }

  @Override public IntStream flatMapToInt(final Function<? super Class<?>, ? extends IntStream> mapper) {
    return source.flatMapToInt(mapper);
  }

  @Override public LongStream flatMapToLong(final Function<? super Class<?>, ? extends LongStream> mapper) {
    return source.flatMapToLong(mapper);
  }

  @Override public DoubleStream flatMapToDouble(final Function<? super Class<?>, ? extends DoubleStream> mapper) {
    return source.flatMapToDouble(mapper);
  }

  @Override public ClasspathStream distinct() {
    return from(source.distinct());
  }

  @Override public ClasspathStream sorted() {
    return from(source.sorted());
  }

  @Override public ClasspathStream sorted(final Comparator<? super Class<?>> comparator) {
    return from(source.sorted(comparator));
  }

  @Override public ClasspathStream peek(final Consumer<? super Class<?>> action) {
    return from(source.peek(action));
  }

  @Override public ClasspathStream limit(final long maxSize) {
    return from(source.limit(maxSize));
  }

  @Override public ClasspathStream skip(final long n) {
    return from(source.skip(n));
  }

  @Override public void forEach(final Consumer<? super Class<?>> action) {
    source.forEach(action);
  }

  @Override public void forEachOrdered(final Consumer<? super Class<?>> action) {
    source.forEachOrdered(action);
  }

  @Override public Object[] toArray() {
    return source.toArray();
  }

  @Override public <A> A[] toArray(final IntFunction<A[]> generator) {
    return source.toArray(generator);
  }

  @Override public Class<?> reduce(final Class<?> identity, final BinaryOperator<Class<?>> accumulator) {
    return source.reduce(identity, accumulator);
  }

  @Override public Optional<Class<?>> reduce(final BinaryOperator<Class<?>> accumulator) {
    return source.reduce(accumulator);
  }

  @Override public <U> U reduce(
      final U identity,
      final BiFunction<U, ? super Class<?>, U> accumulator,
      final BinaryOperator<U> combiner) {

    return source.reduce(identity, accumulator, combiner);
  }

  @Override public <R> R collect(
      final Supplier<R> supplier,
      final BiConsumer<R, ? super Class<?>> accumulator,
      final BiConsumer<R, R> combiner) {

    return source.collect(supplier, accumulator, combiner);
  }

  @Override public <R, A> R collect(final Collector<? super Class<?>, A, R> collector) {
    return source.collect(collector);
  }

  @Override public Optional<Class<?>> min(final Comparator<? super Class<?>> comparator) {
    return source.min(comparator);
  }

  @Override public Optional<Class<?>> max(final Comparator<? super Class<?>> comparator) {
    return source.max(comparator);
  }

  @Override public long count() {
    return source.count();
  }

  @Override public boolean anyMatch(final Predicate<? super Class<?>> predicate) {
    return source.anyMatch(predicate);
  }

  @Override public boolean allMatch(final Predicate<? super Class<?>> predicate) {
    return source.allMatch(predicate);
  }

  @Override public boolean noneMatch(final Predicate<? super Class<?>> predicate) {
    return source.noneMatch(predicate);
  }

  @Override public Optional<Class<?>> findFirst() {
    return source.findFirst();
  }

  @Override public Optional<Class<?>> findAny() {
    return source.findAny();
  }

  @Override public Iterator<Class<?>> iterator() {
    return source.iterator();
  }

  @Override public Spliterator<Class<?>> spliterator() {
    return source.spliterator();
  }

  @Override public boolean isParallel() {
    return source.isParallel();
  }

  @Override public ClasspathStream sequential() {
    return from(source.sequential());
  }

  @Override public ClasspathStream parallel() {
    return from(source.parallel());
  }

  @Override public ClasspathStream unordered() {
    return from(source.unordered());
  }

  @Override public ClasspathStream onClose(final Runnable closeHandler) {
    return from(source.onClose(closeHandler));
  }

  @Override public void close() {
    source.close();
  }
}
