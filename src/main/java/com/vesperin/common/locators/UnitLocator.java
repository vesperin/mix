package com.vesperin.common.locators;

import com.vesperin.common.locations.Location;

import java.util.List;

/**
 * @author Huascar Sanchez
 */
public interface UnitLocator {
  /**
   *
   * Locates program unit declarations.
   *
   * @param unit The program unit to be located, e.g., class, method, ....
   * @return the list of locations containing the declaration of this unit.
   */
  List<Location> locate(ProgramUnit unit);
}
