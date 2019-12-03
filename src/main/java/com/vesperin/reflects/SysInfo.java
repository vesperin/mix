package com.vesperin.reflects;

public enum SysInfo {
  SUN_BOOT_PATH("sun.boot.class.path"),

  /**
   * Java installation directory.
   */
  JAVA_HOME("java.home"),

  /**
   * Java Runtime Environment specification name.
   */
  JAVA_SPEC_NAME("java.specification.name"),

  /**
   * Java class path.
   */
  JAVA_CLASS_PATH("java.class.path"),

  /**
   * List of paths to search when loading libraries.
   */
  JAVA_LIBRARY_PATH("java.library.path"),

  /**
   * Default temp file path.
   */
  JAVA_IO_TMPDIR("java.io.tmpdir"),

  /**
   * Name of JIT compiler to use.
   */
  JAVA_COMPILER("java.compiler"),

  /**
   * Path of extension directory or directories.
   */
  JAVA_EXT_DIRS("java.ext.dirs"),

  /**
   * Operating system version.
   */
  OS_VERSION("os.version"),

  /**
   * File separator ("/" on UNIX).
   */
  FILE_SEPARATOR("file.separator"),

  /**
   * Path separator (":" on UNIX).
   */
  PATH_SEPARATOR("path.separator"),

  /**
   * Line separator ("\n" on UNIX).
   */
  LINE_SEPARATOR("line.separator"),

  /**
   * User's account name.
   */
  USER_NAME("user.name"),

  /**
   * User's home directory.
   */
  USER_HOME("user.home"),

  /**
   * User's current working directory.
   */
  USER_DIR("user.dir");

  private final String property;

  SysInfo(String property) {
    this.property = property;
  }

  public String property() {
    return property;
  }

  public String value() {
    return System.getProperty(property());
  }


  @Override public String toString() {
    return property() + "->" + value();
  }
}
