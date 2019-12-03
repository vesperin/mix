package com.vesperin.reflects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

public class JavaReflectsTest {

  static class Hello {
    public @TestingAll("1") int foo(){
      return 1;
    }
  }

  @Test public void testTypeAnnotation() {
    for (Method each : Hello.class.getDeclaredMethods()){
      assertFalse(JavaMethod.from(each).getReturnType().getAnnotations().isEmpty());
    }
  }

  @Test public void testFindingClasses() throws Exception {
    JavaClasses.publicClassesKnownInJRE().forEach(Assert::assertNotNull);
  }

  @Test public void testFindingClassesInClassloader() throws Exception {
    JavaClasses.publicClasses(Thread.currentThread().getContextClassLoader())
        .forEach(Assert::assertNotNull);
  }
}
