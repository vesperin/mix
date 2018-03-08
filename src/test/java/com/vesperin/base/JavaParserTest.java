package com.vesperin.base;

import com.vesperin.base.locators.UnitLocation;
import com.vesperin.base.visitors.MethodDeclarationVisitor;
import com.vesperin.reflects.AnnotationDefinition;
import com.vesperin.reflects.MethodDefinition;
import com.vesperin.utils.Expect;
import com.vesperin.utils.Immutable;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Huascar Sanchez
 */
public class JavaParserTest {
  private static final Source SRC = Source.from("Foo",
    String.join("\n",
      Immutable.listOf(Arrays.asList(
        "public class Foo {"
        , " public int exit(){"
        , "   return 1;"
        , " }"
        , "}"
      ))
    )
  );


  private static final Source TypeAnnotatedSRC = Source.from("Example",
    String.join("\n",
      Immutable.listOf(Arrays.asList(
        ""
        ,"import java.lang.annotation.ElementType;"
        ,"import java.lang.annotation.Retention;"
        ,"import java.lang.annotation.RetentionPolicy;"
        ,"import java.lang.annotation.Target;"
        ,""
        ,"public class Example {"
        , " @Retention(RetentionPolicy.RUNTIME)"
        , " @Target({ ElementType.TYPE_USE,"
        , " ElementType.TYPE_PARAMETER })"
        , " @interface TestingAll{"
        , "   String[] value();"
        , " }"
        , " public @TestingAll(\"1\") int exit(){"
        , "   return 1;"
        , " }"
        , "}"
        ))
          )
  );

  private static final Source TypeAnnotatedNeedClassPathSRC = Source.from("Example",
    String.join("\n",
      Immutable.listOf(Arrays.asList(
        ""
        ,"import com.vesperin.base.TestingTypeAnnotation;"
        ,""
        ,"public class Example {"
        , " public @TestingTypeAnnotation(TestingTypeAnnotation.TestEnumValue.TOP) int exit(){"
        , "   return 1;"
        , " }"
        , "}"
        ))
          )
  );

  @Test public void testTypeAnnotation() throws Exception {
    final String expectedAnnotationStrValue = "Example.TestingAll(value=1)";
    final Context context = new EclipseJavaParser().parseJava(TypeAnnotatedSRC);
    MethodDeclarationVisitor methodDeclarationVisitor = new MethodDeclarationVisitor();
    context.accept(methodDeclarationVisitor);

    testMethodDeclarations(expectedAnnotationStrValue, methodDeclarationVisitor);
  }

  @Test public void testTypeAnnotationWithClassPath() throws Exception {
      final String expectedAnnotationStrValue =
          "com.vesperin.base.TestingTypeAnnotation("
          + "value=public static final com.vesperin.base.TestingTypeAnnotation.TestEnumValue TOP"
          + ")";

      final String workingDir = System.getProperty("user.dir");
      final JavaParser parser = new EclipseJavaParser(new Configuration() {
        @Override public void configure(JavaParser parser) {
          configureCompilerOptions(parser);
          configureEnvironment(
            Collections.singletonList(Paths.get(workingDir, "target/test-classes").toString()),
            null,
            null,
            parser
          );
          configureBindings(parser);
          cleanupAfter(parser);
        }
      });

      final Context context = parser.parseJava(TypeAnnotatedNeedClassPathSRC);

      MethodDeclarationVisitor methodDeclarationVisitor = new MethodDeclarationVisitor();
      context.accept(methodDeclarationVisitor);

    testMethodDeclarations(expectedAnnotationStrValue, methodDeclarationVisitor);
  }

  private static void testMethodDeclarations(String expectedAnnotationStrValue, MethodDeclarationVisitor methodDeclarationVisitor) {
    for(MethodDeclaration each : methodDeclarationVisitor.getMethodDeclarations()){
      final MethodDefinition definition = MethodDefinition.from(each);
      System.out.println(definition);

      assertEquals(1, definition.getReturnType().getAnnotations().size());
      for (AnnotationDefinition annotationDefinition : definition.getReturnType().getAnnotations()) {
        assertEquals(expectedAnnotationStrValue, annotationDefinition.toString());
      }
    }
  }

  @Test public void testJavaCodeFromResources() throws Exception {
    Path file  = Paths.get(JavaParserTest.class.getResource("/JamaUtils.java").toURI());
    final Source code = Source.from(file.toFile());
    final Context context = new EclipseJavaParser().parseJava(code);
    assertNotNull(context);

    for (UnitLocation each : context.locateMethods()){
      final MethodDeclaration declaration = (MethodDeclaration) each.getUnitNode();
      final IMethodBinding binding = declaration.resolveBinding();
      assertNotNull(binding);
    }

  }


  @Test public void testBasicParsing() throws Exception {

    final JavaParser parser = new EclipseJavaParser();

    assertNotNull(parser);

    try {
      final Context parsedContext = parser.parseJava(SRC);
      assertNotNull(parsedContext);

      Context.throwSyntaxErrorIfMalformed(parsedContext, true);

    } catch (Exception e){
      fail("Context should have been well formed");
    }
  }
}
