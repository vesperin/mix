package com.vesperin.common.visitors;

import com.vesperin.common.locations.Location;
import com.vesperin.common.locations.Locations;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class MethodDeclarations extends ASTVisitor {
  final Set<String> targets;
  final List<Location> locations = new ArrayList<>();

  public MethodDeclarations(Set<String> targets) {
    this.targets = targets;
  }

  public List<Location> getLocations(){
    return this.locations;
  }

  @Override public boolean visit(MethodDeclaration node) {
    if (targets.contains(node.getName().getIdentifier()) || targets.isEmpty()) {
      locations.add(Locations.locate(node));
    }
    return super.visit(node);
  }
}
