package com.vesperin.utils;

import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.JavaParser;
import com.vesperin.base.Jdt;
import com.vesperin.base.ScopeAnalyser;
import com.vesperin.base.Source;
import com.vesperin.base.SourceFormat;
import com.vesperin.base.locations.Locations;
import com.vesperin.utils.Immutable;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class UtilsTest {
  static final Source SRC = Source.from("Foo",
    String.join("\n",
      Immutable.listOf(Arrays.asList(
        "public class Foo {"
        , "  public List<String> exit() {"
        , "    return new ArrayList<>();"
        , "  }"
        , "}"
      ))
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

  @Test public void testNodeRecoveryFromBinding() throws Exception {
    final Source src = Source.from("Foo",
      String.join("\n",
        Immutable.listOf(Arrays.asList(
          "public class Foo {"
          , " private int code = 1; "
          , " public int exit(){"
          , "   int x = Config.CODE;"
          , "   return boo();"
          , " }"
          , " "
          , " public int boo(){"
          , "   System.out.println();"
          , "   return code;"
          , " }"
          , " "
          , " public static class Config {"
          , "   static final int CODE = 1;"
          , " }"
          , "}"
        ))
      )
    );

    final Context context = new EclipseJavaParser().parseJava(src);
    final ScopeAnalyser analyser = context.getScopeAnalyser();

    final Set<IBinding> bindings = analyser.getAllBindings(Locations.locate(context.getCompilationUnit()), context.getCompilationUnit());

    for(IBinding each : bindings){
      final ASTNode actualNode = Jdt.findASTNodeDeclaration(each, context.getCompilationUnit());
      assertNotNull(actualNode);
    }
  }
}
