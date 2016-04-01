package com.vesperin.common.locations;

import com.vesperin.common.Source;

/**
 * @author Huascar Sanchez
 */
public interface Location extends Comparable<Location> {

  default boolean begins(Position start){
    final Position baseStart    = getStart();
    return baseStart.getOffset() <= start.getOffset();

  }

  // makes explicit the fact that this method needs to be implemented
  @Override int compareTo(Location o);


  default boolean ends(Position end){
    final Position baseEnd  = getEnd();

    return baseEnd.getOffset() <= end.getOffset();
  }

  /**
   * Returns the {@link Source} linked from this {@link Location}.
   *
   * @return the source code containing for the location
   */
  Source getSource();

  /**
   * The start position of the range
   *
   * @return the start position of the range, or null
   */
  Position getStart();

  /**
   * The end position of the range
   *
   * @return the end position of the range, may be null for an empty range
   */
  Position getEnd();

  /**
   * Checks whether {@code that} location is the same as {@code this} location.
   *
   * @param that The other location.
   * @return true if they are the same; false otherwise.
   */
  default boolean same(Location that){
    final Position start = this.getStart();
    final Position end   = this.getEnd();

    final Position otherStart   = that.getStart();
    final Position otherEnd     = that.getEnd();

    return start.equals(otherStart) && end.equals(otherEnd);
  }

  // implementors must implement this method
  @Override String toString();
}