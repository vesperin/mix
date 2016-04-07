package com.vesperin.base.locators;

import com.vesperin.base.Context;
import com.vesperin.base.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.List;
import java.util.Objects;

/**
 * This represents a parameter of a class's method.
 *
 * @author Huascar Sanchez
 */
public class ParameterUnit extends AbstractProgramUnit {
  /**
   * Construct a new {@code Parameter} program unit.
   *
   * @param name The parameter's name
   */
  public ParameterUnit(String name) {
    super(name);
  }

  @Override public List<UnitLocation> getLocations(Context context) {
    Objects.requireNonNull(context);

    return findLocationsByIdentifier(context);
  }

  @Override protected void addDeclaration(List<UnitLocation> namedLocations, Location each, ASTNode eachNode) {
    final SingleVariableDeclaration parameter = parent(SingleVariableDeclaration.class, eachNode);

    if (parameter != null) {
      if (!contains(namedLocations, parameter)) {
        namedLocations.add(new ProgramUnitLocation(parameter, each));
      }
    }
  }
}
