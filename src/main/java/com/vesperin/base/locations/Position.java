package com.vesperin.base.locations;

/**
 * @author Huascar Sanchez
 */
public interface Position {
  /**
   * Returns the line number (0-based where the first line is line 0)
   *
   * @return the 0-based line number
   */
  int getLine();

  /**
   * The character offset
   *
   * @return the 0-based character offset
   */
  int getOffset();

  /**
   * Returns the column number (where the first character on the line is 0),
   * or -1 if unknown
   *
   * @return the 0-based column number
   */
  int getColumn();
}