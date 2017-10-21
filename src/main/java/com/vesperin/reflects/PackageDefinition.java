package com.vesperin.reflects;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class PackageDefinition {
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
