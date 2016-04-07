package com.vesperin.base.locators;

import com.google.common.base.Preconditions;
import com.vesperin.base.Context;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.visitors.TypeDeclarationVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This element represents classes in the base Source.
 *
 * @author Huascar Sanchez
 */
public class ClassUnit extends AbstractProgramUnit {
  /**
   * Construct a new {@code Class} program unit.
   */
  public ClassUnit(){
    this("all");
  }

  /**
   * Construct a new {@code Class} program unit.
   *
   * @param name The class's name
   */
  public ClassUnit(String name) {
    super(name);
  }

  @Override public List<UnitLocation> getLocations(Context context) {

    Preconditions.checkNotNull(context);

    return (!getIdentifier().equals("all")) ? findLocationsByIdentifier(context) : findAll(context);
  }

  private static List<UnitLocation> findAll(Context context){

    final TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
    context.accept(visitor);

    final Set<TypeDeclaration> types = visitor.getTypes();

    return types.stream()
      .map(each -> new ProgramUnitLocation(each, Locations.locate(each)))
      .collect(Collectors.toList());
  }

  @Override protected void addDeclaration(List<UnitLocation> locations, Location each, ASTNode eachNode) {
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
