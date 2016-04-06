package com.vesperin.common.requests;

import com.vesperin.common.spi.BindingRequest;
import com.vesperin.common.Scopes;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * @author Huascar Sanchez
 */
public class BindingRequestByValue implements BindingRequest {

  private final int           flags;
  private final ITypeBinding  parentTypeBinding;
  private final IBinding      bindingToSearch;

  private boolean isBindingFound;
  private boolean isBindingVisible;

  /**
   * Creates a BindingRequestByValue object.
   *
   * @param toSearch the binding to search.
   * @param parentTypeBinding the binding of parent type.
   * @param flags the binding's access flags.
   */
  public BindingRequestByValue(IBinding toSearch, ITypeBinding parentTypeBinding, int flags) {
    this.flags = flags;
    this.bindingToSearch    = toSearch;
    this.parentTypeBinding  = parentTypeBinding;

    isBindingFound    = false;
    isBindingVisible  = true;
  }

  @Override public boolean accept(IBinding binding) {
    if (isBindingFound)
      return true;

    if (binding == null)
      return false;

    if (bindingToSearch.getKind() != binding.getKind()) {
      return false;
    }

    boolean checkVisibility = Scopes.isVisibilityFlagAvailable(flags);
    if (binding == bindingToSearch) {
      isBindingFound = true;
    } else {

      final IBinding bindingDeclaration = getDeclaration(binding);

      if (bindingDeclaration == bindingToSearch) {
        isBindingFound = true;
      } else if (bindingDeclaration.getName().equals(bindingToSearch.getName())) {
        String signature = getSignature(bindingDeclaration);
        if (signature != null && signature.equals(getSignature(bindingToSearch))) {
          if (checkVisibility) { isBindingVisible = false; }
          return true; // found element that hides the binding we are interested
        }
      }
    }

    if (isBindingFound && checkVisibility) {
      isBindingVisible = isVisible(binding, parentTypeBinding);
    }

    return isBindingFound;
  }


  public static IBinding getDeclaration(IBinding binding) {
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return ((ITypeBinding) binding).getTypeDeclaration();
      case IBinding.VARIABLE:
        return ((IVariableBinding) binding).getVariableDeclaration();
      case IBinding.METHOD:
        return ((IMethodBinding) binding).getMethodDeclaration();
    }
    return binding;
  }

  public boolean isBindingFound() {
    return isBindingFound;
  }

  public boolean isVisible() {
    return isBindingVisible;
  }
}
