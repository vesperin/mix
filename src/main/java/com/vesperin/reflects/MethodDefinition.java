package com.vesperin.reflects;

import com.vesperin.utils.Expect;
import com.vesperin.utils.Immutable;
import com.vesperin.base.Jdt;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.vesperin.reflects.ClassDefinition.classDefinition;
import static com.vesperin.reflects.ClassDefinition.returnClassDefinition;

/**
 * @author Huascar Sanchez
 */
public class MethodDefinition {
  private final Set<AnnotationDefinition> annotations;
  private final Set<ClassDefinition>      exceptions;
  private final List<ClassDefinition>     arguments;
  private final List<ClassDefinition>     typeParameters;
  private final ClassDefinition           returnType;
  private final ClassDefinition           declaringClass;
  private final Set<MethodModifier>       modifiers;
  private final String                    methodName;
  private final String                    simpleForm;
  private final String                    fullForm;
  private final boolean                   isDeprecated;
  private final boolean                   isStatic;


  /**
   * Constructs a new method definition object for a given a set of arguments.
   *
   * @param methodName the name of this method
   * @param arguments the method's arguments.
   * @param returnType the method's return type
   * @param annotations the method's annotations
   * @param exceptions the exceptions this method throws
   * @param isDeprecated true if this method is deprecated; false otherwise.
   * @param declaringClass the class where this method resides.
   */
  private MethodDefinition(String methodName, List<ClassDefinition>  arguments,
          ClassDefinition returnType, Set<AnnotationDefinition> annotations,
          Set<ClassDefinition> exceptions, List<ClassDefinition> typeParameters,
          Set<MethodModifier> modifiers, boolean isDeprecated,
          ClassDefinition declaringClass){


    this.methodName     = Expect.nonNull(methodName);
    this.returnType     = Expect.nonNull(returnType);
    this.declaringClass = Expect.nonNull(declaringClass);
    this.annotations    = annotations;
    this.exceptions     = exceptions;

    this.arguments      = arguments;
    this.typeParameters = typeParameters;

    this.modifiers      = modifiers;

    this.isStatic     = getModifiers().contains(MethodModifier.Other.STATIC);
    this.isDeprecated = isDeprecated;
    this.simpleForm   = methodFormat(ClassDefinition::getSimpleForm, () -> "");

    this.fullForm = methodFormat(
      ClassDefinition::getCanonicalName,
      () -> declaringClass.getCanonicalName() + (isStatic ? "." : "#") + this.getMethodName() + ": "
    );

  }

  /**
   * Returns all methods declared in a class.
   */
  public static Stream<MethodDefinition> allMethods(Class<?> klass) {
    return Stream.concat(
      allPublicConstructors(klass),
      allDefinitionsStream(allMethodStream(klass), klass.getName().equals("java.lang.Object"))
    );
  }

  private static Stream<Method[]> allMethodStream(Class<?> klass){
    return Stream.of(klass).map(t -> {
      try {
        return t.getDeclaredMethods();
      } catch (NoClassDefFoundError e) {
        return new Method[]{};
      }
    });
  }

  private static Stream<MethodDefinition> allPublicConstructors(Class<?> klass){
    try {
      return Arrays.stream(klass.getDeclaredConstructors())
        .filter(c -> !"java.lang.Object".equals(c.getDeclaringClass().getName()))
        .filter(c -> Modifier.isPublic(c.getModifiers()))
        .map(MethodDefinition::from);
    } catch (Throwable ignored){
      return Stream.empty();
    }

  }

  private static Stream<MethodDefinition> allDefinitionsStream(Stream<Method[]> stream, boolean inObject){
    return stream.flatMap(Stream::of)
      .filter(c -> inObject != MethodDefinition.isUndefinedInObjectClass(c))
      .map(MethodDefinition::from);
  }

