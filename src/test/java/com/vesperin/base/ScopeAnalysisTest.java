package com.vesperin.base;

import com.vesperin.base.locators.UnitLocation;
import com.vesperin.base.utils.Immutable;
import com.vesperin.base.utils.Sets;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

import java.util.Arrays;
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


  static final Source SRC2 = Source.from("Foo",
    String.join("\n",
      Immutable.listOf(Arrays.asList(
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
      ))
    )
  );

  static final Source SRC3 = Source.from("Foo",
    String.join("\n",
      Immutable.listOf(Arrays.asList(
        "public class Foo {"
        , " private int code = 1; "
        , " public int exit(){"
        , "   System.out.println(code);"
        , "   int x = code;"
        , "   System.out.println(x);"
        , "   return x;"
        , " }"
        , "}"
      ))
    )
  );

  static final Source SRC4 = Source.from("Foo",
    String.join("\n",
      Immutable.listOf(Arrays.asList(
        "public class Foo {"
        , " private int code = 1; "
        , " public int exit(){"
        , "   int x = code;"
        , "   return x;"
        , " }"
        , "}"
      ))
    )
  );

  @Test public void testScopeAnalysisLocalVsUniverse() throws Exception {
    final Context context = new EclipseJavaParser().parseJava(SRC);
    final ScopeAnalyser analyser = context.getScopeAnalyser();

    final Set<IBinding> a = new HashSet<>();
    final Set<IBinding> b = new HashSet<>();

    final Set<IBinding> c = new HashSet<>();
    final Set<IBinding> d = new HashSet<>();

    final UnitLocation methodOne  = context.locateMethods().get(0);
    final UnitLocation methodTwo  = context.locateMethods().get(1);

    // method one
    a.addAll(analyser.getUsedLocalDeclarationsInScope(methodOne));
    assertLocalBindings(a, methodOne);

    b.addAll(analyser.getAllBindings(methodOne, methodOne.getUnitNode()));
    assertBindingUniverse(b, methodOne);


    // method two
    c.addAll(analyser.getUsedLocalDeclarationsInScope(methodTwo));
    assertLocalBindings(c, methodTwo);

    d.addAll(analyser.getAllBindings(methodTwo, methodTwo.getUnitNode()));
    assertBindingUniverse(d, methodTwo);

    // General assertions
    assertThat(!a.isEmpty(), is(true));
    assertThat(!b.isEmpty(), is(true));
    assertThat(!c.isEmpty(), is(true));
    assertThat(!d.isEmpty(), is(true));
    assertThat(b.size() > a.size(), is(true));
    assertThat(d.size() > c.size(), is(true));

  }


  private void assertLocalBindings(Set<IBinding> bindings, UnitLocation unitLocation) {
    if(isExitMethod(unitLocation)){
      final Set<String> expected = new HashSet<>(Arrays.asList("exit", "x", "Config", "boo"));

      for(IBinding each : bindings){
        assertThat(expected.contains(each.getName()), is(true));
      }
    } else if (isBooMethod(unitLocation)){
      final Set<String> expected = new HashSet<>(Arrays.asList("boo", "code", "println"));

      for(IBinding each : bindings){
        assertThat(expected.contains(each.getName()), is(true));
      }
    }
  }


  private void assertBindingUniverse(Set<IBinding> a, UnitLocation unitLocation) {
    if(isBooMethod(unitLocation) || isExitMethod(unitLocation)){
      // {exit(m), x(var), Config(c), boo(m), code(f), println(m)}
      final Set<String> expected = new HashSet<>(Arrays.asList("exit", "Config", "boo", "code", "Foo"));

      for(IBinding each : a){
        assertThat(expected.contains(each.getName()), is(true));
      }
    }
  }

  private static boolean isExitMethod(UnitLocation e){
    return ((MethodDeclaration)e.getUnitNode()).getName().getIdentifier().equals("exit");
  }

  private static boolean isBooMethod(UnitLocation e){
    return ((MethodDeclaration)e.getUnitNode()).getName().getIdentifier().equals("boo");
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

    a.addAll(analyser0.getDeclarationsWithinScope(m0.get()).stream().map(IBinding::getKey).collect(Collectors.toList()));
    b.addAll(analyser1.getDeclarationsWithinScope(m1.get()).stream().map(IBinding::getKey).collect(Collectors.toList()));

    final Set<String> intersection = Sets.intersection(a, b);
    assertThat(!intersection.isEmpty(), is(true));

  }
}
