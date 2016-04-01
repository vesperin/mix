package com.vesperin.common.visitors;

import com.vesperin.common.utils.Jdt;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huascar.sanchez@sri.com (Huascar Sanchez)
 */
public class LinkedNodesVisitor extends ASTVisitor {
  private final IBinding binding;
  private final List<SimpleName> result;

  /**
   * Construct a visitor that find all nodes connected to the given binding.
   *
   * @param binding The linked binding.
   */
  public LinkedNodesVisitor(IBinding binding) {
    super(true);
    this.binding = Jdt.getDeclaration(binding);
    this.result = new ArrayList<>();
  }

  @Override public boolean visit(SimpleName node) {
    IBinding binding = node.resolveBinding();
    if (binding == null) {
      return false;
    }

    binding = Jdt.getDeclaration(binding);

    if (this.binding == binding) {
      result.add(node);
    } else if (binding.getKind() != this.binding.getKind()) {
      return false;
    } else if (binding.getKind() == IBinding.METHOD) {
      final IMethodBinding currentBinding = (IMethodBinding) binding;
      final IMethodBinding methodBinding = (IMethodBinding) this.binding;
      if (methodBinding.overrides(currentBinding) || currentBinding.overrides(methodBinding)) {
        result.add(node);
      }
    }
    return false;
  }

  public List<SimpleName> getLinkedNodes() {
    return result;
  }
}