package com.vesperin.base;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.vesperin.base.locators.UnitLocation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class ScopeAnalysisTest {
  static final Source SRC = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
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
      )
    )
  );


  static final Source SRC2 = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
        "public class Foo {"
        , " private int code = 1; "
        , " public int exit(){"
        , "   int x = 1;"
        , "   return x;"
        , " }"
        , " "
        , " public int boo(){"
        , "   System.out.println();"
        , "   return code;"
        , " }"
        , "}"
      )
    )
  );

  static final Source SRC3 = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
        "public class Foo {"
        , " private int code = 1; "
        , " public int exit(){"
        , "   System.out.println(code);"
        , "   int x = code;"
        , "   System.out.println(x);"
        , "   return x;"
        , " }"
        , "}"
      )
    )
  );

  static final Source SRC4 = Source.from("Foo",
    Joiner.on("\n").join(
      ImmutableList.of(
        "public class Foo {"
        , " private int code = 1; "
        , " public int exit(){"
        , "   int x = code;"
        , "   return x;"
        , " }"
        , "}"
      )
    )
  );

  @Test public void testScopeAnalysisLocalVsUniverse() throws Exception {
    final Context context = new EclipseJavaParser().parseJava(SRC);
    final ScopeAnalyser analyser = context.getScopeAnalyser();

    final Set<IBinding> localUniverse = new HashSet<>();
    final Set<IBinding> universe = new HashSet<>();

    for(UnitLocation each : context.locateMethods()){
      localUniverse.addAll(analyser.getUsedLocalDeclarationsInScope(each));
      universe.addAll(analyser.getUsedDeclarationsInScope(each));
    }

    assertThat(!localUniverse.isEmpty(), is(true));
    assertThat(!universe.isEmpty(), is(true));


    assertThat(universe.size() >= localUniverse.size(), is(true));

    final Set<IBinding> intersection = Sets.intersection(localUniverse, universe);
    assertThat(!intersection.isEmpty(), is(true));

  }

  @Test public void testScopeAnalysisLocal() throws Exception {
    final Context context = new EclipseJavaParser().parseJava(SRC2);
    final ScopeAnalyser analyser = context.getScopeAnalyser();

    final Set<IBinding> localUniverse = new HashSet<>();

    final Optional<UnitLocation> selected = context.locateMethods().stream()
      .filter(e -> ((MethodDeclaration)e.getUnitNode()).getName().getIdentifier().equals("exit"))
      .findFirst();

    assertThat(selected.isPresent(), is(true));

    final UnitLocation selectedUnit = selected.get();
    localUniverse.addAll(analyser.getUsedLocalDeclarationsInScope(selectedUnit));

    assertThat(!localUniverse.isEmpty(), is(true));

  }

  @Test public void testUsedFieldNames() throws Exception {
    final Context context = new EclipseJavaParser().parseJava(SRC);
    final ScopeAnalyser analyser = context.getScopeAnalyser();

    final Optional<UnitLocation> mainUnit = context.locateClasses().stream()
      .filter(e -> ((TypeDeclaration)e.getUnitNode()).getName().getIdentifier().equals("Foo"))
      .findFirst();

    assertThat(mainUnit.isPresent(), is(true));

    final Collection<String> names = analyser.getUsedFieldNames(mainUnit.get());
    assertThat(names.isEmpty(), is(false));
    // SRC has only one field, therefore we expect one entry in the collection
    assertThat(names.size() == 1, is(true));

  }

  @Test public void testScopeIntersection() throws Exception {
    final Context context0 = new EclipseJavaParser().parseJava(SRC3);
    final Context context1 = new EclipseJavaParser().parseJava(SRC4);
    final ScopeAnalyser analyser0 = context0.getScopeAnalyser();
    final ScopeAnalyser analyser1 = context1.getScopeAnalyser();

    final Set<String> a = new HashSet<>();
    final Set<String> b = new HashSet<>();

    final Optional<UnitLocation> m0 = context0.locateMethods().stream()
      .filter(e -> ((MethodDeclaration)e.getUnitNode()).getName().getIdentifier().equals("exit"))
      .findFirst();

    final Optional<UnitLocation> m1 = context0.locateMethods().stream()
      .filter(e -> ((MethodDeclaration)e.getUnitNode()).getName().getIdentifier().equals("exit"))
      .findFirst();

    assertThat(m0.isPresent(), is(true));
    assertThat(m1.isPresent(), is(true));

    a.addAll(analyser0.getUsedDeclarationsInScope(m0.get()).stream().map(IBinding::getKey).collect(Collectors.toList()));
    b.addAll(analyser1.getUsedDeclarationsInScope(m1.get()).stream().map(IBinding::getKey).collect(Collectors.toList()));

    final Set<String> intersection = Sets.intersection(a, b);
    assertThat(!intersection.isEmpty(), is(true));

  }
}
