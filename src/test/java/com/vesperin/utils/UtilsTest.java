package com.vesperin.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.vesperin.base.CommonJdt;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.JavaParser;
import com.vesperin.base.ScopeAnalyser;
import com.vesperin.base.Source;
import com.vesperin.base.SourceFormat;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.visitors.MethodDeclarationVisitor;
import com.vesperin.base.visitors.SkeletalVisitor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.Test;

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


  @Test public void testSetOps() {
    final Set<Integer> a = Immutable.setOf(Arrays.asList(1, 2, 3));
    final Set<Integer> b = Immutable.setOf(Arrays.asList(2, 4, 5));

    Set<Integer> ab = Sets.union(a, b);
    Set<Integer> sab = Immutable.setOf(Stream.concat(a.stream(), b.stream()));
    Set<Integer> oab = Immutable.setOf(Iterables.merge(a, b));

    assertEquals(ab, sab);
    assertEquals(ab, oab);

  }

  @Test public void testSetSim() {
    final Set<Integer> a = Immutable.setOf(Arrays.asList(1, 2, 3));
    final Set<Integer> b = Immutable.setOf(Arrays.asList(2, 4, 5));
    final double score = Sets.similarityCoefficient(a, b);

    assertTrue(Double.compare(score, 0) >= 0);

  }

  @Test public void testJDTBasicMethods() {
    final JavaParser parser = new EclipseJavaParser();

    final Context context = parser.parseJava(SRC);

    final Set<String> imports = CommonJdt.getImportStatements(context);

    assertFalse(imports.isEmpty());
    assertEquals(3, imports.size());

  }

  @Test public void testSourceReformatting() {
    final String unformatted = "public class Foo {public List<String> exit(){return new ArrayList<>();}}";
    final String formatted   = SourceFormat.format(unformatted).trim();

    assertEquals(SRC.getContent(), formatted);

  }


  @Test public void testTypeNormalization() {
    final JavaParser parser = new EclipseJavaParser();

    final Source src = Source.from("Foo",
      String.join("\n",
        Immutable.listOf(Arrays.asList(
          "public class Foo {"
          , "  public List<String> exit() {"
          , "    final List<String> a = new TypeNode<ArrayList<Data>>(new ArrayList<>()){}.list();"
          , "    return a;"
          , "  }"
          , "}"
        ))
      )
    );

    final Context context = parser.parseJava(src);
    final InvokesVisitor invokesVisitor = new InvokesVisitor();
    context.accept(invokesVisitor);

    for(MethodInvocation invoke : invokesVisitor.invocationSet()){
      final Expression left = invoke.getExpression();
      assertNotNull(left);

      final ITypeBinding actualType = left.resolveTypeBinding();
      assertNotNull(actualType);

      final ITypeBinding currentType = CommonJdt.normalizeTypeBinding(actualType);

      assertNotEquals(actualType, currentType);
    }

  }

  @Test public void testTypeNormalization2() {
    final Source src = Source.from("Foo",
      String.join("\n",
        Immutable.listOf(Arrays.asList(
          "import java.util.Objects;",
          "public class Foo {"
          , "  public int exit() {"
          , "    final int hash = Objects.hash(1);"
          , "    return hash;"
          , "  }"
          , "}"
        ))
      )
    );

    final JavaParser parser = new EclipseJavaParser();
    final Context context = parser.parseJava(src);

    final MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
    context.accept(visitor);

    for (MethodDeclaration each : visitor.getMethodDeclarations()){

      final InvokesVisitor invokesVisitor = new InvokesVisitor();
      each.accept(invokesVisitor);

      for(MethodInvocation invocation : invokesVisitor.invocationSet()){

        final IMethodBinding binding = invocation.resolveMethodBinding();
        assertNotNull(binding);

        final ITypeBinding returnBinding = binding.getReturnType();
        assertNotNull(binding);

        final ITypeBinding currentReturnBinding = CommonJdt.normalizeTypeBinding(returnBinding);
        assertEquals(returnBinding, currentReturnBinding);

      }

    }

  }

  @Test public void testNodeRecoveryFromBinding() {
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
      final ASTNode actualNode = CommonJdt.findASTNodeDeclaration(each, context.getCompilationUnit()).orElse(null);
      assertNotNull(actualNode);
    }
  }


  static class InvokesVisitor extends SkeletalVisitor {
    final Set<MethodInvocation> invocationSet = new HashSet<>();

    @Override public boolean visit(MethodInvocation invoke) {

      invocationSet.add(invoke);

      return super.visitNode(invoke);
    }

    Set<MethodInvocation> invocationSet(){
      return invocationSet;
    }
  }
}
