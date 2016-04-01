package com.vesperin.common.locators;

import com.google.common.base.Preconditions;
import com.vesperin.common.Context;
import com.vesperin.common.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;

import java.util.List;

/**
 * This represents a field of a class.
 *
 * @author Huascar Sanchez
 */
public class FieldUnit extends AbstractProgramUnit {
  /**
   * Construct a new {@code Field} program unit.
   *
   * @param name The field's name
   */
  public FieldUnit(String name) {
    super(name);
  }

  @Override public List<Location> getLocations(Context context) {
    Preconditions.checkNotNull(context);

    return findLocations(context);
  }

  @Override protected void addDeclaration(List<Location> locations, Location each, ASTNode eachNode) {
    final FieldDeclaration field = parent(FieldDeclaration.class, eachNode);

    if (field != null) {
      if (!contains(locations, field)) {
        locations.add(new ProgramUnitLocation(field, each));
      }
    }
  }
}
