package com.vesperin.common.visitors;

import com.vesperin.common.locations.Location;
import com.vesperin.common.locations.Locations;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class MethodDeclarationVisitor extends SkeletalVisitor {
  final Set<String> targets;
  final Map<Location, MethodDeclaration> data;


  public MethodDeclarationVisitor(){
    this(new HashSet<>());
  }

  public MethodDeclarationVisitor(Set<String> targets) {
    this.targets      = targets;
    this.data         = new HashMap<>();
  }

  public List<Location> getLocations(){
    return getData().keySet().stream()
      .collect(Collectors.toList());
  }

  public List<MethodDeclaration> getMethodDeclarations(){
    return getData().values().stream()
      .collect(Collectors.toList());
  }

  public Map<Location, MethodDeclaration> getData(){
    return this.data;
  }

  @Override public boolean visit(MethodDeclaration node) {
    if (targets.contains(node.getName().getIdentifier()) || targets.isEmpty()) {

      data.put(Locations.locate(node), node);
    }

    return super.visit(node);
  }
}
