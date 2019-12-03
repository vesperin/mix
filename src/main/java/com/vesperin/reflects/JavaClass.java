package com.vesperin.reflects;

import com.vesperin.base.CommonJdt;
import com.vesperin.utils.Immutable;
import com.vesperin.utils.Sets;
import com.vesperin.utils.Strings;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * JavaClass defines a class object
 */
public class JavaClass {

  private static final Map<String, String> PRIMITIVE_TO_OBJECT;

  static {
    final Map<String, String> lookUpTable = new HashMap<>();
    //Primitive types
    lookUpTable.put("byte", "java.lang.Byte");
    lookUpTable.put("short", "java.lang.Short");
    lookUpTable.put("int", "java.lang.Integer");
    lookUpTable.put("long", "java.lang.Long");
    lookUpTable.put("float", "java.lang.Float");
    lookUpTable.put("double", "java.lang.Double");
    lookUpTable.put("boolean", "java.lang.Boolean");
    lookUpTable.put("char", "java.lang.Character");
    lookUpTable.put("byte[]", "java.lang.Byte[]");
    lookUpTable.put("short[]", "java.lang.Short[]");
    lookUpTable.put("int[]", "java.lang.Integer[]");
    lookUpTable.put("long[]", "java.lang.Long[]");
    lookUpTable.put("float[]", "java.lang.Float[]");
    lookUpTable.put("double[]", "java.lang.Double[]");
    lookUpTable.put("boolean[]", "java.lang.Boolean[]");
    lookUpTable.put("char[]", "java.lang.Character[]");

    PRIMITIVE_TO_OBJECT = Collections.unmodifiableMap(lookUpTable);
  }


  private final JavaPack javaPack;
  private final String typeName;
  private final String className;
  private final String simpleForm;
  private final TypeLiteral typeLiteral;
  private final boolean isDeprecated;
  private final boolean isAbstract;
  private final String reifiedCanonicalName;
  private final Set<JavaAnnotation> annotations;
  private final String canonicalName;

  /**
   * Construct a new class definition for a given {@link Type}
   *
   * @param type the {@link Type} of interest.
   */
  private JavaClass(Type type) {
    this(type, ReflectConst.USE_LAMBDA_EXPRESSION);
  }

  /**
   * Construct a new class definition for a given {@link Type}
   *
   * @param type        the {@link Type} of interest.
   * @param annotations type annotations
   */
  private JavaClass(Type type, Set<JavaAnnotation> annotations) {
    this(type, notDotsPackageDefinitionFunction(type), ReflectConst.USE_LAMBDA_EXPRESSION, annotations);
  }

  /**
   * Construct a new class definition for a given {@link Type} and lambda expression check.
   *
   * @param type the {@link Type} of interest.
   * @param flag whether this class uses lambda expressions or not.
   */
  private JavaClass(Type type, int flag) {
    this(type, notDotsPackageDefinitionFunction(type), flag);
  }

  /**
   * Construct a new class definition for a given {@link Type}, filter flags, generic representation
   * of the class.
   *
   * @param type          the {@link Type} of interest.
   * @param genericString string representation of class.
   * @param flag          whether this class uses lambda expressions or not.
   */
  private JavaClass(Type type, String genericString, int flag) {
    this(type, onlyFewPackageDefinitionFunction(genericString), flag, new HashSet<>());
  }

  private JavaClass(Type type, String genericString, int flag,
      Set<JavaAnnotation> annotations) {
    this(type, onlyFewPackageDefinitionFunction(genericString), flag, annotations);
  }

  private JavaClass(Type type, Function<JavaPack, String> pkgFunction, int flag) {
    this(type, pkgFunction, flag, new HashSet<>());
  }

