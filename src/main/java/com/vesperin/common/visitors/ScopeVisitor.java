package com.vesperin.common.visitors;

import com.vesperin.common.Scopes;
import com.vesperin.common.locations.Location;
import com.vesperin.common.locations.Locations;
import com.vesperin.common.spi.BindingRequest;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Objects;

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
    if (Scopes.isTypesFlagAvailable(flags)
        && node.getStartPosition() < scope.getStart().getOffset()) {

      breakStatement = request.accept(
          node.getName().resolveBinding()
      );
    }

    return !breakStatement;
  }

  @Override public boolean visit(SwitchCase node) {
    // switch on enum allows to use enum constants without qualification
    if (Scopes.isVariablesFlagAvailable(flags)
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
    if (Scopes.isVariablesFlagAvailable(flags)
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
    if (Scopes.isTypesFlagAvailable(flags)
        && node.getStartPosition() + node.getLength() < scope.getStart().getOffset()) {

      breakStatement = request.accept(node.resolveBinding());

      return false;
    }

    return !breakStatement && isInsideScope(node, scope);
  }

  private void visitBackwards(List list) {
    if (breakStatement) return;

    for (int i= list.size() - 1; i >= 0; i--) {
      final ASTNode astNode = (ASTNode) list.get(i);
      if (astNode.getStartPosition() < scope.getStart().getOffset()) {
        astNode.accept(this);
      }
    }
  }
}
