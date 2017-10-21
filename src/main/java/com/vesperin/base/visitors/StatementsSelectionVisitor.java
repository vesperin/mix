package com.vesperin.base.visitors;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.Jdt;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class StatementsSelectionVisitor extends ASTNodesSelectionVisitor {
  private boolean invalidSelection;

  /**
   * Instantiates a new {@code StatementsSelectionVisitor} with a given code selection.
   * Selected node won't be traversed.
   *
   * @param selection scope
   */
  public StatementsSelectionVisitor(Location selection){
    this(selection, false);
  }

  /**
   * Instantiates a new {@code StatementsSelectionVisitor} with a given code selection and
   * a traversal flag as values.
   *
   * @param selection The code selected by user, in a location form.
   * @param traverseSelectedNode {@code true} if the selected node should be traversed,
   *    {@code false} otherwise.
   */
  public StatementsSelectionVisitor(Location selection, boolean traverseSelectedNode){
    super(selection, traverseSelectedNode);
    this.invalidSelection = false;
  }

  @Override public boolean checkIfSelectionCoversValidStatements(){
    final List<ASTNode> nodes = getSelectedNodes();

    if(nodes.isEmpty()) return false;

    final Location firstNode  = Locations.locate(getFirstSelectedNode());
    final Location secondNode = Locations.locate(getLastSelectedNode());
    final Location selection  = getSelection();


    final boolean isIntersecting = (Locations.intersects(firstNode, selection)
      || Locations.intersects(secondNode, selection));

    final boolean isNotInside    = !(selection.begins(firstNode.getStart())
      && selection.ends(secondNode.getEnd()));


    invalidSelection(isIntersecting && isNotInside);

    if(isSelectionCoveringValidStatements()){
      if (firstNode instanceof ArrayInitializer) {
        invalidSelection(true);
      }
    }

    return isSelectionCoveringValidStatements();
  }


  @Override public boolean isSelectionCoveringValidStatements(){
    return !this.invalidSelection;
  }


  private void invalidSelection(boolean value){
    this.invalidSelection = value;
  }


  @Override public void endVisit(CompilationUnit node) {
    if(!hasSelectedNodes()){
      super.endVisit(node);
      return;
    }

    if(isSelectionCoveringValidStatements()){
      checkIfSelectionCoversValidStatements();
    }

    super.endVisit(node);
  }


  @Override public void endVisit(DoStatement node) {
    if (doAfterValidation(node, getSelectedNodes())) {
      if (contains(getSelectedNodes(), node.getBody())
        && contains(getSelectedNodes(), node.getExpression())) {

        invalidSelection(true);

      }
    }

    super.endVisit(node);
  }

  @Override public void endVisit(ForStatement node) {
    if (doAfterValidation(node, getSelectedNodes())) {
      boolean containsExpression= contains(getSelectedNodes(), node.getExpression());
      boolean containsUpdaters= contains(getSelectedNodes(), node.updaters());

      if (contains(getSelectedNodes(), node.initializers()) && containsExpression) {
        invalidSelection(true);
      } else if (containsExpression && containsUpdaters) {
        invalidSelection(true);
      } else if (containsUpdaters && contains(getSelectedNodes(), node.getBody())) {
        invalidSelection(true);
      }

    }
    super.endVisit(node);
  }


  @Override public void endVisit(SwitchStatement node) {
    if (doAfterValidation(node, getSelectedNodes())) {
      List<ASTNode> cases = Jdt.getSwitchCases(node);
      for(ASTNode eachSelected : getSelectedNodes()){
        if(cases.contains(eachSelected)){
          invalidSelection(true);
          break;
        }
      }
    }
    super.endVisit(node);
  }


  @Override public void endVisit(SynchronizedStatement node) {
    final ASTNode  firstNode    = getFirstSelectedNode();
    final Location nodeLocation = Locations.locate(firstNode);

    if(Locations.covers(getSelection(), nodeLocation)){
      if(firstNode == node.getBody()){
        invalidSelection(true);
      }
    }

    super.endVisit(node);

  }


  @Override public void endVisit(TryStatement node) {
    final ASTNode  firstNode    = getFirstSelectedNode();
    if(Objects.isNull(firstNode)) {
      super.endVisit(node);
      return;
    }
    final Location nodeLocation = Locations.locate(firstNode);

    if(Locations.isAfterBaseLocation(getSelection(), nodeLocation)){
      if(firstNode == node.getBody() || firstNode == node.getFinally()){
        invalidSelection(true);
      } else {
        final List catchClauses = node.catchClauses();
        for(Object eachCatch : catchClauses){
          final CatchClause element = (CatchClause) eachCatch;
          if (element == firstNode || element.getBody() == firstNode) {
            invalidSelection(true);
          } else if(element.getException() == firstNode){
            invalidSelection(true);
          }
        }
      }
    }

    super.endVisit(node);
  }


  @Override public void endVisit(WhileStatement node) {
    if(doAfterValidation(node, getSelectedNodes())){
      if (contains(getSelectedNodes(), node.getExpression())
        && contains(getSelectedNodes(), node.getBody())) {

        invalidSelection(true);
      }
    }

    super.endVisit(node);
  }


  private boolean doAfterValidation(ASTNode node, List<ASTNode> selectedNodes) {
    final Location nodeLocation = Locations.locate(node);
    final Location base         = getSelection();
    return (selectedNodes.size() > 0
      && node == selectedNodes.get(0).getParent()
      && Locations.isAfterBaseLocation(base, nodeLocation));
  }



  protected static boolean contains(List<ASTNode> nodes, ASTNode node) {
    for (ASTNode each : nodes) {
      if (each == node) return true;
    }

    return false;
  }

  protected static boolean contains(List<ASTNode> nodes, List list) {
    for (ASTNode node : nodes) {
      if (list.contains(node)) return true;
    }

    return false;
  }
}
