package com.vesperin.common.locators;

import com.google.common.base.Preconditions;
import com.vesperin.common.Context;
import com.vesperin.common.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;

/**
 * This element represents classes in the base Source.
 *
 * @author Huascar Sanchez
 */
public class ClassUnit extends AbstractProgramUnit {

  /**
   * Construct a new {@code Class} program unit.
   *
   * @param name The class's name
   */
  public ClassUnit(String name) {
    super(name);
  }

  @Override public List<Location> getLocations(Context context) {

    Preconditions.checkNotNull(context);

    return findLocationsByIdentifier(context);
  }

  @Override protected void addDeclaration(List<Location> locations, Location each, ASTNode eachNode) {
    final TypeDeclaration classDeclaration = parent(
      TypeDeclaration.class,
      eachNode
    );

    if (classDeclaration != null) {
      if (!contains(locations, classDeclaration) &&
        getIdentifier().equals(classDeclaration.getName().getIdentifier())) {

        locations.add(new ProgramUnitLocation(classDeclaration, each));
      }
    }
  }
}
