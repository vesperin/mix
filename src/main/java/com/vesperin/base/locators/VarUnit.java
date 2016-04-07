package com.vesperin.base.locators;

import com.vesperin.base.Context;
import com.vesperin.base.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.List;
import java.util.Objects;

/**
 * This represents a local variable of a class.
 *
 * @author Huascar Sanchez
 */
public class VarUnit extends AbstractProgramUnit {
  /**
   * Construct a new {@code Local Variable} program unit.
   *
   * @param name The field's name
   */
  public VarUnit(String name) {
    super(name);
  }

  @Override public List<UnitLocation> getLocations(Context context) {
    Objects.requireNonNull(context);

    return findLocationsByIdentifier(context);
  }

  @Override protected void addDeclaration(List<UnitLocation> namedLocations, Location each, ASTNode eachNode) {
    final VariableDeclarationStatement localVar = parent(VariableDeclarationStatement.class, eachNode);

    if (localVar != null) {
      if (!contains(namedLocations, localVar)) {
        namedLocations.add(new ProgramUnitLocation(localVar, each));
      }
    }
  }
}
