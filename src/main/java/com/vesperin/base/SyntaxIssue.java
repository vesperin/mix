package com.vesperin.base;

import com.vesperin.base.locations.Location;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class SyntaxIssue {
  private final String    message;
  private final Location  errorLocation;

  /**
   * Constructs a syntax issue found during the parsing of a source file.
   * @param message the error message
   * @param errorLocation the location of the code element that produced the error.
   */
  public SyntaxIssue(String message, Location errorLocation){
    this.message = Objects.requireNonNull(message);
    this.errorLocation = Objects.requireNonNull(errorLocation);
  }

  @Override public boolean equals(Object o) {
    if(!(o instanceof SyntaxIssue)){
      return false;
    }

    final SyntaxIssue e = (SyntaxIssue) o;

    final boolean sameMessage = e.getMessage().equals(getMessage());
    final boolean sameLocation = e.getErrorLocation().same(getErrorLocation());

    return sameMessage
        && sameLocation;
  }

  /**
   * @return the location of the code element that produced the error.
   */
  public Location getErrorLocation() {
    return errorLocation;
  }

  /**
   * @return the error message produced by some code element.
   */
  public String getMessage() {
    return message;
  }

  @Override public int hashCode() {
    return Objects.hash(getMessage(), getErrorLocation());
  }

  @Override public String toString() {
    return getMessage() + ". " + getErrorLocation();
  }
}
