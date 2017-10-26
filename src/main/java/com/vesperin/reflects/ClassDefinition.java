package com.vesperin.reflects;

import com.vesperin.base.Jdt;
import com.vesperin.utils.Expect;
import com.vesperin.utils.Immutable;
import com.vesperin.utils.Strings;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Huascar Sanchez
 */
public class ClassDefinition {


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



  private static final String MISSING = "MISSING";
  private static final String DEFAULT_NAMESPACE = "";
  private static final String JAVA_LANG_NAMESPACE = "java.lang";

  private static final Pattern EXCLUDING_MODIFIERS = Pattern.compile(
    "\\b(?:public|protected|private|class|interface|enum|abstract|native|static|strictfp|final|synchronized) \\b"
  );

  private static final int USE_LAMBDA_EXPRESSION      = 0;
  private static final int NOT_USE_LAMBDA_EXPRESSION  = 1;

  private final PackageDefinition packageDefinition;
  private final String typeName;
  private final String className;
  private final String canonicalName;
  private final String simpleForm;
  private final TypeLiteral typeLiteral;
  private final boolean isDeprecated;
  private final boolean isAbstract;
  private final String reifiedCanonicalName;

  /**
   * Construct a new class definition for a given {@link Type}
   *
   * @param type the {@link Type} of interest.
   */
  private ClassDefinition(Type type) {
    this(type, USE_LAMBDA_EXPRESSION);
  }

  /**
   * Construct a new class definition for a given {@link Type} and lambda expression check.
   *
   * @param type the {@link Type} of interest.
   * @param flag whether this class uses lambda expressions or not.
   */
  private ClassDefinition(Type type, int flag) {
    this(type, notDotsPackageDefinitionFunction(type), flag);
  }

  /**
   * Construct a new class definition for a given {@link Type}, filter flags, generic
   * representation of the class.
   *
   * @param type the {@link Type} of interest.
   * @param genericString string representation of class.
   * @param flag whether this class uses lambda expressions or not.
   */
  private ClassDefinition(Type type, String genericString, int flag) {
    this(type, onlyFewPackageDefinitionFunction(genericString), flag);
  }

  /**
   * Construct a new class definition.
   *
   * @param type the {@link Type} of interest.
   * @param pkgFunction package definition function.
   * @param flag whether this class uses lambda expressions or not.
   */
  private ClassDefinition(Type type, Function<PackageDefinition, String> pkgFunction, int flag) {

    this.packageDefinition = PackageDefinition.from(type);


    this.typeLiteral = TypeLiteral.from(type);
    this.isAbstract  = isAbstract(type);

    if (typeLiteral == TypeLiteral.VOID) {
      this.typeName = "()";
      this.simpleForm = "()";
      this.canonicalName = "()";
      this.reifiedCanonicalName = "()";
    } else {
      this.typeName = pkgFunction.apply(packageDefinition);

      if (TypeLiteral.FUNCTIONAL_INTERFACE == typeLiteral && USE_LAMBDA_EXPRESSION == flag) {
        this.simpleForm = lambdaIfFunctionalInterface(type)
          .orElse(typeName);
      } else {
        this.simpleForm = typeName;
      }

      final String packageToString = packageDefinition.toString();
      this.canonicalName = (packageToString.isEmpty()
        ? typeName
        : packageToString + "." + typeName
      );

      this.reifiedCanonicalName = canonicalName.replace(genericsSubstring(canonicalName), "");
    }

    this.isDeprecated = isDeprecated(type);

    this.className    = (typeName.contains("<") && typeName.contains(">")
      ? typeName.replace(genericsSubstring(typeName), "")
      : typeName);
  }

  /**
   * Construct a new class definition.
   *
   * @param pkgDef package definition
   * @param typeName the type this definition represents.
   * @param isDeprecated true if this class is deprecated; false otherwise.
   * @param isAbstract true if this class is an abstract class; false otherwise.
   */
  private ClassDefinition(PackageDefinition pkgDef, String typeName,
    boolean isDeprecated, boolean isAbstract){

    this.packageDefinition  = pkgDef;
    this.typeLiteral        = TypeLiteral.voidOrClass(typeName);
    this.isAbstract         = isAbstract;
    this.isDeprecated       = isDeprecated;
    this.typeName           = typeName;
    this.simpleForm         = this.typeName;

    this.className    = (this.typeName.contains("<") && this.typeName.contains(">")
      ? typeName.replace("<(.+?)>", "")//this.typeName.substring(0, this.typeName.lastIndexOf("<"))
      : typeName);

    final String packageToString = this.packageDefinition.toString();
    this.canonicalName = (packageToString.isEmpty()
      ? this.typeName
      : packageToString + "." + this.typeName
    );

    this.reifiedCanonicalName = this.canonicalName
      .replace(
        genericsSubstring(this.canonicalName), ""
      );
  }

  private static Function<PackageDefinition, String> notDotsPackageDefinitionFunction(Type type) {
    return (packageDef) -> {
      final String typeName = typeNameOrMissing(type);
      if(typeName.contains(".")){
        return typeName.replace(packageDef.getName() + ".", "");
      }

      return typeName;
    };

  }

