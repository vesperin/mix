package com.vesperin.common.visitors;

import com.vesperin.common.Scopes;
import com.vesperin.common.spi.BindingRequest;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/**
 * @author Huascar Sanchez
 */
public class DeclarationsAfterVisitor extends ASTVisitorWithHierarchicalWalk {
  private final int             position;
  private final int             flags;
  private final BindingRequest  request;
  private boolean stopTheWalk;

  /**
   * Creates a new DeclarationsAfterVisitor object.
   */
  public DeclarationsAfterVisitor(int position, int flags, BindingRequest request){
    this.position     = position;
    this.flags        = flags;
    this.request      = request;
    this.stopTheWalk  = false;
  }

  @Override public boolean visit(ASTNode node) {
    return !stopTheWalk;
  }

  @Override public boolean visit(VariableDeclaration node) {
    if(Scopes.isVariablesFlagAvailable(flags) && position < node.getStartPosition()){
      stopTheWalk = request.accept(node.resolveBinding());
    }

    return false;
  }

  @Override public boolean visit(AnonymousClassDeclaration node) {
    return false;
  }

  @Override public boolean visit(TypeDeclarationStatement node) {
    if(Scopes.isTypesFlagAvailable(flags) && position < node.getStartPosition()){
      stopTheWalk = request.accept(node.resolveBinding());
    }

    return false;
  }

}