  /**
   * Construct a new class definition.
   *
   * @param type        the {@link Type} of interest.
   * @param pkgFunction package definition function.
   * @param flag        whether this class uses lambda expressions or not.
   * @param annotations type annotations
   */
  private JavaClass(Type type, Function<JavaPack, String> pkgFunction, int flag,
      Set<JavaAnnotation> annotations) {

    this.javaPack = JavaPack.from(type);

    this.typeLiteral = TypeLiteral.from(type);
    this.isAbstract = isAbstract(type);

    this.annotations = new HashSet<>(annotations);

    if (typeLiteral == TypeLiteral.VOID) {
      this.typeName = "()";
      this.simpleForm = "()";
      this.canonicalName = "()";
      this.reifiedCanonicalName = "()";
    } else {
      this.typeName = pkgFunction.apply(javaPack);

      if (TypeLiteral.FUNCTIONAL_INTERFACE == typeLiteral && ReflectConst.USE_LAMBDA_EXPRESSION == flag) {
        this.simpleForm = lambdaIfFunctionalInterface(type)
            .orElse(typeName);
      } else {
        this.simpleForm = typeName;
      }

      final String toStringAnnotation = (annotations.isEmpty()
          ? "" : annotations.toString() + " ");

      final String packageToString = javaPack.toString();
      this.canonicalName = (packageToString.isEmpty()
          ? (toStringAnnotation) + typeName
          : (toStringAnnotation) + packageToString + "." + this.typeName
      );

      this.reifiedCanonicalName = canonicalName.replace(genericsSubstring(canonicalName), "");
    }

    this.isDeprecated = isDeprecated(type);

    this.className = (typeName.contains("<") && typeName.contains(">")
        ? typeName.replace(genericsSubstring(typeName), "")
        : typeName);

  }

  /**
   * Construct a new class definition.
   *
   * @param pkgDef       package definition
   * @param typeName     the type this definition represents.
   * @param isDeprecated true if this class is deprecated; false otherwise.
   * @param isAbstract   true if this class is an abstract class; false otherwise.
   */
  private JavaClass(JavaPack pkgDef, String typeName,
      boolean isDeprecated, boolean isAbstract, Set<JavaAnnotation> annotations) {

    this.javaPack = pkgDef;
    this.typeLiteral = TypeLiteral.voidOrClass(typeName);
    this.isAbstract = isAbstract;
    this.isDeprecated = isDeprecated;
    this.typeName = typeName;
    this.simpleForm = this.typeName;

    this.className = (this.typeName.contains("<") && this.typeName.contains(">")
        ? typeName.replace("<(.+?)>", "")
        : typeName);

    this.annotations = new HashSet<>(annotations);

    final String toStringAnnotation = (annotations.isEmpty()
        ? "" : annotations.toString() + " ");

    final String packageToString = this.javaPack.toString();
    this.canonicalName = (packageToString.isEmpty()
        ? (toStringAnnotation) + typeName
        : (toStringAnnotation) + packageToString + "." + this.typeName
    );

    this.reifiedCanonicalName = this.canonicalName
        .replace(
            genericsSubstring(this.canonicalName), ""
        );
  }

  private static Function<JavaPack, String> notDotsPackageDefinitionFunction(Type type) {
    return (packageDef) -> getTypeName(type)
        .filter(t -> t.contains("."))
        .map(t -> t.replace(packageDef.getName() + ".", ""))
        .orElse(ReflectConst.MISSING);
  }

  static String getTypeName(Type type, String alternative){
    return getTypeName(type).orElse(alternative);
  }


  static Optional<String> getTypeName(Type type) {
    try {
      return Optional.of(type.getTypeName());
    } catch (TypeNotPresentException ignored) {
      return Optional.empty();
    }
  }

  private static Function<JavaPack, String> onlyFewPackageDefinitionFunction(
      String genericString) {
    return (packageDef) -> {
      final String genericTypeName = ReflectConst.EXCLUDING_MODIFIERS.matcher(genericString).replaceAll("");
      return genericTypeName.replace(packageDef.getName() + ".", "");
    };
  }

