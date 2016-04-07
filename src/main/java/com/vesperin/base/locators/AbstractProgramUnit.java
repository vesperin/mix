package com.vesperin.base.locators;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.vesperin.base.Context;
import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.StatementsSelectionVisitor;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
abstract class AbstractProgramUnit implements ProgramUnit {
  private final String name;

  /**
   * Construct a new {@code AbstractProgramUnit}.
   *
   * @param identifier the identifier of the program unit.
   */
  AbstractProgramUnit(String identifier){
    Preconditions.checkArgument(!Strings.isNullOrEmpty(identifier), "Invalid identifier");
    this.name = identifier;
  }

  @Override public String getIdentifier() {
    return name;
  }


  protected static <T extends ASTNode> boolean contains(List<UnitLocation> nodes, T node){
    for(Location each : nodes){
      final UnitLocation pul = (UnitLocation)each;
      if(pul.getUnitNode().equals(node) || pul.getUnitNode() == node ) return true;
    }

    return false;
  }

  protected static <T extends ASTNode> T parent(Class<T> klass, ASTNode node){
    return Jdt.parent(klass, node);
  }


  protected List<UnitLocation> findLocationsByIdentifier(Context parsedContext){
    Preconditions.checkNotNull(parsedContext);

    final List<UnitLocation> locations = new ArrayList<>();
    final List<Location> instances = Locations.locateWord(parsedContext.getSource(), getIdentifier());

    for(Location each : instances){

      addLocations(parsedContext, locations, each);
    }


    return locations;
  }

  protected void addLocations(Context parsedContext, List<UnitLocation> locations, Location each) {
    final StatementsSelectionVisitor statements = new StatementsSelectionVisitor(
      each,
      true
    );

    parsedContext.accept(statements);
    statements.checkIfSelectionCoversValidStatements();


    if(statements.isSelectionCoveringValidStatements()){
      // Note: once formatted, it is hard to locate a method. This mean that statements
      // getSelectedNodes is empty, and the only non null node is the statements.lastCoveringNode,
      // which can be A BLOCK if method is the selection. Therefore, I should get the parent of
      // this block to get the method or class to remove.

      for(ASTNode eachNode : statements.getSelectedNodes()){
        // ignore instance creation, parameter passing,... just give me its declaration
        addDeclaration(locations, each, eachNode);
      }
    }
  }


  protected abstract void addDeclaration(List<UnitLocation> namedLocations, Location each, ASTNode eachNode);

  @Override public String toString() {
    return "ProgramUnit(" + getIdentifier() + ")";
  }
}
