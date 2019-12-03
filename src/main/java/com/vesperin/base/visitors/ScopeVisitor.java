package com.vesperin.base.visitors;

import com.vesperin.base.BindingRequest;
import com.vesperin.base.Scope;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * @author Huascar Sanchez
 */
public class ScopeVisitor extends ASTVisitorWithHierarchicalWalk {
  private Location scope;
  private int      flags;
  private boolean  breakStatement;

  private final BindingRequest request;

  /**
   * Constructs a ScopeVisitor given a scope, flags, a binding request, and
   * a request to visit JavaDoc tags as parameters.
   */
  public ScopeVisitor(Location scope, int flags, BindingRequest request) {
    this.scope      = scope;
    this.flags      = flags;
    this.request    = request;
    breakStatement  = false;
  }

  public boolean isBreakStatement(){
    return breakStatement;
  }


  private static boolean isInsideScope(ASTNode node, Location scope) {

    final Location location = Locations.locate(node);

    return Locations.inside(location, scope);
  }

  @Override public boolean visit(MethodDeclaration node) {
    if (isInsideScope(node, scope)) {
      final Block body  = node.getBody();
      if (body != null) {
        body.accept(this);
      }

      visitBackwards(node.parameters());
      visitBackwards(node.typeParameters());
    }

    return false;
  }


  @Override public boolean visit(TypeParameter node) {
    if (Scope.isTypesFlagAvailable(flags)
        && node.getStartPosition() < scope.getStart().getOffset()) {

      breakStatement = request.accept(
          node.getName().resolveBinding()
      );
    }

    return !breakStatement;
  }

  @Override public boolean visit(SwitchCase node) {
    // switch on enum allows to use enum constants without qualification
    if (Scope.isVariablesFlagAvailable(flags)
        && !node.isDefault() && isInsideScope(node.getExpression(), scope)) {

      final ASTNode         nonNullParent   = Objects.requireNonNull(node.getParent());
      final SwitchStatement switchStatement = (SwitchStatement) nonNullParent;
      final ITypeBinding    binding         = switchStatement.getExpression().resolveTypeBinding();
      if (binding != null && binding.isEnum()) {
        IVariableBinding[] declaredFields= binding.getDeclaredFields();
        for (IVariableBinding variableBinding : declaredFields) {
          if (variableBinding.isEnumConstant()) {
            breakStatement = request.accept(variableBinding);
            if (breakStatement)
              return false;
          }
        }
      }
    }

    return false;
  }


  public boolean visit(Initializer node) {
    return !breakStatement && isInsideScope(node, scope);
  }

  public boolean visit(Statement node) {
    return !breakStatement && isInsideScope(node, scope);
  }

  @SuppressWarnings("UnusedParameters")
  public boolean visit(ASTNode node) {
    return false;
  }

  public boolean visit(Block node) {
    if (isInsideScope(node, scope)) {
      visitBackwards(node.statements());
    }
    return false;
  }

  public boolean visit(VariableDeclaration node) {
    if (Scope.isVariablesFlagAvailable(flags)
        && node.getStartPosition() < scope.getStart().getOffset()) {

      breakStatement = request.accept(node.resolveBinding());
    }

    return !breakStatement;
  }

  public boolean visit(VariableDeclarationStatement node) {
    visitBackwards(node.fragments());
    return false;
  }

  public boolean visit(VariableDeclarationExpression node) {
    visitBackwards(node.fragments());
    return false;
  }

  public boolean visit(CatchClause node) {
    if (isInsideScope(node, scope)) {
      node.getBody().accept(this);
      node.getException().accept(this);
    }
    return false;
  }

  public boolean visit(ForStatement node) {
    if (isInsideScope(node, scope)) {
      node.getBody().accept(this);
      visitBackwards(node.initializers());
    }
    return false;
  }

  public boolean visit(TypeDeclarationStatement node) {
    if (Scope.isTypesFlagAvailable(flags)
        && node.getStartPosition() + node.getLength() < scope.getStart().getOffset()) {

      breakStatement = request.accept(node.resolveBinding());

      return false;
    }

    return !breakStatement && isInsideScope(node, scope);
  }

  private void visitBackwards(List<?> list) {
    if (breakStatement) return;

    for (int i= list.size() - 1; i >= 0; i--) {
      final ASTNode astNode = (ASTNode) list.get(i);
      if (astNode.getStartPosition() < scope.getStart().getOffset()) {
        astNode.accept(this);
      }
    }
  }
}
