package com.vesperin.utils;

import com.vesperin.base.*;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.visitors.MethodDeclarationVisitor;
import com.vesperin.base.visitors.SkeletalVisitor;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

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


  @Test public void testTypeNormalization() throws Exception {
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

      final ITypeBinding currentType = Jdt.normalizeTypeBinding(actualType);

      assertNotEquals(actualType, currentType);
    }

  }

  @Test public void testTypeNormalization2() throws Exception {
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

        final ITypeBinding currentReturnBinding = Jdt.normalizeTypeBinding(returnBinding);
        assertEquals(returnBinding, currentReturnBinding);

      }

    }

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
