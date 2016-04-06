package com.vesperin.common;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.vesperin.common.locations.Locations;
import com.vesperin.common.locators.UnitLocation;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class ContextTest {
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

  static final Source SRC1 = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
        "public class Foo {"
        , " public class Boo {"
        , " }"
        , "}"
      )
    )
  );

  @Test public void testBasicLocator() throws Exception {

    final JavaParser parser = new EclipseJavaParser();

    final Context parsedContext = parser.parseJava(SRC);
    assertThat(parsedContext.locateClasses().size() == 1, is(true));
    assertThat(parsedContext.locateFields().size() == 0, is(true));
    assertThat(parsedContext.locateMethods().size() == 1, is(true));
    assertThat(parsedContext.locateUnit(31, 35).size() == 1, is(true));
  }

  @Test public void testBasicLocations() throws Exception {

    final JavaParser parser = new EclipseJavaParser();

    final Context parsedContext = parser.parseJava(SRC);

    for( UnitLocation each : parsedContext.locateMethods()){
      assertThat(Locations.covers(parsedContext.getScope(), each), is(true));
    }
  }


  @Test public void testBasicLocators2() throws Exception {

    final JavaParser parser = new EclipseJavaParser();

    final Context parsedContext = parser.parseJava(SRC1);
    assertThat(parsedContext.locateClasses().size() == 2, is(true));

  }


}
