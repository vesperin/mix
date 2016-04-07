package com.vesperin.base.locators;

import com.google.common.base.Preconditions;
import com.vesperin.base.Context;
import com.vesperin.base.locations.Location;
import com.vesperin.base.visitors.MethodDeclarationVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This represents a method of a class.
 *
 * @author Huascar Sanchez
 */
public class MethodUnit extends AbstractProgramUnit {
  /**
   * Construct a new {@code Method} program unit.
   */
  public MethodUnit(){
    this("all");
  }

  /**
   * Construct a new {@code Method} program unit.
   *
   * @param identifier The method's identifier or "all" for all methods in a context.
   */
  public MethodUnit(String identifier) {
    super(identifier);
  }

  @Override public List<UnitLocation> getLocations(Context context) {
    Preconditions.checkNotNull(context);

    return (!getIdentifier().equals("all")) ? findLocationsByIdentifier(context) : findAll(context);
  }

  private static List<UnitLocation> findAll(Context context){
    final MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
    context.accept(visitor);

    return visitor.getData().keySet().stream()
      .map(each -> new ProgramUnitLocation(visitor.getData().get(each), each))
      .collect(Collectors.toList());
  }

  @Override protected void addDeclaration(List<UnitLocation> locations, Location each, ASTNode eachNode) {
    final MethodDeclaration methodDeclaration = parent(MethodDeclaration.class, eachNode);

    if (methodDeclaration != null) {
      if (!contains(locations, methodDeclaration) &&
        getIdentifier().equals(methodDeclaration.getName().getIdentifier())) {

        locations.add(new ProgramUnitLocation(methodDeclaration, each));
      }
    }
  }
}
