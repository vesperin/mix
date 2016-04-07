package com.vesperin.base.spi;


/**
 * @author Huascar Sanchez
 */
public interface Matcher <R, I> {
  /**
   * matches an input {@literal I} to an output {@literal R}.
   *
   * @param input the input to match.
   * @return the matched output.
   */
  R matches(I input);
}
