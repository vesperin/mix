package com.vesperin.base;

import com.vesperin.base.visitors.MethodDeclarationVisitor;
import com.vesperin.reflects.AnnotationDefinition;
import com.vesperin.reflects.MethodDefinition;
import com.vesperin.utils.Immutable;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;

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

    for(MethodDeclaration each : methodDeclarationVisitor.getMethodDeclarations()){
      final MethodDefinition definition = MethodDefinition.from(each);
      System.out.println(definition);

      assertEquals(1, definition.getReturnType().getAnnotations().size());
      for (AnnotationDefinition annotationDefinition : definition.getReturnType().getAnnotations()) {
        assertEquals(expectedAnnotationStrValue, annotationDefinition.toString());
      }
    }
  }

  @Test public void testTypeAnnotationWithClassPath() throws Exception {
      final String expectedAnnotationStrValue =
          "com.vesperin.base.TestingTypeAnnotation("
          + "value=public static final com.vesperin.base.TestingTypeAnnotation.TestEnumValue TOP"
          + ")";

      final String workingDir = System.getProperty("user.dir");
      final Context context = new JavaParserConfiguration()
          .setClasspathEntries(Arrays.asList(Paths.get(workingDir, "bin").toString()))
          .configure()
          .parseJava(TypeAnnotatedNeedClassPathSRC);

      MethodDeclarationVisitor methodDeclarationVisitor = new MethodDeclarationVisitor();
      context.accept(methodDeclarationVisitor);

      for(MethodDeclaration each : methodDeclarationVisitor.getMethodDeclarations()){
        final MethodDefinition definition = MethodDefinition.from(each);
        System.out.println(definition);

        assertEquals(1, definition.getReturnType().getAnnotations().size());
        for (AnnotationDefinition annotationDefinition : definition.getReturnType().getAnnotations()) {
          assertEquals(expectedAnnotationStrValue, annotationDefinition.toString());
        }
      }
    }

  @Test public void testBasicParsing() throws Exception {

    final JavaParser parser = new JavaParserConfiguration()
      .setClasspathEntries(null)
      .setSourcepathEntries(null)
      .setEncodings(null)
      .setBindingResolution(true)
      .configure();

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
