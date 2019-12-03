package com.vesperin.reflects;

import static com.vesperin.reflects.JavaClass.classDefinition;
import static com.vesperin.reflects.JavaClass.returnClassDefinition;

import com.vesperin.base.CommonJdt;
import com.vesperin.utils.Immutable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * JavaMethod defines a Java method in the Vesperin framework
 */
public class JavaMethod {

  private final Set<JavaAnnotation> annotations;
  private final Set<JavaClass> exceptions;
  private final List<JavaClass> arguments;
  private final List<JavaClass> typeParameters;
  private final JavaClass returnType;
  private final JavaClass declaringClass;
  private final int modifiers;
  private final String methodName;
  private final String simpleForm;
  private final String fullForm;
  private final boolean isDeprecated;


  /**
   * Constructs a new method definition object for a given a set of arguments.
   *
   * @param methodName     the name of this method
   * @param arguments      the method's arguments.
   * @param returnType     the method's return type
   * @param annotations    the method's annotations
   * @param exceptions     the exceptions this method throws
   * @param isDeprecated   true if this method is deprecated; false otherwise.
   * @param declaringClass the class where this method resides.
   */
  private JavaMethod(String methodName, List<JavaClass> arguments,
      JavaClass returnType, Set<JavaAnnotation> annotations,
      Set<JavaClass> exceptions, List<JavaClass> typeParameters,
      int modifiers, boolean isDeprecated,
      JavaClass declaringClass) {

    this.methodName = Objects.requireNonNull(methodName);
    this.returnType = Objects.requireNonNull(returnType);
    this.declaringClass = Objects.requireNonNull(declaringClass);
    this.annotations = annotations;
    this.exceptions = exceptions;

    this.arguments = arguments;
    this.typeParameters = typeParameters;

    this.modifiers = modifiers;
    this.isDeprecated = isDeprecated;
    this.simpleForm = formatString(JavaClass::getSimpleForm, () -> "");

    final boolean isStatic = getModifiers().contains(Modifiers.STATIC);

    this.fullForm = formatString(JavaClass::getCanonicalName,
        () -> declaringClass.getCanonicalName() + (isStatic ? "." : "#") + this.getMethodName() + ": "
    );

  }

  /**
   * Returns all methods declared in a class.
   */
  public static Stream<JavaMethod> declaredMethods(Class<?> klass) {
    return Stream.concat(
        allPublicConstructors(klass),
        allDefinitionsStream(allMethodStream(klass), klass.getName().equals("java.lang.Object"))
    );
  }

  private static Stream<Method[]> allMethodStream(Class<?> klass) {
    return Stream.of(klass).map(t -> {
      try {
        return t.getDeclaredMethods();
      } catch (NoClassDefFoundError e) {
        return new Method[]{};
      }
    });
  }

  private static Stream<JavaMethod> allPublicConstructors(Class<?> klass) {
    try {
      return Arrays.stream(klass.getDeclaredConstructors())
          .filter(c -> !"java.lang.Object".equals(c.getDeclaringClass().getName()))
          .filter(c -> Modifier.isPublic(c.getModifiers()))
          .map(JavaMethod::from);
    } catch (Throwable ignored) {
      return Stream.empty();
    }

  }

  private static Stream<JavaMethod> allDefinitionsStream(Stream<Method[]> stream,
      boolean inObject) {
    return stream.flatMap(Stream::of)
        .filter(c -> inObject != JavaMethod.isUndefinedInObjectClass(c))
        .map(JavaMethod::from);
  }

  /**
   * Creates a new method definition from a {@link IMethodBinding}
   *
   * @param methodBinding the {@link IMethodBinding} object.
   * @return a new method definition.
   */
  public static JavaMethod from(IMethodBinding methodBinding) {
    final IMethodBinding nonNullBinding = Objects.requireNonNull(methodBinding);

    final String methodName = nonNullBinding.getName();

    final List<JavaClass> arguments = Immutable.listOf(
        Arrays.stream(methodBinding.getParameterTypes()).map(JavaClass::classDefinition)
    );

    final Set<JavaClass> exceptions = Immutable.setOf(
        Arrays.stream(methodBinding.getExceptionTypes()).map(JavaClass::classDefinition)
    );

    final Set<JavaAnnotation> annotations = Immutable.setOf(
        Arrays.stream(methodBinding.getAnnotations())
            .map(JavaAnnotation::annotationDefinition)
    );

    final List<JavaClass> typeParameters = Immutable.listOf(
        Arrays.stream(methodBinding.getTypeParameters()).map(JavaClass::classDefinition)
    );

    final int modifiers = methodBinding.getModifiers();
    final boolean isDeprecated = methodBinding.isDeprecated();

    final JavaClass declaringClass = classDefinition(methodBinding.getDeclaringClass());

    // if name is a Java identifier (e.g., E), then default to Object.class.
    final String rtQualifiedName = methodBinding.getReturnType().getQualifiedName();
    final JavaClass returnType = !isJavaIdentifier(rtQualifiedName)
        ? classDefinition(methodBinding.getReturnType())
        : JavaClass.forceGeneric(Object.class);

    return from(methodName, arguments,
        returnType, annotations, exceptions, typeParameters,
        modifiers, isDeprecated, declaringClass);


  }

