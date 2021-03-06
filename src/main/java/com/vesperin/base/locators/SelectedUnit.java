package com.vesperin.base.locators;

import com.vesperin.base.Context;
import com.vesperin.base.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
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

  @Override public List<UnitLocation> getLocations(Context context) {
    ensureIsWildCard();

    final List<UnitLocation> locations = new ArrayList<>();

    addLocations(context, locations, selection);

    return locations;
  }

  private void ensureIsWildCard() {
    if (!WILD_CARD.equals(getIdentifier())) {
      throw new RuntimeException("Not a wildcard unit");
    }
  }

  @Override protected void addDeclaration(List<UnitLocation> locations, Location each, ASTNode eachNode) {
    locations.add(new ProgramUnitLocation(eachNode, each));
  }
}
