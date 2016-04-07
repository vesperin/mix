package com.vesperin.base.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.JavaParser;
import com.vesperin.base.Source;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class UtilsTest {
  static final Source SRC = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
        "public class Foo {"
        , "  public List<String> exit() {"
        , "    return new ArrayList<>();"
        , "  }"
        , "}"
      )
    )
  );

  @Test public void testJDTBasicMethods() throws Exception {
    final JavaParser parser = new EclipseJavaParser();

    final Context context = parser.parseJava(SRC);

    final Set<String> imports = Jdt.collectImportCandidates(context);

    assertTrue(!imports.isEmpty());
    assertTrue(imports.size() == 3);

  }

  @Test public void testSourceReformatting() throws Exception {
    final String unformatted = "public class Foo {public List<String> exit(){return new ArrayList<>();}}";
    final String formatted   = SourceFormat.format(unformatted).trim();

    assertEquals(SRC.getContent(), formatted);

  }

  @Test public void testStringTemplating() throws Exception {

  }
}
