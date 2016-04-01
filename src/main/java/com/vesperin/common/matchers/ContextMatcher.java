package com.vesperin.common.matchers;

import com.vesperin.common.Context;
import com.vesperin.common.ParsedUnit;
import com.vesperin.common.spi.Matcher;

/**
 * @author Huascar Sanchez
 */
public interface ContextMatcher extends Matcher<ParsedUnit, Context> {
  /**
   * Checks whether a current context is either a "complete" compilation unit,
   * a partial compilation unit with no type body declaration, or a partial
   * compilation unit with no type and method body declaration.
   *
   * @param context the context of interest.
   * @return a matched program unit.
   */
  @Override ParsedUnit matches(Context context);
}
