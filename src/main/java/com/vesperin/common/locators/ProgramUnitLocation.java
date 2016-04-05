package com.vesperin.common.locators;

import com.google.common.base.Preconditions;
import com.vesperin.common.Source;
import com.vesperin.common.locations.Location;
import com.vesperin.common.locations.Position;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * @author Huascar Sanchez
 */
public class ProgramUnitLocation implements UnitLocation {
  private static final int METHOD_DECLARATION = ASTNode.METHOD_DECLARATION;
  private static final int TYPE_DECLARATION = ASTNode.TYPE_DECLARATION;
  private static final int VARIABLE_DECLARATION = ASTNode.SINGLE_VARIABLE_DECLARATION;
  private static final int FIELD_DECLARATION = ASTNode.FIELD_DECLARATION;
  private static final int VARIABLE_DECLARATION_STATEMENT = ASTNode.VARIABLE_DECLARATION_STATEMENT;
  private static final int SIMPLE_NAME = ASTNode.SIMPLE_NAME;
  private final ASTNode node;
  private final Location location;


  /**
   * Construct a new ProgramUnitLocation
   *
   * @param node     the located ASTNode
   * @param location the location of this ASTNode.
   */
  public ProgramUnitLocation(ASTNode node, Location location) {
    this.node = Preconditions.checkNotNull(node);
    this.location = Preconditions.checkNotNull(location);
  }

  private static String nameUnit(ASTNode node) {
    switch (node.getNodeType()) {
      case METHOD_DECLARATION: {
        final MethodDeclaration method = (MethodDeclaration) node;
        return "Method(" + method.getName().getIdentifier() + ")";
      }

      case TYPE_DECLARATION: {
        final TypeDeclaration clazz = (TypeDeclaration) node;
        return "Type(" + clazz.getName().getIdentifier() + ")";
      }

      case VARIABLE_DECLARATION: {
        final SingleVariableDeclaration param = (SingleVariableDeclaration) node;
        final MethodDeclaration method = (MethodDeclaration) param.getParent();
        return "Method(" + method.getName().getIdentifier() + ")";
      }

      case FIELD_DECLARATION: {
        final FieldDeclaration field = (FieldDeclaration) node;
        final TypeDeclaration clazz = (TypeDeclaration) field.getParent();
        return "Type(" + clazz.getName().getIdentifier() + ")";
      }

      case VARIABLE_DECLARATION_STATEMENT: {
        final VariableDeclarationStatement statement = (VariableDeclarationStatement) node;
        final MethodDeclaration method = (MethodDeclaration) statement.getParent();
        return "Method(" + method.getName().getIdentifier() + ")";
      }

      case SIMPLE_NAME: {

        if(node.getParent() != null){
          if(TYPE_DECLARATION == node.getParent().getNodeType()){
            final TypeDeclaration clazz = (TypeDeclaration) node.getParent();
            return "Type(" + clazz.getName().getIdentifier() + ")";
          } else if (METHOD_DECLARATION == node.getParent().getNodeType()){
            final MethodDeclaration method = (MethodDeclaration) node.getParent();
            return "Method(" + method.getName().getIdentifier() + ")";
          }
        }

      }


      default:
        return "ProgramUnitLocation(unknown)";
    }
  }

  @Override public ASTNode getUnitNode() {
    return node;
  }

  @Override public int compareTo(Location o) {
    return location.compareTo(o);
  }

  @Override public Source getSource() {
    return location.getSource();
  }

  @Override public Position getStart() {
    return location.getStart();
  }

  @Override public Position getEnd() {
    return location.getEnd();
  }

  @Override public String toString() {
    final ASTNode node = getUnitNode();
    return nameUnit(node);
  }
}
