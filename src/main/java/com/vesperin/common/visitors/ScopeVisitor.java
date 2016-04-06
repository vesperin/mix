package com.vesperin.common.visitors;

import com.vesperin.common.Scopes;
import com.vesperin.common.spi.BindingRequest;
import com.vesperin.common.locations.Location;
import com.vesperin.common.locations.Locations;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class ScopeVisitor extends ASTVisitorWithHierarchicalWalk {
  private int             position;
  private int             flags;
  private boolean         breakStatement;

  private final BindingRequest request;

  /**
   * Constructs a ScopeVisitor given a position, flags, a binding request, and
   * a request to visit JavaDoc tags as parameters.
   */
  public ScopeVisitor(int position, int flags, BindingRequest request) {
    this.position   = position;
    this.flags      = flags;
    this.request    = request;
    breakStatement  = false;
  }

  public boolean isBreakStatement(){
    return breakStatement;
  }


  private static boolean isInsideScope(ASTNode node, int position) {

    final Location location = Locations.locate(node);

    return Locations.insideScope(location, position);
  }

  @Override public boolean visit(MethodDeclaration node) {
    if (isInsideScope(node, position)) {
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
        && node.getStartPosition() < position) {

      breakStatement = request.accept(
          node.getName().resolveBinding()
      );
    }

    return !breakStatement;
  }

  @Override public boolean visit(SwitchCase node) {
    // switch on enum allows to use enum constants without qualification
    if (Scopes.isVariablesFlagAvailable(flags)
        && !node.isDefault() && isInsideScope(node.getExpression(), position)) {

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
    return !breakStatement && isInsideScope(node, position);
  }

  public boolean visit(Statement node) {
    return !breakStatement && isInsideScope(node, position);
  }

  @SuppressWarnings("UnusedParameters")
  public boolean visit(ASTNode node) {
    return false;
  }

  public boolean visit(Block node) {
    if (isInsideScope(node, position)) {
      visitBackwards(node.statements());
    }
    return false;
  }

  public boolean visit(VariableDeclaration node) {
    if (Scopes.isVariablesFlagAvailable(flags)
        && node.getStartPosition() < position) {

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
    if (isInsideScope(node, position)) {
      node.getBody().accept(this);
      node.getException().accept(this);
    }
    return false;
  }

  public boolean visit(ForStatement node) {
    if (isInsideScope(node, position)) {
      node.getBody().accept(this);
      visitBackwards(node.initializers());
    }
    return false;
  }

  public boolean visit(TypeDeclarationStatement node) {
    if (Scopes.isTypesFlagAvailable(flags)
        && node.getStartPosition() + node.getLength() < position) {

      breakStatement = request.accept(node.resolveBinding());

      return false;
    }

    return !breakStatement && isInsideScope(node, position);
  }

  private void visitBackwards(List list) {
    if (breakStatement) return;

    for (int i= list.size() - 1; i >= 0; i--) {
      final ASTNode astNode = (ASTNode) list.get(i);
      if (astNode.getStartPosition() < position) {
        astNode.accept(this);
      }
    }
  }
}
