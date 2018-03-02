package com.vesperin.reflects;

import com.vesperin.base.Jdt;
import com.vesperin.utils.Expect;
import com.vesperin.utils.Immutable;
import com.vesperin.utils.Sets;
import com.vesperin.utils.Strings;
import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Huascar Sanchez
 */
public class ClassDefinition {


  private static final Pattern EXCLUDING_MODIFIERS = Pattern.compile(
    "\\b(?:public|protected|private|class|interface|enum|abstract|native|static|strictfp|final|synchronized) \\b"
  );

  private static final int USE_LAMBDA_EXPRESSION      = 0;
  private static final int NOT_USE_LAMBDA_EXPRESSION  = 1;


  private static final Classpath CS;
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

    CS = Classpath.getClasspath();
  }



  private static final String MISSING = "MISSING";
  private static final String DEFAULT_NAMESPACE = "";
  private static final String JAVA_LANG_NAMESPACE = "java.lang";


  private final PackageDefinition packageDefinition;
  private final String typeName;
  private final String className;
  private final String simpleForm;
  private final TypeLiteral typeLiteral;
  private final boolean isDeprecated;
  private final boolean isAbstract;
  private final String reifiedCanonicalName;
  private final Set<AnnotationDefinition> annotations;
  private final String canonicalName;

  /**
   * Construct a new class definition for a given {@link Type}
   *
   * @param type the {@link Type} of interest.
   */
  private ClassDefinition(Type type) {
    this(type, USE_LAMBDA_EXPRESSION);
  }

  /**
   * Construct a new class definition for a given {@link Type}
   *
   * @param type the {@link Type} of interest.
   * @param annotations type annotations
   */
  private ClassDefinition(Type type, Set<AnnotationDefinition> annotations) {
    this(type,  notDotsPackageDefinitionFunction(type), USE_LAMBDA_EXPRESSION, annotations);
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
    this(type, onlyFewPackageDefinitionFunction(genericString), flag, new HashSet<>());
  }

  private ClassDefinition(Type type, String genericString, int flag, Set<AnnotationDefinition> annotations) {
    this(type, onlyFewPackageDefinitionFunction(genericString), flag, annotations);
  }

  private ClassDefinition(Type type, Function<PackageDefinition, String> pkgFunction, int flag) {
    this(type, pkgFunction, flag, new HashSet<>());
  }

   /**
    * Construct a new class definition.
    *
    * @param type the {@link Type} of interest.
    * @param pkgFunction package definition function.
    * @param flag whether this class uses lambda expressions or not.
    * @param annotations type annotations
    */
  private ClassDefinition(Type type, Function<PackageDefinition, String> pkgFunction, int flag, Set<AnnotationDefinition> annotations) {

    this.packageDefinition = PackageDefinition.from(type);


    this.typeLiteral = TypeLiteral.from(type);
    this.isAbstract  = isAbstract(type);

    this.annotations = new HashSet<>(annotations);

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

      final String toStringAnnotation = annotations.toString();

      final String packageToString = packageDefinition.toString();
      this.canonicalName = (packageToString.isEmpty()
        ? (toStringAnnotation + " ") + typeName
        : (toStringAnnotation + " ") + packageToString + "." + this.typeName
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
    boolean isDeprecated, boolean isAbstract, Set<AnnotationDefinition> annotations){

    this.packageDefinition  = pkgDef;
    this.typeLiteral        = TypeLiteral.voidOrClass(typeName);
    this.isAbstract         = isAbstract;
    this.isDeprecated       = isDeprecated;
    this.typeName           = typeName;
    this.simpleForm         = this.typeName;

    this.className    = (this.typeName.contains("<") && this.typeName.contains(">")
      ? typeName.replace("<(.+?)>", "")
      : typeName);


    this.annotations = new HashSet<>(annotations);

    final String toStringAnnotation = annotations.toString();

    final String packageToString = this.packageDefinition.toString();
    this.canonicalName = (packageToString.isEmpty()
      ? (toStringAnnotation + " ") + typeName
      : (toStringAnnotation + " ") + packageToString + "." + this.typeName
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
   *
   * @param typeName type name
   * @return object equivalent of primitive type.
   */
  public static String objectifyPrimitiveType(String typeName){
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

  public static ClassDefinition from(
    String pkgString, String candidateTypeName, boolean isDeprecated, boolean isAbstract) {

    return from(pkgString, candidateTypeName, isDeprecated, isAbstract, new HashSet<>());
  }


  public static ClassDefinition from(String pkgString, String candidateTypeName,
    boolean isDeprecated, boolean isAbstract, Set<AnnotationDefinition> annotations) {


    if("void".equals(candidateTypeName)) {
      return ClassDefinition.voidClassDefinition();
    }

    final String[] packageAndTypeName = perfectCouple(pkgString, candidateTypeName);

    final String packageName = packageAndTypeName[0];
    final String typeName    = packageAndTypeName[1];

    Expect.validArgument(typeName != null && !typeName.isEmpty());

    final PackageDefinition pkgDef = PackageDefinition.from(packageName);

    return new ClassDefinition(pkgDef, typeName, isDeprecated, isAbstract, annotations);
  }

  public static ClassDefinition from(Type type){
    return from(type, new HashSet<>());
  }

  public static ClassDefinition from(Type type, Set<AnnotationDefinition> annotations) {
    if (type instanceof ParameterizedType || type instanceof TypeVariable ||
      type instanceof GenericArrayType || type instanceof Class) {
      return new ClassDefinition(type, annotations);
    } else {
      throw new IllegalArgumentException("unknown subtype of Type: " + type.getClass());
    }
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

    final Set<AnnotationDefinition> annotations = Immutable.setOf(
      Arrays.stream(typeBinding.getTypeAnnotations()).map(AnnotationDefinition::annotationDefinition)
    );

    return ClassDefinition.from(pkgDef, typeName, isClassDeprecated,  isClassAbstract, annotations);
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
    String typeName = Expect.nonNull(candidateTypeName);
    if(isPrimitive(typeName) && "".equals(pkgString)){
      packageName = pkgString;
      result[0]   = packageName;
      result[1]   = typeName;

    } else {
      packageName = pkgString;
      result[0]   = packageName;
      result[1]   = typeName;
    }

    return result;
  }

  private static ClassDefinition voidClassDefinition(){
    return new ClassDefinition(
      PackageDefinition.from("java.lang"),
      "Void",
      false,
      false,
      Immutable.set()
    );
  }

  static ClassDefinition missingClassDefinition(){
    return new ClassDefinition(
      PackageDefinition.from(""), missingType(), false, false, Immutable.set()
    );
  }

  private static ClassDefinition forceClassNameFormEvenIfFunctionalInterface(Type type) {
    if (type instanceof ParameterizedType || type instanceof TypeVariable ||
      type instanceof GenericArrayType || type instanceof Class<?>) {
      return new ClassDefinition(type, NOT_USE_LAMBDA_EXPRESSION);
    } else {
      throw new IllegalArgumentException("unknown subtype of Type: " + type.getClass());
    }
  }

  /**
   * Returns all implementing interfaces, or super class for a given java.lang.Class
   * object.
   *
   * @param klass the class to introspect
   * @return a set containing all implementing interfaces and a super class.
   */
  public static Set<ClassDefinition> getSuperClassDefinitions(Class<?> klass){

    return Immutable.setOf(
      parentsOf(klass)
        .stream()
        .map(ClassDefinition::forceGeneric)
    );
  }

  public static Set<Class<?>> parentsOf(Class<?> that) {
    if (that == null) return Immutable.set();

    final Queue<Class<?>> Q = new LinkedList<>();
    Q.add(that);

    final Set<Class<?>> seen = new HashSet<>();

    do {
      final Class<?> w = Q.remove();

      Set<Class<?>> one =  Arrays.stream(w.getInterfaces()).collect(Collectors.toSet());
      final Optional<Class<?>> superClass = Optional.ofNullable(w.getSuperclass());

      Set<Class<?>> two = new HashSet<>();
      superClass.ifPresent(two::add);
      two.removeIf(c -> c.getName().equals("java.lang.Object"));

      final Set<Class<?>> parents = Sets.union(one, two);

      for(Class<?> eachParent : parents){
        if(!seen.contains(eachParent)){
          seen.add(eachParent);
          Q.add(eachParent);
        }
      }
    } while (!Q.isEmpty());

    return seen;
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

  /**
   * Determines if the class or interface represented by this
   * {@code ClassDefinition} object is either the same as, or is
   * a superclass or super interface of, the class or interface
   * represented by the specified {@code ClassDefinition} parameter.
   *
   * @param other the other definition to be checked
   * @return true if it is assignable from; false otherwise.
   */
  public boolean isSuperDefinitionOf(ClassDefinition other){

    final ClassDefinition thisDefinition = this;
    final ClassDefinition thatDefinition = Expect.nonNull(other);

    return isAssignableFrom(thisDefinition, thatDefinition);

  }

  private boolean isAssignableFrom(ClassDefinition thisDefinition, ClassDefinition thatDefinition) {
    // same definition case
    final String thisReifiedName = thisDefinition.getReifiedCanonicalName();
    final String thatReifiedName = thatDefinition.getReifiedCanonicalName();
    if(thisReifiedName.equals(thatDefinition.getReifiedCanonicalName())){
      return true;
    }

    final Set<ClassDefinition> children = childrenOf(thisDefinition);
    for(ClassDefinition child : children){
      if(thatReifiedName.equals(child.getReifiedCanonicalName())){
        return true;
      }
    }

    return false;
  }

  /**
   * Determines if the class or interface represented by this
   * {@code ClassDefinition} object is either the same as, or is
   * a subclass or sub interface of, the class or interface
   * represented by the specified {@code ClassDefinition} parameter.
   *
   * @param other the other definition to be checked
   * @return true if they are compatible; false otherwise.
   */
  public boolean isSubDefinitionOf(ClassDefinition other){
    final ClassDefinition thisDefinition = this;
    final ClassDefinition thatDefinition = Expect.nonNull(other);

    return isAssignableFrom(thatDefinition, thisDefinition);
  }


  private static Set<ClassDefinition> childrenOf(ClassDefinition thisDefinition){
    if(thisDefinition == null) return Immutable.set();
    if(!CS.getClassToSubDefinitions().containsKey(thisDefinition)) return Immutable.set();

    return CS.getClassToSubDefinitions().get(thisDefinition);
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
    return reifiedCanonicalName;
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

  public Set<AnnotationDefinition> getAnnotations(){
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

    final ClassDefinition typeDef = (ClassDefinition) o;
    return canonicalName.equals(typeDef.canonicalName);
  }

  @Override public String toString() {
    return "ClassDefinition (" +
      "pkgDef=" + packageDefinition +
      ", qualifiedName='" + getReifiedCanonicalName() + '\'' +
      ", isDeprecated='" + isDeprecated() + '\'' +
      ", isAbstract='" + isDeprecated() + '\'' +
      ", type=" + typeLiteral +
      (!annotations.isEmpty() ? (", annotatedWith=" + annotations) : "") +
      ')';
  }

  public static void main(String[] args) {
    //Classpath.getClasspath();
    System.out.println(System.getProperty("java.home") + "/lib");
    System.out.println(System.getProperty("sun.boot.class.path"));
//    System.out.println(Classpath.getClasspath().getClassNameToDefinitionIndex().containsKey("int"));
//    System.out.println(Classpath.getClasspath().getCanonicalNameToDefinition().containsKey("int"));
//
//    final ClassDefinition      a    = ClassDefinition.forceGeneric(int.class);
//    final ClassDefinition      b    = ClassDefinition.forceGeneric(Integer.class);
//
//    System.out.println(b.isSubDefinitionOf(a));

//
//    final ClassDefinition      c    = ClassDefinition.forceGeneric(Collection.class);
//    final ClassDefinition      d    = ClassDefinition.forceGeneric(int.class);
//    final ClassDefinition      e    = ClassDefinition.forceGeneric(Integer.class);
//
//
//    System.out.println("...");
//
//    System.out.println(a.isSuperDefinitionOf(b));
//    System.out.println(b.isSubDefinitionOf(a));
//
//
//    System.out.println((b.isSuperDefinitionOf(b) == b.isSubDefinitionOf(b)));
//
//    System.out.println(a.isSuperDefinitionOf(c));
//    System.out.println(c.isSuperDefinitionOf(a));
//
//    System.out.println(d.isSuperDefinitionOf(e));
//    System.out.println(e.isSubDefinitionOf(d));

//    if(ClassDefinition.isPrimitive("int")){
//      String intObj = ClassDefinition.objectifyPrimitiveType("int");
//      if(Classpath.getClasspath().getCanonicalNameToDefinition().containsKey(intObj)){
//        System.out.println(intObj);
//      }
//    }
    final ClassDefinition      z    = ClassDefinition.forceGeneric(String[].class);
    System.out.println(z);

  }

}