  /**
   * Test if the type name is a primitive type
   *
   * @param typeName type name
   * @return true if it is a primitive type; false otherwise.
   */
  public static boolean isPrimitive(String typeName) {
    return PRIMITIVE_TO_OBJECT.containsKey(Objects.requireNonNull(typeName));
  }

  /**
   * Converts a primitive type into its object equivalent.
   *
   * @param typeName type name
   * @return object equivalent of primitive type.
   */
  public static String objectifyPrimitiveType(String typeName) {
    return PRIMITIVE_TO_OBJECT.get(typeName);
  }

  private static boolean isDeprecated(Type type) {
    if (type instanceof Class<?>) {
      return ((Class<?>) type).getAnnotation(Deprecated.class) != null;
    } else if (type instanceof ParameterizedType) {
      try {
        final Type rawType = ((ParameterizedType) type).getRawType();
        return ((Class<?>) rawType).getAnnotation(Deprecated.class) != null;
      } catch (ClassCastException e) {
        return false;
      }
    } else {
      return false;
    }
  }


  private static boolean isAbstract(Type type) {
    if (type instanceof Class<?>) {
      return Modifier.isAbstract(((Class<?>) type).getModifiers());
    } else if (type instanceof ParameterizedType) {
      try {
        final Type rawType = ((ParameterizedType) type).getRawType();
        return Modifier.isAbstract(((Class<?>) rawType).getModifiers());
      } catch (ClassCastException e) {
        return false;
      }
    } else {
      return false;
    }
  }

  public static JavaClass from(
      String pkgString, String candidateTypeName, boolean isDeprecated, boolean isAbstract) {

    return from(pkgString, candidateTypeName, isDeprecated, isAbstract, new HashSet<>());
  }


  public static JavaClass from(String pack, String candidateTypeName,
      boolean isDeprecated, boolean isAbstract, Set<JavaAnnotation> annotations) {

    if ("void".equals(candidateTypeName)) {
      return JavaClass.voidClassDefinition();
    }

    final String[] packageAndTypeName = perfectCouple(pack, candidateTypeName);

    final String packageName = packageAndTypeName[0];
    final String typeName = packageAndTypeName[1];

    assert (typeName != null && !typeName.isEmpty());

    final JavaPack pkgDef = JavaPack.from(packageName);

    return new JavaClass(pkgDef, typeName, isDeprecated, isAbstract, annotations);
  }

  public static JavaClass from(Type type) {
    return from(type, new HashSet<>());
  }

  public static JavaClass from(Type type, Set<JavaAnnotation> annotations) {
    if (type instanceof ParameterizedType || type instanceof TypeVariable ||
        type instanceof GenericArrayType || type instanceof Class) {
      return new JavaClass(type, annotations);
    } else {
      throw new IllegalArgumentException("unknown subtype of Type: " + type.getClass());
    }
  }

  public static JavaClass normalizedClassDefinition(CompilationUnit unit,
      ITypeBinding typeBinding) {
    final Optional<ITypeBinding> normalized = Optional
        .ofNullable(CommonJdt.normalizeTypeBinding(Objects.requireNonNull(typeBinding)));

    return classDefinition(unit, normalized.orElse(typeBinding));
  }

  public static JavaClass classDefinition(CompilationUnit unit, ITypeBinding typeBinding) {
    if (unit == null) {
      throw new IllegalArgumentException("compilation unit is null");
    }
    if (typeBinding == null) {
      throw new IllegalArgumentException("type binding is null");
    }

    final String typeName = typeBinding.getName();
    String pkgDef;

    if (typeBinding.toString().contains(ReflectConst.MISSING)) {
      pkgDef = packageName(unit, typeName);
    } else {
      final Optional<IPackageBinding> packageBinding = Optional
          .ofNullable(typeBinding.getPackage());
      pkgDef = packageBinding.isPresent() ? packageBinding.get().getName() : "";
    }

    final boolean isClassDeprecated = typeBinding.isDeprecated();
    final boolean isClassAbstract = Modifier.isAbstract(typeBinding.getModifiers());

    final Set<JavaAnnotation> annotations = Immutable.setOf(
        Arrays.stream(typeBinding.getTypeAnnotations())
            .map(JavaAnnotation::annotationDefinition)
    );

    return JavaClass.from(pkgDef, typeName, isClassDeprecated, isClassAbstract, annotations);
  }

