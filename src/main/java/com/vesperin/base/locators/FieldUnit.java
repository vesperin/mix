package com.vesperin.base.locators;

import com.vesperin.base.Context;
import com.vesperin.base.locations.Location;
import com.vesperin.base.visitors.FieldDeclarationVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This represents a field of a class.
 *
 * @author Huascar Sanchez
 */
public class FieldUnit extends AbstractProgramUnit {
  /**
   * Construct a new {@code Field} program unit.
   */
  public FieldUnit(){
    this("all");
  }

  /**
   * Construct a new {@code Field} program unit.
   *
   * @param name The field's name
   */
  public FieldUnit(String name) {
    super(name);
  }

  @Override public List<UnitLocation> getLocations(Context context) {
    Objects.requireNonNull(context);

    return (!getIdentifier().equals("all")) ? findLocationsByIdentifier(context) : findAll(context);
  }

  private static List<UnitLocation> findAll(Context context){
    final FieldDeclarationVisitor visitor = new FieldDeclarationVisitor();
    context.accept(visitor);

    return visitor.getData().keySet().stream()
      .map(each -> new ProgramUnitLocation(visitor.getData().get(each), each))
      .collect(Collectors.toList());
  }

  @Override protected void addDeclaration(List<UnitLocation> locations, Location each, ASTNode eachNode) {
    final FieldDeclaration field = parent(FieldDeclaration.class, eachNode);

    if (field != null) {
      if (!contains(locations, field)) {
        locations.add(new ProgramUnitLocation(field, each));
      }
    }
  }
}
