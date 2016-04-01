package com.vesperin.common.locators;

import com.google.common.base.Preconditions;
import com.vesperin.common.Context;
import com.vesperin.common.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.List;

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

  @Override public List<Location> getLocations(Context context) {
    Preconditions.checkNotNull(context);

    return findLocations(context);
  }

  @Override protected void addDeclaration(List<Location> namedLocations, Location each, ASTNode eachNode) {
    final VariableDeclarationStatement localVar = parent(VariableDeclarationStatement.class, eachNode);

    if (localVar != null) {
      if (!contains(namedLocations, localVar)) {
        namedLocations.add(new ProgramUnitLocation(localVar, each));
      }
    }
  }
}
