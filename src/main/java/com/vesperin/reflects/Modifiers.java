package com.vesperin.reflects;

import com.vesperin.utils.Immutable;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Stream;

public enum Modifiers {
  // Access level
  PRIVATE, PROTECTED, PUBLIC,
  // Other
  ABSTRACT, FINAL, NATIVE, STATIC, SYNCHRONIZED;

  /**
   * Returns a set of method modifiers for a given modifiers value.
   *
   * @param modifier a set of modifiers
   * @return a set of modifiers that {@link JavaMethod} can understand.
   */
  public static Set<Modifiers> from(int modifier){
    final Stream.Builder<Modifiers> builder = Stream.builder();

    setAccessLevel(modifier, builder);
    setAbstract(modifier, builder);
    setStatic(modifier, builder);
    setFinal(modifier, builder);
    setNative(modifier, builder);
    setSync(modifier, builder);

    return Immutable.setOf(builder.build());
  }

  static void setSync(int modifier, Stream.Builder<Modifiers> builder) {
    if (Modifier.isSynchronized(modifier)) {
      builder.accept(SYNCHRONIZED);
    }
  }

  static void setNative(int modifier, Stream.Builder<Modifiers> builder) {
    if (Modifier.isNative(modifier)) {
      builder.accept(NATIVE);
    }
  }

  static void setFinal(int modifier, Stream.Builder<Modifiers> builder) {
    if (Modifier.isFinal(modifier)) {
      builder.accept(FINAL);
    }
  }

  static void setStatic(int modifier, Stream.Builder<Modifiers> builder) {
    if (Modifier.isStatic(modifier)) {
      builder.accept(STATIC);
    }
  }

  static void setAbstract(int modifier, Stream.Builder<Modifiers> builder) {
    if (Modifier.isAbstract(modifier)) {
      builder.accept(ABSTRACT);
    }
  }

  static void setAccessLevel(int modifier, Stream.Builder<Modifiers> builder) {
    if (Modifier.isPublic(modifier)) {
      builder.accept(PUBLIC);
    } else if (Modifier.isProtected(modifier)) {
      builder.accept(PROTECTED);
    } else if (Modifier.isPrivate(modifier)) {
      builder.accept(PRIVATE);
    }
  }

}
