package com.vesperin.reflects;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

public class JavaReflectsTest {

  static class Hello {
    public @TestingAll("1") int foo(){
      return 1;
    }
  }

  @Test public void testTypeAnnotation() throws Exception {
    for (Method each : Hello.class.getDeclaredMethods()){
      assertTrue(!MethodDefinition.from(each).getReturnType().getAnnotations().isEmpty());
    }
  }
}