  /**
   * Creates a new method definition from a {@link MethodDeclaration}
   *
   * @param method the {@link MethodDeclaration} object.
   * @return a new method definition.
   */
  public static MethodDefinition from(MethodDeclaration method){
    final IMethodBinding methodBinding = Expect.nonNull(method.resolveBinding());

    final CompilationUnit unit  = Jdt.parent(CompilationUnit.class, method);
    Expect.nonNull(unit);

    final String methodName = methodBinding.getName();
    final List<ClassDefinition> arguments = Immutable.listOf(
      Arrays.stream(methodBinding.getParameterTypes()).map(t -> classDefinition(unit, t))
    );


    final Set<ClassDefinition> exceptions = Immutable.setOf(
      Arrays.stream(methodBinding.getExceptionTypes()).map(t -> classDefinition(unit, t))
    );

    final Set<AnnotationDefinition> annotations = Immutable.setOf(
      Arrays.stream(methodBinding.getAnnotations()).map(MethodDefinition::annotationDefinition)
    );

    final List<ClassDefinition> typeParameters = Immutable.listOf(
      Arrays.stream(methodBinding.getTypeParameters()).map(t -> classDefinition(unit, t))
    );

    final Set<MethodModifier> modifiers = Immutable.setOf(MethodModifier.from(methodBinding.getModifiers()));
    final boolean isDeprecated  = methodBinding.isDeprecated();

    final ClassDefinition    declaringClass = classDefinition(unit, methodBinding.getDeclaringClass());


    final ClassDefinition returnType = returnClassDefinition(unit, methodBinding);

    return from(methodName, arguments,
      returnType, annotations, exceptions, typeParameters,
      modifiers, isDeprecated, declaringClass);

  }

  /**
   * Creates a new method definition from an array of arguments.
   *
   * @param methodName the name of this method
   * @param arguments the method's arguments.
   * @param returnType the method's return type
   * @param annotations the method's annotations
   * @param exceptions the exceptions this method throws
   * @param isDeprecated true if this method is deprecated; false otherwise.
   * @param declaringClass the class where this method resides.
   * @return a new Method Definition.
   */
  public static MethodDefinition from(String methodName, List<ClassDefinition>  arguments,
         ClassDefinition returnType, Set<AnnotationDefinition> annotations,
         Set<ClassDefinition> exceptions, List<ClassDefinition> typeParameters,
         Set<MethodModifier> modifiers, boolean isDeprecated,
         ClassDefinition declaringClass){

    return new MethodDefinition(methodName, arguments,
      returnType, annotations, exceptions, typeParameters,
      modifiers, isDeprecated, declaringClass);
  }

  private static AnnotationDefinition annotationDefinition(IAnnotationBinding annotationBinding){
    final StringBuilder annotation = new StringBuilder(annotationBinding.getAnnotationType().getQualifiedName());
    annotation.append("(");

    final List<String> entries = new ArrayList<>();

    for(IMemberValuePairBinding each : annotationBinding.getDeclaredMemberValuePairs()){
      final String key = each.getName();
      final String val = String.valueOf(each.getValue());

      final String entry = key + "=" + val;
      entries.add(entry);

    }

    annotation.append(entries.toString().replace("[", "").replace("]", ""));

    annotation.append(")");

    return new AnnotationDefinition(annotation.toString());
  }

  /**
   * Creates a new method definition from a {@link Constructor}
   *
   * @param constructor the {@link Constructor} object
   * @return a new method definition
   */
  public static MethodDefinition from(Constructor<?> constructor){
    final String          constructorName = "new";

    final ClassDefinition declaringClass  = ClassDefinition.forceGeneric(constructor.getDeclaringClass());

    final Set<AnnotationDefinition> annotations = Immutable.setOf(
      Stream.of(constructor.getAnnotations()).map(AnnotationDefinition::new));

    final Set<ClassDefinition> exceptions   = Immutable.setOf(
      Stream.of(constructor.getExceptionTypes()).map(ClassDefinition::from));

    final List<ClassDefinition> arguments    = Immutable.listOf(
      Stream.of(genericParameterTypes(constructor)).map(ClassDefinition::from));

    final List<ClassDefinition> typeParameters = Immutable.listOf(Stream.of(constructor.getTypeParameters())
      .map(ClassDefinition::from));

    final Set<MethodModifier> modifiers = Immutable.setOf(MethodModifier.from(constructor));
    final boolean isDeprecated  = constructor.getAnnotation(Deprecated.class) != null;

    return new MethodDefinition(constructorName, arguments,
      declaringClass, annotations, exceptions, typeParameters, modifiers,
      isDeprecated, declaringClass);

  }


  /**
   * Creates a new method definition from a {@link Method}
   *
   * @param method the {@link Method} object.
   * @return a new method definition.
   */
  public static MethodDefinition from(Method method) {

    final String          methodName     = method.getName();
    final ClassDefinition returnType     = missingReturnTypeOrElse(method);

    final ClassDefinition declaringClass = ClassDefinition.forceGeneric(method.getDeclaringClass());

    final Set<AnnotationDefinition> annotations = Immutable.setOf(
      Stream.of(method.getAnnotations()).map(AnnotationDefinition::new));

    final Set<ClassDefinition> exceptions   = Immutable.setOf(
      Stream.of(method.getExceptionTypes()).map(ClassDefinition::from));

    final List<ClassDefinition> arguments    = Immutable.listOf(
      Stream.of(genericParameterTypes(method)).map(ClassDefinition::from));

    final List<ClassDefinition> typeParameters = Immutable.listOf(Stream.of(method.getTypeParameters())
      .map(ClassDefinition::from));

    final Set<MethodModifier> modifiers = Immutable.setOf(MethodModifier.from(method));
    final boolean isDeprecated  = method.getAnnotation(Deprecated.class) != null;


    return new MethodDefinition(methodName, arguments,
      returnType, annotations, exceptions, typeParameters, modifiers,
      isDeprecated, declaringClass);
  }

