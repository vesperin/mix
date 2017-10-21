package com.vesperin.reflects;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * @author huascar.sanchez@sri.com (Huascar Sanchez)
 */
public interface MethodModifier {
  static Stream<MethodModifier> from(Constructor<?> constructor) {
    final int modifier = constructor.getModifiers();
    return from(modifier);
  }

  static Stream<MethodModifier> from(Method method) {
    final int modifier = method.getModifiers();
    return from(modifier);
  }

  static Stream<MethodModifier> from(int modifier){
    final Stream.Builder<MethodModifier> builder = Stream.builder();

    setAccessLevel(modifier, builder);
    setAbstract(modifier, builder);
    setStatic(modifier, builder);
    setFinal(modifier, builder);
    setNative(modifier, builder);
    setSync(modifier, builder);

    return builder.build();
  }

  static void setSync(int modifier, Stream.Builder<MethodModifier> builder) {
    if (Modifier.isSynchronized(modifier)) {
      builder.accept(Other.SYNCHRONIZED);
    }
  }

  static void setNative(int modifier, Stream.Builder<MethodModifier> builder) {
    if (Modifier.isNative(modifier)) {
      builder.accept(Other.NATIVE);
    }
  }

  static void setFinal(int modifier, Stream.Builder<MethodModifier> builder) {
    if (Modifier.isFinal(modifier)) {
      builder.accept(Other.FINAL);
    }
  }

  static void setStatic(int modifier, Stream.Builder<MethodModifier> builder) {
    if (Modifier.isStatic(modifier)) {
      builder.accept(Other.STATIC);
    }
  }

  static void setAbstract(int modifier, Stream.Builder<MethodModifier> builder) {
    if (Modifier.isAbstract(modifier)) {
      builder.accept(Other.ABSTRACT);
    }
  }

  static void setAccessLevel(int modifier, Stream.Builder<MethodModifier> builder) {
    if (Modifier.isPublic(modifier)) {
      builder.accept(AccessLevel.PUBLIC);
    } else if (Modifier.isProtected(modifier)) {
      builder.accept(AccessLevel.PROTECTED);
    } else if (Modifier.isPrivate(modifier)) {
      builder.accept(AccessLevel.PRIVATE);
    }
  }

  enum AccessLevel implements MethodModifier {
    PRIVATE, PROTECTED, PUBLIC
  }

  enum Other implements MethodModifier {
    ABSTRACT, FINAL, NATIVE, STATIC, SYNCHRONIZED
  }
}
