package com.vesperin.base.locators;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.Source;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.vesperin.base.locations.Locations.createLocation;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class UnitLocatorsTest {
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


  static Context parsedContext;
  static UnitLocator locator;

  @BeforeClass public static void setUp() throws Exception {
    parsedContext = new EclipseJavaParser().parseJava(SRC);
    locator       = new ProgramUnitLocator(parsedContext);
  }


  @Test public void testClassUnitLocation() throws Exception {
    final List<UnitLocation> locations = locator.locate(new ClassUnit("Foo"));

    sharedAssertionChecking(locations);
    assertNodeType(locations, ASTNode.TYPE_DECLARATION);
  }

  @Test public void testMethodUnitLocation() throws Exception {
    final List<UnitLocation> locations = locator.locate(new MethodUnit("exit"));

    sharedAssertionChecking(locations);
    assertNodeType(locations, ASTNode.METHOD_DECLARATION);
  }

  @Test public void testNodeSelection() throws Exception {
    final List<UnitLocation> locations = locator.locate(new SelectedUnit(createLocation(SRC, SRC.getContent(), 31, 35)));
    sharedAssertionChecking(locations);
    assertNodeType(locations, ASTNode.SIMPLE_NAME);
  }


  @Test public void testMultipleOccurrencesSelection() throws Exception {
    final Source src = Source.from("Foo",
      Joiner.on("\n").join(
        ImmutableList.of(
          "public class Foo {"
          , " private final int code = 0; "
          , " public int exit(){"
          , "   return code;"
          , " }"
          , "}"
        )
      )
    );

    final Context context = new EclipseJavaParser().parseJava(src);
    final UnitLocator locator = new ProgramUnitLocator(context);
    final List<UnitLocation> locations = locator.locate(new FieldUnit("code"));

    sharedAssertionChecking(locations);
    assertNodeType(locations, ASTNode.FIELD_DECLARATION);

    assertThat(context.locate(new FieldUnit()).size() == 1, is(true));

  }

  @Test public void testAllMethodsLocation() throws Exception {
    final Source src = Source.from("Foo",
      Joiner.on("\n").join(
        ImmutableList.of(
          "public class Foo {"
          , " private final int code = 0; "
          , " public int exit(){"
          , "   return code;"
          , " }"
          , " "
          , " public int exit1(){"
          , "   return code;"
          , " }"
          , "}"
        )
      )
    );

    final Context context = new EclipseJavaParser().parseJava(src);
    final List<UnitLocation> locations = context.locate(new MethodUnit());

    assertThat(locations.isEmpty(), is(false));
    assertThat(locations.size() == 2, is(true));
  }

  private static void sharedAssertionChecking(List<UnitLocation> locations) {
    assertThat(locations.isEmpty(), is(false));
    assertThat(locations.size() == 1, is(true));
  }

  private static void assertNodeType(List<UnitLocation> locations, int nodeType){
    final ProgramUnitLocation pul = (ProgramUnitLocation) locations.get(0);
    assertEquals(pul.getUnitNode().getNodeType(), nodeType);
  }



  @AfterClass public static void tearDown() {
    parsedContext = null;
    locator       = null;
  }
}
