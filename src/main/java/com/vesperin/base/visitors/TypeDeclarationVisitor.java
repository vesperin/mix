package com.vesperin.base.visitors;

import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class TypeDeclarationVisitor extends SkeletalVisitor {
  private Set<TypeDeclaration> types;

  public TypeDeclarationVisitor(){
    this.types = new HashSet<>();
  }

  public Set<TypeDeclaration> getTypes(){
    return this.types;
  }

  @Override public boolean visit(TypeDeclaration node) {
    this.types.add(node);

    return super.visit(node);
  }
}
