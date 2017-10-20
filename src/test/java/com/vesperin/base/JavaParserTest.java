package com.vesperin.base;

import com.vesperin.base.utils.Immutable;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Huascar Sanchez
 */
public class JavaParserTest {
  static final Source SRC = Source.from("Foo",
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
