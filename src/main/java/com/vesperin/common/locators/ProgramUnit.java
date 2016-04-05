package com.vesperin.common.locators;

import com.vesperin.common.Context;
import com.vesperin.common.locations.Location;

import java.util.List;

/**
 * @author Huascar Sanchez
 */
public interface ProgramUnit {
  /**
   * @return the unit's name.
   */
  String getIdentifier();

  /**
   * Returns the list of locations where <strong>this</strong>
   * program unit occurs.
   *
   * @param context a parsed context.
   * @return new list of locations.
   */
  List<Location> getLocations(Context context);
}