  private static boolean isJavaIdentifier(String qualifiedName) {
    final boolean isSingleChar = qualifiedName.length() == 1;
    return isSingleChar && Character.isJavaIdentifierPart(qualifiedName.charAt(0));
  }

  /**
   * Creates a new method definition from a {@link MethodDeclaration}
   *
   * @param method the {@link MethodDeclaration} object.
   * @return a new method definition.
   */
  public static JavaMethod from(MethodDeclaration method) {
    final IMethodBinding methodBinding = Objects.requireNonNull(method.resolveBinding());

    final CompilationUnit unit = CommonJdt.parent(CompilationUnit.class, method);
    Objects.requireNonNull(unit);

    final String methodName = methodBinding.getName();
    final List<JavaClass> arguments = Immutable.listOf(
        Arrays.stream(methodBinding.getParameterTypes()).map(t -> classDefinition(unit, t))
    );

    final Set<JavaClass> exceptions = Immutable.setOf(
        Arrays.stream(methodBinding.getExceptionTypes()).map(t -> classDefinition(unit, t))
    );

    final Set<JavaAnnotation> annotations = Immutable.setOf(
        Arrays.stream(methodBinding.getAnnotations())
            .map(JavaAnnotation::annotationDefinition)
    );

    final List<JavaClass> typeParameters = Immutable.listOf(
        Arrays.stream(methodBinding.getTypeParameters()).map(t -> classDefinition(unit, t))
    );

    final int modifiers = methodBinding.getModifiers();
    final boolean isDeprecated = methodBinding.isDeprecated();

    final JavaClass declaringClass = classDefinition(unit, methodBinding.getDeclaringClass());

    final JavaClass returnType = returnClassDefinition(unit, methodBinding);

    return from(methodName, arguments,
        returnType, annotations, exceptions, typeParameters,
        modifiers, isDeprecated, declaringClass);

  }

  /**
   * Creates a new method definition from an array of arguments.
   *
   * @param methodName     the name of this method
   * @param arguments      the method's arguments.
   * @param returnType     the method's return type
   * @param annotations    the method's annotations
   * @param exceptions     the exceptions this method throws
   * @param isDeprecated   true if this method is deprecated; false otherwise.
   * @param declaringClass the class where this method resides.
   * @return a new Method Definition.
   */
  public static JavaMethod from(String methodName, List<JavaClass> arguments,
      JavaClass returnType, Set<JavaAnnotation> annotations,
      Set<JavaClass> exceptions, List<JavaClass> typeParameters,
      int modifiers, boolean isDeprecated,
      JavaClass declaringClass) {

    return new JavaMethod(methodName, arguments,
        returnType, annotations, exceptions, typeParameters,
        modifiers, isDeprecated, declaringClass);
  }

  /**
   * Creates a new method definition from a {@link Constructor}
   *
   * @param constructor the {@link Constructor} object
   * @return a new method definition
   */
  public static JavaMethod from(Constructor<?> constructor) {
    final String constructorName = "new";
    final int modifiers = constructor.getModifiers();
    final boolean isDeprecated = constructor.getAnnotation(Deprecated.class) != null;

    return from(constructorName, JavaClass.forceGeneric(constructor.getDeclaringClass()),
        null, constructor.getAnnotations(), constructor.getExceptionTypes(),
        genericParameterTypes(constructor), constructor.getTypeParameters(),
        modifiers, isDeprecated);

  }

  private static JavaMethod from(String name, JavaClass declaringClass, JavaClass returnType, Annotation[] ans,
      Class<?>[] excepts, Type[] parameterTypes, Type[] typeParams, int modifiers,
      boolean isDeprecated) {

    final Set<JavaAnnotation> annotations = Immutable.setOf(Stream.of(ans).map(JavaAnnotation::new));
    final Set<JavaClass> exceptions = Immutable.setOf(Stream.of(excepts).map(JavaClass::from));
    final List<JavaClass> arguments = Immutable.listOf(Stream.of(parameterTypes).map(JavaClass::from));
    final List<JavaClass> typeParameters = Immutable.listOf(Stream.of(typeParams).map(JavaClass::from));

    final JavaClass rt = Optional.ofNullable(returnType).orElse(declaringClass);

    return new JavaMethod(name, arguments,
        rt, annotations, exceptions, typeParameters, modifiers,
        isDeprecated, declaringClass);

  }


  /**
   * Creates a new method definition from a {@link Method}
   *
   * @param method the {@link Method} object.
   * @return a new method definition.
   */
  public static JavaMethod from(Method method) {

    final String methodName = method.getName();
    final JavaClass returnType = missingReturnTypeOrElse(method);

    final int modifiers = method.getModifiers();
    final boolean isDeprecated = method.getAnnotation(Deprecated.class) != null;

    return from(methodName, JavaClass.forceGeneric(method.getDeclaringClass()),
        returnType, method.getAnnotations(), method.getExceptionTypes(),
        genericParameterTypes(method), method.getTypeParameters(),
        modifiers, isDeprecated);
  }


