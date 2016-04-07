package com.vesperin.base.visitors;

import com.vesperin.base.Source;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public abstract class ASTNodesSelectionVisitor extends SkeletalVisitor {
  private final Location      selectedArea;
  private final boolean       traverseSelectedNode;
  private final List<ASTNode> selectedNodes;

  private ASTNode lastCoveringNode;


  /**
   * Instantiates a new {@link ASTNodesSelectionVisitor} object.
   *
   * @param selectedArea The selected area.
   * @param traverseSelectedNode {@code true} if selected node
   *             should be traversed, {@code false} otherwise.
   */
  public ASTNodesSelectionVisitor(Location selectedArea, boolean traverseSelectedNode){
    super(true);
    this.selectedArea           = selectedArea;
    this.traverseSelectedNode   = traverseSelectedNode;
    this.selectedNodes          = new ArrayList<>();
  }


  public abstract boolean checkIfSelectionCoversValidStatements();


  /**
   * @return {@code true} if the selected area by user
   *      has mapped into covered AST nodes, {@code false} otherwise.
   */
  public boolean hasSelectedNodes() {
    return !selectedNodes.isEmpty();
  }

  /**
   * @return the list of AST nodes covered by
   *      selected area, empty list if none selected..
   */
  public List<ASTNode> getSelectedNodes() {
    return selectedNodes;
  }

  /**
   * @return The first selected ASTNode.
   */
  public ASTNode getFirstSelectedNode() {
    if (!hasSelectedNodes()) return null;
    return selectedNodes.get(0);
  }

  /**
   * @return The last selected ASTNode.
   */
  public ASTNode getLastSelectedNode() {
    if (!hasSelectedNodes()) return null;
    return selectedNodes.get(selectedNodes.size() - 1);
  }

  /**
   * @return {@code true} if the first selected node is an {@code Expression}.
   */
  public boolean isExpressionSelected() {
    return hasSelectedNodes()
      && getFirstSelectedNode() instanceof Expression;
  }

  /**
   * @return The Location mapping to a code selection.
   */
  protected Location getSelection() {
    return selectedArea;
  }

  /**
   * @return The last covering node by user selected area.
   */
  public ASTNode getLastCoveringNode() {
    return lastCoveringNode;
  }

  /**
   * @return The range, in location form, of all selected nodes.
   */
  public Location getSelectedNodeRange() {
    if (!hasSelectedNodes()) return null;


    final ASTNode   firstNode   = getFirstSelectedNode();
    final ASTNode   lastNode    = getLastSelectedNode();
    final int       start       = firstNode.getStartPosition();
    final Source code        = selectedArea.getSource();

    return Locations.createLocation(
      code, code.getContent(),
      start,
      lastNode.getStartPosition() + lastNode.getLength() - start
    );
  }

  public abstract boolean isSelectionCoveringValidStatements();


  @Override protected boolean visitNode(ASTNode node) {
    final Location nodeLocation = Locations.locate(node);

    if(Locations.outside(selectedArea, nodeLocation)) {
      return false;
    } else if(Locations.covers(selectedArea, nodeLocation)) {
      if (isFirstNode()) {
        handleFirstSelectedNode(node);
      } else {
        handleNextSelectedNode(node);
      }

      return traverseSelectedNode;
    } else if(Locations.covers(nodeLocation, selectedArea)){
      lastCoveringNode = node;
      return true;
    } else if(Locations.endsInside(selectedArea, nodeLocation)){
      return handleSelectionEndsIn(node);
    }

    return true;
  }

  /**
   * @return {@code true} if we have selected any node, and the one
   *      we are exploring is the first one to check.
   */
  private boolean isFirstNode() {
    return !hasSelectedNodes();
  }


  protected void handleFirstSelectedNode(ASTNode node) {
    selectedNodes.clear();
    selectedNodes.add(node);
  }


  protected void handleNextSelectedNode(ASTNode node) {
    if (getFirstSelectedNode().getParent() == node.getParent()) {
      selectedNodes.add(node);
    }
  }


  protected boolean handleSelectionEndsIn(ASTNode node) {
    Objects.requireNonNull(node);
    return false;
  }
}
