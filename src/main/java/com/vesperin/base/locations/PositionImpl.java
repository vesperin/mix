package com.vesperin.base.locations;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public final class PositionImpl implements Position {
  /**
   * The line number (0-based where the first line is line 0)
   */
  private final int line;

  /**
   * The column number (where the first character on the line is 0), or -1 if
   * unknown
   */
  private final int column;

  /**
   * The character offset
   */
  private final int offset;

  /**
   * Creates a new {@link PositionImpl}
   *
   * @param line   the 0-based line number, or -1 if unknown
   * @param column the 0-based column number, or -1 if unknown
   * @param offset the offset, or -1 if unknown
   */
  public PositionImpl(int line, int column, int offset) {
    this.line   = line;
    this.column = column;
    this.offset = offset;
  }

  @Override public boolean equals(Object obj) {
    if(!(obj instanceof Position)) return false;

    final Position other = (Position)obj;
    final boolean sameLines = getLine()   == other.getLine();
    final boolean sameCols  = getColumn() == other.getColumn();
    final boolean sameOffs  = getOffset() == other.getOffset();

    return sameLines && sameCols && sameOffs;
  }

  @Override public int getLine() {
    return line;
  }

  @Override public int getOffset() {
    return offset;
  }

  @Override public int getColumn() {
    return column;
  }

  @Override public int hashCode() {
    return 31 * Objects.hash(getLine(), getColumn(), getOffset());
  }

  @Override public String toString() {
    return ("PositionImpl(line=" + getLine()
        + ", column=" + getColumn()
        + ", offset=" + getOffset()
        + ")"
    );
  }
}