  private static String missingType(){
    return MISSING;
  }

  static String typeNameOrMissing(Type type){
    try {
      return type.getTypeName();
    } catch (TypeNotPresentException ignored){
      return missingType();
    }
  }

  private static Function<PackageDefinition, String> onlyFewPackageDefinitionFunction(String genericString) {
    return (packageDef) -> {
      final String genericTypeName = EXCLUDING_MODIFIERS.matcher(genericString).replaceAll("");
      return genericTypeName.replace(packageDef.getName() + ".", "");
    };
  }

  /**
   * Test if the type name is a primitive type
   *
   * @param typeName type name
   * @return true if it is a primitive type; false otherwise.
   */
  public static boolean isPrimitive(String typeName){
    return PRIMITIVE_TO_OBJECT.containsKey(Expect.nonNull(typeName));
  }

  /**
   * Converts a primitive type into its object equivalent.
   * @param typeName type name
   * @return object equivalent of primitive type.
   */
  public static String objefyPrimitiveType(String typeName){
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

  public static ClassDefinition from(String pkgString, String candidateTypeName,
    boolean isDeprecated, boolean isAbstract) {


    if("void".equals(candidateTypeName)) {
      return ClassDefinition.voidClassDefinition();
    }

    final String[] packageAndTypeName = perfectCouple(pkgString, candidateTypeName);

    final String packageName = packageAndTypeName[0];
    final String typeName    = packageAndTypeName[1];

    Expect.validArgument(typeName != null && !typeName.isEmpty());

    final PackageDefinition pkgDef = PackageDefinition.from(packageName);

    return new ClassDefinition(pkgDef, typeName, isDeprecated, isAbstract);
  }

  public static ClassDefinition classDefinition(CompilationUnit unit, ITypeBinding typeBinding){
    Expect.nonNull(typeBinding);

    final String typeName = typeBinding.getName();
    String pkgDef;

    if(typeBinding.toString().contains(MISSING)) {
      pkgDef = packageName(unit, typeName);
    } else {
      final Optional<IPackageBinding> packageBinding = Optional.ofNullable(typeBinding.getPackage());
      pkgDef  = packageBinding.isPresent() ? packageBinding.get().getName() : "";
    }

    final boolean isClassDeprecated = typeBinding.isDeprecated();
    final boolean isClassAbstract = Modifier.isAbstract(typeBinding.getModifiers());

    return ClassDefinition.from(pkgDef, typeName, isClassDeprecated,  isClassAbstract);
  }

  static ClassDefinition classDefinition(ITypeBinding typeBinding){
    Expect.nonNull(typeBinding);

    final String typeName = typeBinding.getName();
    String pkgDef;

    if(typeBinding.toString().contains(MISSING)) {
      pkgDef = PackageDefinition.isJavaLang(typeName)
        ? JAVA_LANG_NAMESPACE
        : DEFAULT_NAMESPACE;
    } else {
      final Optional<IPackageBinding> packageBinding = Optional.ofNullable(typeBinding.getPackage());

      pkgDef  = packageBinding.isPresent() ? packageBinding.get().getName() : DEFAULT_NAMESPACE;

      if(pkgDef.isEmpty()){
        pkgDef = PackageDefinition.isJavaLang(typeName)
          ? JAVA_LANG_NAMESPACE
          : pkgDef;
      }
    }

    final boolean isClassDeprecated = typeBinding.isDeprecated();
    final boolean isClassAbstract = Modifier.isAbstract(typeBinding.getModifiers());

    return ClassDefinition.from(pkgDef, typeName, isClassDeprecated,  isClassAbstract);
  }

  static ClassDefinition returnClassDefinition(CompilationUnit unit, IMethodBinding methodBinding){
    return classDefinition(unit, methodBinding.getReturnType());
  }


  static ClassDefinition classDefinition(CompilationUnit unit, IMethodBinding methodBinding){
    return classDefinition(unit, methodBinding.getDeclaringClass());
  }

  static Set<String> unitImports(CompilationUnit unit){
    return Immutable.setOf(
      Jdt.typeSafeList(ImportDeclaration.class, unit.imports())
        .stream().map(i -> i.getName().getFullyQualifiedName())
    );
  }

  private static String packageName(CompilationUnit unit, String typeName){
    final Set<String> imports = unitImports(unit);

    for(String eachImport: imports){
      if(eachImport.contains(typeName)) return eachImport.replace("." + typeName, "");
    }


    return PackageDefinition.isJavaLang(typeName)
      ? JAVA_LANG_NAMESPACE
      : DEFAULT_NAMESPACE;
  }

  private static String[] perfectCouple(String pkgString, String candidateTypeName){
    final String[] result = new String[2];

    String packageName;
    String typeName = candidateTypeName;
    if(isPrimitive(typeName) && "".equals(pkgString)){
      final String pn = objefyPrimitiveType(typeName);
      final int typeNameIndex = pn.lastIndexOf('.');

      if(typeNameIndex != -1)  {
        packageName = pn.substring(0, typeNameIndex);
        typeName    = pn.substring(typeNameIndex + 1, pn.length());
        result[0]   = packageName;
        result[1]   = typeName;

      } else {
        packageName = pkgString;
        result[0]   = packageName;
        result[1]   = typeName;
      }
    } else {
      packageName = pkgString;
      result[0]   = packageName;
      result[1]   = typeName;
    }

    return result;
  }

  public static ClassDefinition from(Type type) {
    if (type instanceof ParameterizedType || type instanceof TypeVariable ||
      type instanceof GenericArrayType || type instanceof Class) {
      return new ClassDefinition(type);
    } else {
      throw new IllegalArgumentException("unknown subtype of Type: " + type.getClass());
    }
  }

  private static ClassDefinition voidClassDefinition(){
    return new ClassDefinition(
      PackageDefinition.from("java.lang"),
      "Void",
      false,
      false
    );
  }

  static ClassDefinition missingClassDefinition(){
    return new ClassDefinition(PackageDefinition.from(""), missingType(), false, false);
  }

  private static ClassDefinition forceClassNameFormEvenIfFunctionalInterface(Type type) {
    if (type instanceof ParameterizedType || type instanceof TypeVariable ||
      type instanceof GenericArrayType || type instanceof Class<?>) {
      return new ClassDefinition(type, NOT_USE_LAMBDA_EXPRESSION);
    } else {
      throw new IllegalArgumentException("unknown subtype of Type: " + type.getClass());
    }
  }


  public static ClassDefinition forceGeneric(Class<?> klass) {
    return new ClassDefinition(klass, klass.toGenericString(), USE_LAMBDA_EXPRESSION);
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

  private static Map<TypeVariable, Type> typeParamRelation(TypeVariable[] variables, Type[] actual) {
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
    final ClassDefinition typeDef = ClassDefinition.forceClassNameFormEvenIfFunctionalInterface(returnType);
    final String returnTypeString = typeDef.getSimpleForm();

    return String.format("%s -> %s", argumentsInSimpleNotation(parameterTypes), returnTypeString);
  }

  private static String argumentsInSimpleNotation(Type[] arguments) {
    switch (arguments.length) {
      case 0:
        return "()";
      case 1:
        // arg
        return ClassDefinition
          .forceClassNameFormEvenIfFunctionalInterface(arguments[0])
          .getSimpleForm();
      default:
        // (arg1, arg2)
        final StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Type eachArg : arguments) {
          joiner.add(ClassDefinition
            .forceClassNameFormEvenIfFunctionalInterface(eachArg)
            .getSimpleForm()
          );
        }
        return joiner.toString();
    }
  }

  private static Optional<String> chooseDeclaredSamOrInheritedSam(
    final Class<?> thisClass, final Type[] actualTypeArgsOfClass) {
    final Optional<Method> declaredSuperAbstractMethods = findDeclaredSuperAbstractMethods(thisClass);

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
    } catch (Exception ignored){
      return Optional.empty();
    }

  }

