package com.vesperin.reflects;

import com.vesperin.utils.Expect;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.vesperin.reflects.ClassDefinition.typeNameOrMissing;

/**
 * @author Huascar Sanchez
 */
public enum TypeLiteral {
  /** Primitive types **/
  PRIMITIVE (new PrimitiveMatcher()),

  /** The void type **/
  VOID (new VoidMatcher()),

  /** Array type **/
  ARRAY (new ArrayMatcher()),

  /** Generic array **/
  GENERIC_ARRAY (new GenericArrayMatcher()),

  /** Parameterized type **/
  PARAMETERIZED_TYPE (new ParameterizedTypeMatcher()),

  /** Functional interface **/
  FUNCTIONAL_INTERFACE (new FunctionalInterfaceMatcher()),

  /** type variable**/
  TYPE_VARIABLE (new TypeVariableMatcher()),

  INTERFACE(null),

  /** class **/
  CLASS (null);

  final TypeMatcher matcher;

  /**
   * Construct a new TypeLiteral enum.
   *
   * @param matcher the condition that represents the enum instance.
   */
  TypeLiteral(TypeMatcher matcher){
    this.matcher = matcher;
  }

  private boolean matches(Type type){
    return this.matcher != null && this.matcher.matches(type);
  }


  public static TypeLiteral from(Type type){

    final Type nonNull = Expect.nonNull(type);

    for( TypeLiteral each : values()){
      if(each.matches(nonNull)){
        return each;
      }
    }

    final boolean isInterface = nonNull.toString().contains("interface");

    return isInterface ? INTERFACE : CLASS;
  }

  public static TypeLiteral voidOrClass(String name){
    if("java.lang.Void".equals(name) ||  "void".equals(name)) {
      return TypeLiteral.VOID;
    }

    return TypeLiteral.CLASS;
  }


  interface TypeMatcher extends Matcher<Boolean, Type> {
    @Override Boolean matches(Type input);
  }

  static abstract class AbstractTypeMatcher implements TypeMatcher {
    TypeMatcher not(TypeMatcher other){
      return new NotMatcher(other);
    }
  }

  static class NotMatcher implements TypeMatcher {
    final TypeMatcher matcher;

    NotMatcher(TypeMatcher matcher){
      this.matcher = Objects.requireNonNull(matcher);
    }

    @Override public Boolean matches(Type input) {
      return !matcher.matches(input);
    }
  }

  static class ParameterizedTypeMatcher extends AbstractTypeMatcher {
    final TypeMatcher negatedFunctionalInterface;

    ParameterizedTypeMatcher(){
      this.negatedFunctionalInterface = not(
          new FunctionalInterfaceMatcher()
      );
    }

    @Override public Boolean matches(Type input) {
      return (input instanceof ParameterizedType)
          && negatedFunctionalInterface.matches(input);
    }
  }


  static class FunctionalInterfaceMatcher extends AbstractTypeMatcher {
    @Override public Boolean matches(Type input) {
      Type typeOfInterest = input;
      if( input instanceof ParameterizedType) {
        typeOfInterest = ((ParameterizedType) input).getRawType();
      }

      return (typeOfInterest instanceof Class
          && isFunctionalInterface((Class) typeOfInterest)
      );
    }

    static boolean isFunctionalInterface(Class<?> klass) {

      if (klass.isInterface()) {
        final Annotation annotation = klass.getAnnotation(FunctionalInterface.class);
        if (annotation != null) { return true; }

        final Stream<Method> declaredMethods    = methodsOfaNoClassDefFound(klass);
        final Predicate<Method> isDefault       = Method::isDefault;
        final Predicate<Method> isAbstract      = isDefault.negate();

        return declaredMethods.filter(isAbstract).count() == 1;
      }

      return false;
    }

    static Stream<Method> methodsOfaNoClassDefFound(Class<?> klass){
      try {
        return Stream.of(klass.getDeclaredMethods());
      } catch (NoClassDefFoundError | TypeNotPresentException ignore){
        return Stream.empty();
      }
    }
  }

  static class TypeVariableMatcher extends AbstractTypeMatcher {
    @Override public Boolean matches(Type input) {
      return (input instanceof TypeVariable);
    }
  }

  static class PrimitiveMatcher extends AbstractTypeMatcher {
    @Override public Boolean matches(Type input) {
      final String typeName = typeNameOrMissing(input);

      switch (typeName) {
        case "int":
        case "long":
        case "float":
        case "double":
        case "short":
        case "boolean":
        case "byte":
        case "char":
          return true;
      }

      return false;
    }
  }

  static class VoidMatcher extends AbstractTypeMatcher {
    @Override public Boolean matches(Type input) {
      final String typeName = typeNameOrMissing(input);
      return "void".equals(typeName);
    }
  }

  static class ArrayMatcher extends AbstractTypeMatcher {
    @Override public Boolean matches(Type input) {
      final String typeName = typeNameOrMissing(input);
      return typeName.endsWith("[]") && !(input instanceof GenericArrayType);
    }
  }

  static class GenericArrayMatcher extends AbstractTypeMatcher {
    @Override public Boolean matches(Type input) {
      final String typeName = typeNameOrMissing(input);
      return typeName.endsWith("[]") && (input instanceof GenericArrayType);
    }
  }
}
