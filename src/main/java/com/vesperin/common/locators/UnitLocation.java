package com.vesperin.common.locators;

import com.vesperin.common.locations.Location;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author Huascar Sanchez
 */
public interface UnitLocation extends Location {
  /**
   * @return the AST node at this location.
   */
  ASTNode getUnitNode();
}