  private static Optional<Method> findDeclaredSuperAbstractMethods(final Class<?> thisClass) {
    return Stream.of(thisClass.getDeclaredMethods())
      .filter(method -> !Modifier.isStatic(method.getModifiers()))
      .filter(MethodDefinition::isAbstract)
      .filter(MethodDefinition::isUndefinedInObjectClass)
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
      .filter(MethodDefinition::isAbstract)
      .findFirst().orElseThrow(() -> new IllegalStateException("no inherited super abstract method"));
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

  public boolean isDeprecated() {
    return isDeprecated;
  }

  public TypeLiteral getTypeLiteral() {
    return typeLiteral;
  }

  public PackageDefinition getPackageDefinition() {
    return packageDefinition;
  }

  public String getTypeName() {
    return typeName;
  }


  public String getClassName(){
    return className;
  }

  public String getCanonicalName() {
    return canonicalName;
  }

  public String getReifiedCanonicalName() {
    return canonicalName;
  }

  public boolean isAbstractClass() {
    return isAbstract;
  }

  private static String genericsSubstring(final String typeName) {
    Expect.validArgument(!Expect.nonNull(typeName).isEmpty());

    return Strings.substringWithin(typeName, "<", ">");
  }

  /**
   * Returns lambda-expression if this type is functional interface, otherwise just type name.
   */
  public String getSimpleForm() {
    return simpleForm;
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

    final ClassDefinition typeDef = (ClassDefinition) o;
    return canonicalName.equals(typeDef.canonicalName);
  }

  @Override public String toString() {
    return "ClassDefinition (" +
      "pkgDef=" + packageDefinition +
      ", qualifiedName='" + getCanonicalName() + '\'' +
      ", isDeprecated='" + isDeprecated() + '\'' +
      ", isAbstract='" + isDeprecated() + '\'' +
      ", type=" + typeLiteral +
      ')';
  }

}
