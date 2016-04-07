package com.vesperin.base.locators;

import com.vesperin.base.locations.Location;
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