  static JavaClass classDefinition(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      throw new IllegalArgumentException("type binding is null");
    }

    final String typeName = typeBinding.isAnonymous() ? typeBinding.getDeclaringClass().getName()
        : typeBinding.getName();
    String pkgDef;

    if (typeBinding.toString().contains(ReflectConst.MISSING)) {
      pkgDef = JavaPack.isJavaLang(typeName)
          ? ReflectConst.JAVA_LANG_NAMESPACE
          : ReflectConst.DEFAULT_NAMESPACE;
    } else {
      final Optional<IPackageBinding> packageBinding = Optional
          .ofNullable(typeBinding.getPackage());

      pkgDef = packageBinding.isPresent() ? packageBinding.get().getName() : ReflectConst.DEFAULT_NAMESPACE;

      if (pkgDef.isEmpty()) {
        pkgDef = JavaPack.isJavaLang(typeName)
            ? ReflectConst.JAVA_LANG_NAMESPACE
            : pkgDef;
      }
    }

    final boolean isClassDeprecated = typeBinding.isDeprecated();
    final boolean isClassAbstract = Modifier.isAbstract(typeBinding.getModifiers());

    return JavaClass.from(pkgDef, typeName, isClassDeprecated, isClassAbstract);
  }

  static JavaClass returnClassDefinition(CompilationUnit unit, IMethodBinding methodBinding) {
    return classDefinition(unit, methodBinding.getReturnType());
  }


  static JavaClass classDefinition(CompilationUnit unit, IMethodBinding methodBinding) {
    return classDefinition(unit, methodBinding.getDeclaringClass());
  }

  static Set<String> unitImports(CompilationUnit unit) {
    return Immutable.setOf(
        CommonJdt.typeSafeList(ImportDeclaration.class, unit.imports())
            .stream().map(i -> i.getName().getFullyQualifiedName())
    );
  }

  private static String packageName(CompilationUnit unit, String typeName) {
    final Set<String> imports = unitImports(unit);

    for (String eachImport : imports) {
      if (eachImport.contains(typeName)) {
        return eachImport.replace("." + typeName, "");
      }
    }

    return JavaPack.isJavaLang(typeName)
        ? ReflectConst.JAVA_LANG_NAMESPACE
        : ReflectConst.DEFAULT_NAMESPACE;
  }

  private static String[] perfectCouple(String pkgString, String candidateTypeName) {
    final String[] result = new String[2];

    String packageName;
    String typeName = Objects.requireNonNull(candidateTypeName);
    if (isPrimitive(typeName) && "".equals(pkgString)) {
      packageName = pkgString;
      result[0] = packageName;
      result[1] = typeName;

    } else {
      packageName = pkgString;
      result[0] = packageName;
      result[1] = typeName;
    }

    return result;
  }

  private static JavaClass voidClassDefinition() {
    return new JavaClass(
        JavaPack.from("java.lang"),
        "Void",
        false,
        false,
        Immutable.set()
    );
  }

  static JavaClass missingClassDefinition() {
    return new JavaClass(
        JavaPack.emptyPackage(),
        ReflectConst.MISSING,
        false,
        false,
        Immutable.set()
    );
  }

  private static JavaClass forceClassNameFormEvenIfFunctionalInterface(Type type) {
    if (type instanceof ParameterizedType || type instanceof TypeVariable ||
        type instanceof GenericArrayType || type instanceof Class<?>) {
      return new JavaClass(type, ReflectConst.NOT_USE_LAMBDA_EXPRESSION);
    } else {
      throw new IllegalArgumentException("unknown subtype of Type: " + type.getClass());
    }
  }

  /**
   * Returns all implementing interfaces, or super class for a given java.lang.Class object.
   *
   * @param klass the class to introspect
   * @return a set containing all implementing interfaces and a super class.
   */
  public static Set<JavaClass> getSuperClassDefinitions(Class<?> klass) {

    return Immutable.setOf(
        parentsOf(klass)
            .stream()
            .map(JavaClass::forceGeneric)
    );
  }

  public static Set<Class<?>> parentsOf(Class<?> that) {
    if (that == null) {
      return Immutable.set();
    }

    final Queue<Class<?>> Q = new LinkedList<>();
    Q.add(that);

    final Set<Class<?>> seen = new HashSet<>();

    do {
      final Class<?> w = Q.remove();

      Set<Class<?>> one = Arrays.stream(w.getInterfaces()).collect(Collectors.toSet());
      final Optional<Class<?>> superClass = Optional.ofNullable(w.getSuperclass());

      Set<Class<?>> two = new HashSet<>();
      superClass.ifPresent(two::add);
      two.removeIf(c -> c.getName().equals("java.lang.Object"));

      final Set<Class<?>> parents = Sets.union(one, two);

      for (Class<?> eachParent : parents) {
        if (!seen.contains(eachParent)) {
          seen.add(eachParent);
          Q.add(eachParent);
        }
      }
    } while (!Q.isEmpty());

    return seen;
  }

  public static JavaClass forceGeneric(Class<?> klass) {
    return new JavaClass(klass, klass.toGenericString(), ReflectConst.USE_LAMBDA_EXPRESSION);
  }

  private static Optional<String> typeVariableToActual(
      Method sam, TypeVariable[] variables, Type[] actual) {

    final Map<TypeVariable, Type> relation = typeParamRelation(variables, actual);
    final BinaryOperator<String> neverUsed = (lambda1, lambda2) -> null;

    final String lambda = relation.entrySet().stream().reduce(toLambda(sam), (l, entry) -> {
      final String tentative = entry.getKey().getTypeName();
      final String actualName = entry.getValue()
          .getTypeName()
          .replaceAll("\\? (super|extends) ", "")
          .replace("$", "__");
      return Pattern.compile("\\b" + tentative + "\\b").matcher(l).replaceAll(actualName);
    }, neverUsed);

    return Optional.of(lambda);
  }

  private static Map<TypeVariable, Type> typeParamRelation(TypeVariable[] variables,
      Type[] actual) {
    final Map<TypeVariable, Type> toActual = new HashMap<>();

    if (actual.length == 0) {
      for (TypeVariable variable : variables) {
        toActual.put(variable, variable);
      }
    } else {
      for (int i = 0; i < variables.length; i++) {
        toActual.put(variables[i], actual[i]);
      }
    }

    return toActual;
  }

  private static String toLambda(final Method method) {
    final Type[] parameterTypes = method.getGenericParameterTypes();
    final Type returnType = method.getGenericReturnType();
    final JavaClass typeDef = JavaClass
        .forceClassNameFormEvenIfFunctionalInterface(returnType);
    final String returnTypeString = typeDef.getSimpleForm();

    return String.format("%s -> %s", argumentsInSimpleNotation(parameterTypes), returnTypeString);
  }

  private static String argumentsInSimpleNotation(Type[] arguments) {
    switch (arguments.length) {
      case 0:
        return "()";
      case 1:
        // arg
        return JavaClass
            .forceClassNameFormEvenIfFunctionalInterface(arguments[0])
            .getSimpleForm();
      default:
        // (arg1, arg2)
        final StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Type eachArg : arguments) {
          joiner.add(JavaClass
              .forceClassNameFormEvenIfFunctionalInterface(eachArg)
              .getSimpleForm()
          );
        }
        return joiner.toString();
    }
  }

  private static Optional<String> chooseDeclaredSamOrInheritedSam(
      final Class<?> thisClass, final Type[] actualTypeArgsOfClass) {
    final Optional<Method> declaredSuperAbstractMethods = findDeclaredSuperAbstractMethods(
        thisClass);

    if (declaredSuperAbstractMethods.isPresent()) {
      return typeVariableToActual(
          declaredSuperAbstractMethods.get(),
          thisClass.getTypeParameters(),
          actualTypeArgsOfClass
      );
    }

    try {
      final Method inheritedSuperAbstractMethod = findInheritedSuperAbstractMethods(thisClass);

      final Class<?> superClass = findDeclaringClassOfInheritedSuperAbstractMethod(
          thisClass,
          inheritedSuperAbstractMethod
      );

      final ParameterizedType parameterizedSuper = superClassAsParameterized(thisClass, superClass);

      return typeVariableToActual(inheritedSuperAbstractMethod,
          superClass.getTypeParameters(),
          parameterizedSuper.getActualTypeArguments());
    } catch (Exception ignored) {
      return Optional.empty();
    }

  }

  private static Optional<Method> findDeclaredSuperAbstractMethods(final Class<?> thisClass) {
    return Stream.of(thisClass.getDeclaredMethods())
        .filter(method -> !Modifier.isStatic(method.getModifiers()))
        .filter(JavaMethod::isAbstract)
        .filter(JavaMethod::isUndefinedInObjectClass)
        .findFirst();
  }

  private static ParameterizedType superClassAsParameterized(
      final Class<?> thisClass, final Class<?> superClass) {

    return Stream.of(thisClass.getGenericInterfaces())
        .filter(k -> k.getTypeName().startsWith(superClass.getTypeName()))
        .findFirst()
        .map(ParameterizedType.class::cast)
        .orElseThrow(() -> new IllegalStateException(String.format("thisClass:%s superclass:%s",
            thisClass,
            superClass)));
  }

  // FIXME
  private static Class<?> findDeclaringClassOfInheritedSuperAbstractMethod(
      final Class<?> thisClass, final Method inheritedMethods) {

    return Stream.of(thisClass.getInterfaces())
        .filter(klass -> inheritedMethods.getDeclaringClass().equals(klass))
        .findFirst().orElseThrow(() -> new IllegalStateException("no implementing interface"));
  }

  // FIXME
  private static Method findInheritedSuperAbstractMethods(final Class<?> thisClass) {
    return Stream.of(thisClass.getMethods())
        .filter(method -> !Modifier.isStatic(method.getModifiers()))
        .filter(JavaMethod::isAbstract)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no inherited super abstract method"));
  }

  /**
   * Test if the class itself is public and all enclosing classes are public.
   *
   * @param self the Class instance.
   * @return true if given class is public and its all enclosing classes are public.
   */
  public static boolean isPublic(Class<?> self) {
    try {
      while (true) {
        if (!Modifier.isPublic(self.getModifiers())) {
          return false;
        }
        self = self.getEnclosingClass();
        if (self == null) {
          return true;
        }
      }
    } catch (NoClassDefFoundError ex) {
      return false;
    }
  }

  /**
   * Determines if the class or interface represented by this {@code JavaClass} object is
   * either the same as, or is a superclass or super interface of, the class or interface
   * represented by the specified {@code JavaClass} parameter.
   *
   * @param other the other definition to be checked
   * @return true if it is assignable from; false otherwise.
   */
  public boolean isSuperDefinitionOf(JavaClass other, Classpath classpath) {

    final JavaClass thisDefinition = this;
    final JavaClass thatDefinition = Objects.requireNonNull(other);

    return isAssignableFrom(thisDefinition, thatDefinition, classpath);

  }

  private boolean isAssignableFrom(JavaClass thisDefinition, JavaClass thatDefinition,
      Classpath classpath) {
    // same definition case
    final String thisReifiedName = thisDefinition.getReifiedCanonicalName();
    final String thatReifiedName = thatDefinition.getReifiedCanonicalName();
    if (thisReifiedName.equals(thatDefinition.getReifiedCanonicalName())) {
      return true;
    }

    final Set<JavaClass> children = childrenOf(thisDefinition, classpath);
    for (JavaClass child : children) {
      if (thatReifiedName.equals(child.getReifiedCanonicalName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Determines if the class or interface represented by this {@code JavaClass} object is
   * either the same as, or is a subclass or sub interface of, the class or interface represented by
   * the specified {@code JavaClass} parameter.
   *
   * @param other the other definition to be checked
   * @return true if they are compatible; false otherwise.
   */
  public boolean isSubDefinitionOf(JavaClass other, Classpath classpath) {
    final JavaClass thisDefinition = this;
    final JavaClass thatDefinition = Objects.requireNonNull(other);

    return isAssignableFrom(thatDefinition, thisDefinition, classpath);
  }


  private static Set<JavaClass> childrenOf(JavaClass thisDefinition,
      Classpath classpath) {
    if (thisDefinition == null) {
      return Immutable.set();
    }
    if (classpath == null) {
      return Immutable.set();
    }
    if (classpath.isEmpty()) {
      return Immutable.set();
    }

    final Set<JavaClass> subClasses = classpath.subClassSet(thisDefinition);

    if (subClasses.isEmpty()) {
      return Immutable.set();
    }

    return subClasses;
  }

  public boolean isDeprecated() {
    return isDeprecated;
  }

  public TypeLiteral getTypeLiteral() {
    return typeLiteral;
  }

  public JavaPack getJavaPack() {
    return javaPack;
  }

  public String getTypeName() {
    return typeName;
  }


  public String getClassName() {
    return className;
  }

  public String getCanonicalName() {
    return canonicalName;
  }

  public String getReifiedCanonicalName() {
    return reifiedCanonicalName;
  }

  public boolean isAbstractClass() {
    return isAbstract;
  }

  private static String genericsSubstring(final String typeName) {
    if (typeName == null || typeName.isEmpty()) {
      throw new IllegalArgumentException("cannot generate a generics substring with null typename");
    }

    return Strings.firstNonNullString(Strings.textWithin(typeName, "<", ">"), "");
  }

  /**
   * Returns lambda-expression if this type is functional interface, otherwise just type name.
   */
  public String getSimpleForm() {
    return simpleForm;
  }

  public Set<JavaAnnotation> getAnnotations() {
    return Immutable.setOf(annotations);
  }

  private Optional<String> lambdaIfFunctionalInterface(Type type) {
    if (typeLiteral != TypeLiteral.FUNCTIONAL_INTERFACE) {
      return Optional.empty();
    }

    if (type instanceof ParameterizedType) {
      final ParameterizedType parameterized = (ParameterizedType) type;
      return chooseDeclaredSamOrInheritedSam((Class) parameterized.getRawType(),
          parameterized.getActualTypeArguments());
    }

    return chooseDeclaredSamOrInheritedSam((Class) type, new Type[]{});
  }

  @Override public int hashCode() {
    return canonicalName.hashCode();
  }

  @Override public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final JavaClass typeDef = (JavaClass) o;
    return canonicalName.equals(typeDef.canonicalName);
  }

  @Override public String toString() {
    return "JavaClass (" +
        "pkgDef=" + javaPack +
        ", qualifiedName='" + getReifiedCanonicalName() + '\'' +
        ", isDeprecated='" + isDeprecated() + '\'' +
        ", isAbstract='" + isDeprecated() + '\'' +
        ", type=" + typeLiteral +
        (!annotations.isEmpty() ? (", annotatedWith=" + annotations) : "") +
        ')';
  }

}
