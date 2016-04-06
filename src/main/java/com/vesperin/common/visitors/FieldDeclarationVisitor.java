package com.vesperin.common.visitors;

import com.vesperin.common.locations.Location;
import com.vesperin.common.locations.Locations;
import org.eclipse.jdt.core.dom.FieldDeclaration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class FieldDeclarationVisitor extends SkeletalVisitor {
  final Map<Location, FieldDeclaration> data;

  public FieldDeclarationVisitor() {
    this.data         = new HashMap<>();
  }

  public List<Location> getLocations(){
    return getData().keySet().stream()
      .collect(Collectors.toList());
  }

  public List<FieldDeclaration> getFieldDeclations(){
    return getData().values().stream()
      .collect(Collectors.toList());
  }

  public Map<Location, FieldDeclaration> getData(){
    return this.data;
  }

  @Override public boolean visit(FieldDeclaration node) {
    data.put(Locations.locate(node), node);
    return super.visit(node);
  }
}
