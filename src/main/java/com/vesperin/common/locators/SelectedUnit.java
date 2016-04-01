package com.vesperin.common.locators;

import com.google.common.collect.Lists;
import com.vesperin.common.Context;
import com.vesperin.common.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class SelectedUnit extends AbstractProgramUnit {
  private static final String WILD_CARD = "*";

  private final Location selection;

  /**
   * Construct a new {@code InferredUnit} program unit with a Java {@code Context}
   * and a {@code Location}.
   */
  public SelectedUnit(Location selection) {
    super(WILD_CARD);
    this.selection = selection;
  }

  @Override public List<Location> getLocations(Context context) {
    ensureIsWildCard();

    final List<Location> locations = Lists.newArrayList();

    addLocations(context, locations, selection);

    return locations;
  }

  private void ensureIsWildCard() {
    if (!WILD_CARD.equals(getName())) {
      throw new RuntimeException("Not a wildcard unit");
    }
  }

  @Override protected void addDeclaration(List<Location> locations, Location each, ASTNode eachNode) {
    locations.add(new ProgramUnitLocation(eachNode, each));
  }
}
