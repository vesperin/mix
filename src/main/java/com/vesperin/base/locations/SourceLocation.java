package com.vesperin.base.locations;

import com.vesperin.base.Source;

/**
 * @author Huascar Sanchez
 */
public final class SourceLocation implements Location {
  private final Source code;
  private final Position start;
  private final Position  end;

  /**
   * Constructs a new location object for a given file. If
   * the length of the location is not known, end may be null.
   *
   * @param code the associated code snippet (but see the documentation for {@link #getSource()}
   *             for more information on what the code snippet represents)
   * @param start the starting position, or null
   * @param end the ending position, or null
   */
  public SourceLocation(Source code, Position start, Position end){
    this.code   = code;
    this.start  = start;
    this.end    = end;
  }


  @Override public int compareTo(Location location) {
    final int startLine         = getStart().getLine();
    final int endLine           = getEnd().getLine();
    final int otherStartLine    = location.getStart().getLine();
    final int otherEndLine      = location.getEnd().getLine();

    final int lineDiff = startLine - otherStartLine;

    if (lineDiff != 0) {
      return lineDiff;
    }

    return endLine - otherEndLine;
  }

  @Override public Source getSource() {
    return code;
  }

  @Override public Position getStart() {
    return start;
  }

  @Override public Position getEnd() {
    return end;
  }

  @Override public String toString() {
    return "Location(start=" + getStart() + ", end=" + getEnd() + ")";
  }
}
