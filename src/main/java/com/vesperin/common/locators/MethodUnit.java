package com.vesperin.common.locators;

import com.google.common.base.Preconditions;
import com.vesperin.common.Context;
import com.vesperin.common.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.List;

/**
 * This represents a method of a class.
 *
 * @author Huascar Sanchez
 */
public class MethodUnit extends AbstractProgramUnit {
  /**
   * Construct a new {@code Method} program unit.
   *
   * @param name The method's name
   */
  public MethodUnit(String name) {
    super(name);
  }

  @Override public List<Location> getLocations(Context context) {
    Preconditions.checkNotNull(context);

    return findLocations(context);
  }

  @Override protected void addDeclaration(List<Location> locations, Location each, ASTNode eachNode) {
    final MethodDeclaration methodDeclaration = parent(MethodDeclaration.class, eachNode);

    if (methodDeclaration != null) {
      if (!contains(locations, methodDeclaration) &&
        getName().equals(methodDeclaration.getName().getIdentifier())) {

        locations.add(new ProgramUnitLocation(methodDeclaration, each));
      }
    }
  }
}