  private static JavaClass missingReturnTypeOrElse(Method method) {
    try {
      if (method.getAnnotatedReturnType().getAnnotations().length > 0) {
        final Annotation[] annotations = method.getAnnotatedReturnType().getAnnotations();
        final Set<JavaAnnotation> definitions = Arrays.stream(annotations)
            .map(JavaAnnotation::new)
            .collect(Collectors.toSet());
        return JavaClass.from(method.getGenericReturnType(), definitions);
      } else {
        return JavaClass.from(method.getGenericReturnType());
      }
    } catch (NoClassDefFoundError ignored) {
      return JavaClass.missingClassDefinition();
    }
  }

  private static Type[] genericParameterTypes(Method method) {
    try {
      return method.getGenericParameterTypes();
    } catch (NoClassDefFoundError ignored) {
      return new Type[0];
    }
  }

  private static Type[] genericParameterTypes(Constructor<?> method) {
    try {
      return method.getGenericParameterTypes();
    } catch (NoClassDefFoundError ignored) {
      return new Type[0];
    }
  }


  private static String formatArgumentsInSimpleNotation(List<JavaClass> arguments,
      Function<JavaClass, String> mapper) {

    switch (arguments.size()) {
      case 0:
        return "()";
      case 1:
        // arg
        return mapper.apply(arguments.get(0));
      default:
        // (arg1, arg2)
        final StringJoiner joiner = new StringJoiner(", ", "(", ")");

        for (JavaClass eachArg : arguments) {
          joiner.add(mapper.apply(eachArg));
        }

        return joiner.toString();
    }
  }

  /**
   * Utility method that returns true if a {@link Method} is abstract, and returns false if it is
   * not.
   *
   * @param method the method to check.
   * @return true if abstract method; false otherwise.
   */
  static boolean isAbstract(Method method) {
    return Modifier.isAbstract(method.getModifiers());
  }


  static boolean isUndefinedInObjectClass(Method method) {
    final Class<?>[] params = method.getParameterTypes();
    final String methodName = method.getName();
    try {
      Object.class.getMethod(methodName, params);
      return false;
    } catch (NoSuchMethodException e) {
      return true;
    }
  }

  public static Set<JavaMethod> declaredMethodDefinitions(Class<?> eachClass){
    return declaredMethods(eachClass)
      .filter(isRelevantMethodDefinition())
      .collect(Collectors.toSet());
  }

  public static Predicate<JavaMethod> isRelevantMethodDefinition() {
    return p -> !p.isDeprecated();
  }

  public static void main(String[] args) {
    System.out.println(isJavaIdentifier("E"));
  }

  /**
   * @return true if method is deprecated; false otherwise.
   */
  public boolean isDeprecated() {
    return isDeprecated;
  }

  /**
   * @return true if method is static; false otherwise.
   */
  public boolean isStatic() {
    return getModifiers().contains(Modifiers.STATIC);
  }

  /**
   * @return all declared annotations for this method.
   */
  public Set<JavaAnnotation> getDeclaredAnnotations() {
    return annotations;
  }

  /**
   * @return this method's declaring class.
   */
  public JavaClass getDeclaringClass() {
    return declaringClass;
  }

  /**
   * @return this method's exceptions
   */
  public Set<JavaClass> getExceptions() {
    return exceptions;
  }

  /**
   * @return this method's arguments.
   */
  public List<JavaClass> getParameterTypes() {
    return arguments;
  }

  /**
   * @return this method's return type.
   */
  public JavaClass getReturnType() {
    return returnType;
  }

  /**
   * @return this method's type parameters.
   */
  public List<JavaClass> getTypeParameters() {
    return typeParameters;
  }

  /**
   * @return this method's modifiers.
   */
  public Set<Modifiers> getModifiers() {
    return Modifiers.from(modifiers);
  }

  /**
   * @return the name of this method.
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * @return this method's type signature in simple form.
   */
  public String getSimpleForm() {
    return simpleForm;
  }

  /**
   * @return this method's type signature in full form (verbose). One can also read its simple form
   * by invoking the {@link #getSimpleForm()} method.
   */
  public String getFullForm() {
    return fullForm;
  }

  private String formatString(final Function<JavaClass, String> name, final Supplier<String> begin) {
    final List<JavaClass> args = new ArrayList<>();
    if (!getModifiers().contains(Modifiers.STATIC)) {
      args.add(declaringClass);
    }

    args.addAll(new ArrayList<>(arguments));

    final String argumentsString = formatArgumentsInSimpleNotation(args, name);
    return String.format("%s%s -> %s", begin.get(), argumentsString, name.apply(returnType));
  }

  @Override public String toString() {
    return getFullForm();
  }
}
