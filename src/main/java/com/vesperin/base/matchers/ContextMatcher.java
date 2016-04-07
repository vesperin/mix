package com.vesperin.base.matchers;

import com.vesperin.base.Context;
import com.vesperin.base.ParsedUnit;
import com.vesperin.base.spi.Matcher;

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
