package com.vesperin.common;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.vesperin.common.locations.Locations;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Huascar Sanchez
 */
public class JavaParserTest {
  static final Source SRC = Source.from("Foo",
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


  @Test public void testBasicLocator() throws Exception {

    final JavaParser parser = new EclipseJavaParser();

    final Context parsedContext = parser.parseJava(SRC);
    assertThat(parsedContext.locateClasses().size() == 1, is(true));
    assertThat(parsedContext.locateFields().size() == 0, is(true));
    assertThat(parsedContext.locateMethods().size() == 1, is(true));
    assertThat(parsedContext.locateUnit(Locations.createLocation(SRC, SRC.getContent(), 31, 35)).size() == 1, is(true));

  }
}
