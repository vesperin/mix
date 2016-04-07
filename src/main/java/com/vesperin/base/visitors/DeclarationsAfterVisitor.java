package com.vesperin.base.visitors;

import com.vesperin.base.Scopes;
import com.vesperin.base.spi.BindingRequest;
import org.eclipse.jdt.core.dom.*;

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

    return stopTheWalk;
  }

  @Override public boolean visit(MethodInvocation node) {
    if(Scopes.isMethodsFlagAvailable(flags) && position < node.getStartPosition()){
      stopTheWalk = request.accept(node.resolveMethodBinding());
    }

    return stopTheWalk;
  }

  @Override public boolean visit(VariableDeclarationFragment node) {
    if(Scopes.isTypesFlagAvailable(flags) && position < node.getStartPosition()){
      if(node.getInitializer() instanceof QualifiedName){
        final QualifiedName name = (QualifiedName) node.getInitializer();
        // If we encounter a name where its first char is an uppercase letter
        // then we can assume the name is the name of a class.
        if(Character.isUpperCase(name.getName().getIdentifier().charAt(0))){
          stopTheWalk = request.accept(name.getQualifier().resolveBinding());
        }
      }
    }



    return visit((VariableDeclaration) node);
  }

  @Override public boolean visit(AnonymousClassDeclaration node) {
    return false;
  }

  @Override public boolean visit(TypeDeclarationStatement node) {
    if(Scopes.isTypesFlagAvailable(flags) && position < node.getStartPosition()){
      stopTheWalk = request.accept(node.resolveBinding());
    }

    return stopTheWalk;
  }

}
