package com.vesperin.reflects;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class PackageDefinition {

  private static final Set<String> JAVA_LANG;

  static {
    final Set<String> lookUpSet = new HashSet<>();
    //Primitive types
    lookUpSet.add("Appendable");
    lookUpSet.add("AutoCloseable");
    lookUpSet.add("CharSequence");
    lookUpSet.add("Cloneable");
    lookUpSet.add("Comparable<T>");
    lookUpSet.add("Comparable");
    lookUpSet.add("Iterable<T>");
    lookUpSet.add("Iterable");
    lookUpSet.add("Readable");
    lookUpSet.add("Runnable");
    lookUpSet.add("Thread.UncaughtExceptionHandler");
    lookUpSet.add("Boolean");
    lookUpSet.add("Byte");
    lookUpSet.add("Character");
    lookUpSet.add("Character.Subset");
    lookUpSet.add("Character.UnicodeBlock");
    lookUpSet.add("Class<T>");
    lookUpSet.add("Class");
    lookUpSet.add("ClassLoader");
    lookUpSet.add("ClassValue<T>");
    lookUpSet.add("ClassValue");
    lookUpSet.add("Compiler");
    lookUpSet.add("Double");
    lookUpSet.add("Enum<E extends Enum<E>>");
    lookUpSet.add("Enum");
    lookUpSet.add("Float");
    lookUpSet.add("InheritableThreadLocal<T>");
    lookUpSet.add("InheritableThreadLocal");
    lookUpSet.add("Integer");
    lookUpSet.add("Long");
    lookUpSet.add("Math");
    lookUpSet.add("Number");
    lookUpSet.add("Object");
    lookUpSet.add("Package");
    lookUpSet.add("Process");
    lookUpSet.add("ProcessBuilder");
    lookUpSet.add("ProcessBuilder.Redirect");
    lookUpSet.add("Runtime");
    lookUpSet.add("RuntimePermission");
    lookUpSet.add("SecurityManager");
    lookUpSet.add("Short");
    lookUpSet.add("StackTraceElement");
    lookUpSet.add("StrictMath");
    lookUpSet.add("String");
    lookUpSet.add("StringBuffer");
    lookUpSet.add("StringBuilder");
    lookUpSet.add("System");
    lookUpSet.add("Thread");
    lookUpSet.add("ThreadGroup");
    lookUpSet.add("ThreadLocal<T>");
    lookUpSet.add("ThreadLocal");
    lookUpSet.add("Throwable");
    lookUpSet.add("Void");
    lookUpSet.add("Character.UnicodeScript");
    lookUpSet.add("ProcessBuilder.Redirect.Type");
    lookUpSet.add("Thread.State");
    lookUpSet.add("ArithmeticException");
    lookUpSet.add("ArrayIndexOutOfBoundsException");
    lookUpSet.add("ArrayStoreException");
    lookUpSet.add("ClassCastException");
    lookUpSet.add("ClassNotFoundException");
    lookUpSet.add("CloneNotSupportedException");
    lookUpSet.add("EnumConstantNotPresentException");
    lookUpSet.add("Exception");
    lookUpSet.add("IllegalAccessException");
    lookUpSet.add("IllegalArgumentException");
    lookUpSet.add("IllegalMonitorStateException");
    lookUpSet.add("IllegalStateException");
    lookUpSet.add("IllegalThreadStateException");
    lookUpSet.add("IndexOutOfBoundsException");
    lookUpSet.add("InstantiationException");
    lookUpSet.add("InterruptedException");
    lookUpSet.add("NegativeArraySizeException");
    lookUpSet.add("NoSuchFieldException");
    lookUpSet.add("NoSuchMethodException");
    lookUpSet.add("NullPointerException");
    lookUpSet.add("NumberFormatException");
    lookUpSet.add("ReflectiveOperationException");
    lookUpSet.add("RuntimeException");
    lookUpSet.add("SecurityException");
    lookUpSet.add("StringIndexOutOfBoundsException");
    lookUpSet.add("TypeNotPresentException");
    lookUpSet.add("UnsupportedOperationException");
    lookUpSet.add("AbstractMethodError");
    lookUpSet.add("AssertionError");
    lookUpSet.add("BootstrapMethodError");
    lookUpSet.add("ClassCircularityError");
    lookUpSet.add("ClassFormatError");
    lookUpSet.add("Error");
    lookUpSet.add("ExceptionInInitializerError");
    lookUpSet.add("IllegalAccessError");
    lookUpSet.add("IncompatibleClassChangeError");
    lookUpSet.add("InstantiationError");
    lookUpSet.add("InternalError");
    lookUpSet.add("LinkageError");
    lookUpSet.add("NoClassDefFoundError");
    lookUpSet.add("NoSuchFieldError");
    lookUpSet.add("NoSuchMethodError");
    lookUpSet.add("OutOfMemoryError");
    lookUpSet.add("StackOverflowError");
    lookUpSet.add("ThreadDeath");
    lookUpSet.add("UnknownError");
    lookUpSet.add("UnsatisfiedLinkError");
    lookUpSet.add("UnsupportedClassVersionError");
    lookUpSet.add("VerifyError");
    lookUpSet.add("VirtualMachineError");
    lookUpSet.add("Deprecated");
    lookUpSet.add("Override");
    lookUpSet.add("SafeVarargs");
    lookUpSet.add("SuppressWarnings");

    JAVA_LANG = Collections.unmodifiableSet(lookUpSet);
  }





  private static final String NOTHING       = "";
  private static final Pattern PACKAGE_NAME = Pattern.compile(
      "^(?<package>(?:\\w+\\.)+)\\w+[^.]"
  );

  private final String name;

  /**
   * Construct a new package definition.
   *
   * @param instance a package instance
   */
  private PackageDefinition(Package instance) {
    this.name = instance == null ? NOTHING : instance.getName();
  }

  /**
   * Construct a new package definition.
   *
   * @param instance name of package.
   */
  private PackageDefinition(String instance) {
    this.name = instance == null ? NOTHING : instance;
  }

  private static PackageDefinition emptyPackage(){
    return from(NOTHING);
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final PackageDefinition typeDef = (PackageDefinition) o;
    return getName().equals(typeDef.getName());
  }

  /**
   * Test if a type name belongs to the java.lang package.
   *
   * @param typename unqualified type name
   * @return true if the class is in the java lang projects.
   */
  public static boolean isJavaLang(String typename) {
    return typename != null
      && !typename.isEmpty()
      && JAVA_LANG.contains(typename);
  }

  private static PackageDefinition from(Class<?> cls){
    final Package pkg = cls.getPackage();
    if(pkg != null) return new PackageDefinition(pkg);
    return new PackageDefinition(NOTHING);
  }

  public static PackageDefinition from(String pkgString){
    return new PackageDefinition(pkgString);
  }

  public static PackageDefinition from(Type type) {
    if (type instanceof Class) {
      return from((Class<?>) type);
    }

    String typeName;
    try {
      typeName = type.getTypeName();
    } catch (TypeNotPresentException e){
      return emptyPackage();
    }

    final Matcher matcher = PACKAGE_NAME.matcher(typeName);

    if (matcher.find()) {
      final String pkg            = matcher.group("package");
      final String lastDotRemoved = pkg.substring(0, pkg.length() - 1);

      return from(lastDotRemoved);
    }

    return emptyPackage();
  }

  public String getName() {
    return name;
  }

  @Override public int hashCode() {
    return getName().hashCode();
  }

  @Override public String toString() {
    return name;
  }
}
