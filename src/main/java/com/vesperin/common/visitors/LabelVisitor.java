package com.vesperin.common.visitors;

import com.vesperin.common.utils.Jdt;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class LabelVisitor extends ASTVisitor {
  private final SimpleName label;
  private final List<SimpleName> result;

  private ASTNode definingLabel;

  public LabelVisitor(SimpleName label) {
    this.label          = label;
    this.result         = new ArrayList<>();
    this.definingLabel  = null;
  }

  @Override public boolean visit(BreakStatement node) {
    final SimpleName label = node.getLabel();

    if (definingLabel != null && isSameLabel(label) && Jdt.isParent(label, definingLabel)) {
      result.add(label);
    }

    return false;
  }

  @Override public boolean visit(ContinueStatement node) {
    final SimpleName label = node.getLabel();

    if (definingLabel != null && isSameLabel(label) && Jdt.isParent(label, definingLabel)) {
      result.add(label);
    }

    return false;
  }

  @Override public boolean visit(LabeledStatement node) {
    if (definingLabel == null) {
      final SimpleName label = node.getLabel();
      if (this.label == label || isSameLabel(label) && Jdt.isParent(label, node)) {
        definingLabel = node;
        result.add(label);
      }
    }

    node.getBody().accept(this);

    return false;
  }


  public List<SimpleName> getLabels(){
    return result;
  }


  private boolean isSameLabel(SimpleName label) {
    return label != null && label.getIdentifier().equals(label.getIdentifier());
  }

}