package com.vesperin.common.visitors;

import org.eclipse.jdt.core.dom.IBinding;

/**
 * @author Huascar Sanchez
 */
public class Scopes {

  public static final int METHODS             = 1;  // Specifies that methods should be reported.
  public static final int VARIABLES           = 2;  // Specifies that variables should be reported.
  public static final int TYPES               = 4;  // Specifies that types should be reported.
  public static final int CHECK_VISIBILITY    = 16; // Specifies that only visible elems should be added.

  public static final IBinding[] EMPTY_BINDINGS = new IBinding[0];

  private Scopes(){
    throw new Error("Utility class");
  }

  public static boolean isTypesFlagAvailable(int flags){
    return isFlagAvailable(TYPES, flags);
  }

  public static boolean isMethodsFlagAvailable(int flags){
    return isFlagAvailable(METHODS, flags);
  }

  public static boolean isVariablesFlagAvailable(int flags){
    return isFlagAvailable(VARIABLES, flags);
  }

  public static boolean isVisibilityFlagAvailable(int flags){
    return isFlagAvailable(CHECK_VISIBILITY, flags);
  }

  public static boolean isFlagAvailable(int property, int flags) {
    return (flags & property) != 0;
  }
}