  private static ClassDefinition missingReturnTypeOrElse(Method method){
    try {
      return ClassDefinition.from(method.getGenericReturnType());
    } catch (NoClassDefFoundError ignored){
      return ClassDefinition.missingClassDefinition();
    }
  }

  private static Type[] genericParameterTypes(Method method){
    try {
      return method.getGenericParameterTypes();
    } catch (NoClassDefFoundError ignored){
      return new Type[0];
    }
  }

  private static Type[] genericParameterTypes(Constructor<?> method){
    try {
      return method.getGenericParameterTypes();
    } catch (NoClassDefFoundError ignored){
      return new Type[0];
    }
  }


  private static String formatArgumentsInSimpleNotation(List<ClassDefinition> arguments,
          Function<ClassDefinition, String> mapper) {

    switch (arguments.size()) {
      case 0:
        return "()";
      case 1:
        // arg
        return mapper.apply(arguments.get(0));
      default:
        // (arg1, arg2)
        final StringJoiner joiner = new StringJoiner(", ", "(", ")");

        for (ClassDefinition eachArg : arguments) {
          joiner.add(mapper.apply(eachArg));
        }

        return joiner.toString();
    }
  }

  /**
   * Utility method that returns true if a {@link Method} is abstract, and
   * returns false if it is not.
   *
   * @param method the method to check.
   * @return true if abstract method; false otherwise.
   */
  static boolean isAbstract(Method method) {
    return !method.isDefault();
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

  /**
   * @return true if method is deprecated; false otherwise.
   */
  public boolean isDeprecated() {
    return isDeprecated;
  }

  public static Predicate<MethodDefinition> isRelevantMethodDefinition() {
    return p -> !p.isDeprecated();
  }

  /**
   * @return true if method is static; false otherwise.
   */
  public boolean isStatic() {
    return isStatic;
  }

  /**
   * @return all declared annotations for this method.
   */
  public Set<AnnotationDefinition> getDeclaredAnnotations() {
    return annotations;
  }

  /**
   * @return this method's declaring class.
   */
  public ClassDefinition getDeclaringClass() {
    return declaringClass;
  }

  /**
   * @return this method's exceptions
   */
  public Set<ClassDefinition> getExceptions() {
    return exceptions;
  }

  /**
   * @return this method's arguments.
   */
  public List<ClassDefinition> getArguments() {
    return arguments;
  }

  /**
   * @return this method's return type.
   */
  public ClassDefinition getReturnType() {
    return returnType;
  }

  /**
   * @return this method's type parameters.
   */
  public List<ClassDefinition> getTypeParameters() {
    return typeParameters;
  }

  /**
   * @return this method's modifiers.
   */
  public Set<MethodModifier> getModifiers() {
    return modifiers;
  }

  /**
   * @return the name of this method.
   */
  public String getMethodName() {
    return methodName;
  }

  // todo
  public boolean sameName(String name){
    return getMethodName().equals(name);
  }

  public boolean sameArgCount(int count){
    return getArguments().size() == count;
  }

  /**
   * @return this method's type signature in simple form.
   */
  public String getSimpleForm() {
    return simpleForm;
  }

  /**
   * @return this method's type signature in full form (verbose). One can
   * also read its simple form by invoking the {@link #getSimpleForm()} method.
   */
  public String getFullForm() {
    return fullForm;
  }

  private String methodFormat(final Function<ClassDefinition, String> name, final Supplier<String> begin) {
    final List<ClassDefinition> args = new ArrayList<>();
    if (!getModifiers().contains(MethodModifier.Other.STATIC)) {
      args.add(declaringClass);
    }

    args.addAll(new ArrayList<>(arguments));

    final String argumentsString = formatArgumentsInSimpleNotation(args, name);
    return String.format("%s%s -> %s", begin.get(), argumentsString, name.apply(returnType));
  }

  @Override public String toString() {
    return "MethodDefinition {"
      + getFullForm() + "}.";
  }
}
