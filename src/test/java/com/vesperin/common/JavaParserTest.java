package com.vesperin.common;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Huascar Sanchez
 */
public class JavaParserTest {
  @Test public void testBasicParsing() throws Exception {
    final Source src = Source.from("Foo",
      Joiner.on("\n").join(
        ImmutableList.of(
          "public class Foo {"
          , " public int exit(){"
          , "   return 1;"
          , " }"
          , "}"
        )
      )
    );


    final JavaParser parser = new EclipseJavaParser();

    assertNotNull(parser);

    try {
      final Context parsedContext = parser.parseJava(src);
      assertNotNull(parsedContext);

      Context.throwSyntaxErrorIfMalformed(parsedContext, true);

    } catch (Exception e){
      fail("Context should have been well formed");
    }
  }
}
