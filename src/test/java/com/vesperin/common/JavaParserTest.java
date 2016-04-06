package com.vesperin.common;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.vesperin.common.visitors.ScopeAnalyser;
import org.eclipse.jdt.core.dom.IBinding;
import org.junit.Test;

import java.util.Set;

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


  static final Source SRC1 = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
        "public class Foo {"
        , " private int code = 1; "
        , " public int exit(){"
        , "   return boo();"
        , " }"
        , " "
        , " public int boo(){"
        , "   return code;"
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

  @Test public void testScopeAnalysis() throws Exception {
    final Context context = new EclipseJavaParser().parseJava(SRC1);
    final ScopeAnalyser analyser = context.getScopeAnalyser();

    final Set<IBinding> universe = analyser.getUsedDeclarationsInScope(44, 81);

    assertThat(universe.size() > 0, is(true));

//    for(IBinding each : universe){
//      if(each.getKind() == IBinding.METHOD){
//        final IMethodBinding methodBinding = (IMethodBinding) each;
//        methodBinding.getDeclaringClass().getName().equals("Object");
//      }
//    }

  }
}
