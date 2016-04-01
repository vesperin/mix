package com.vesperin.common.locators;

import com.vesperin.common.locations.Location;

import java.util.List;

/**
 * @author Huascar Sanchez
 */
public interface UnitLocator {
  /**
   *
   * Locates a program unit.
   *
   * @param unit The program unit to be located, e.g., class, method, ....
   * @return the list of locations containing this unit.
   */
  List<Location> locate(ProgramUnit unit);
}
