package com.vesperin.base.requests;

import com.vesperin.base.spi.BindingRequest;
import com.vesperin.base.Scopes;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class BindingRequestBySignature implements BindingRequest {

  final List<IBinding>  requestedBindings;
  final Set<String>     signatures;
  final int             flags;
  final ITypeBinding    parentTypeBinding;


  /**
   * Constructs a BindingRequestBySignature object.
   */
  public BindingRequestBySignature(){
    this(null, 0);
  }

  /**
   * Constructs a BindingRequestBySignature object.
   * 
   * @param parentTypeBinding the binding of parent type.
   * @param flags the binding's access flags
   */
  public BindingRequestBySignature(ITypeBinding parentTypeBinding, int flags) {
    this.parentTypeBinding  = parentTypeBinding;
    this.flags              = flags;
    this.requestedBindings  = new ArrayList<>();
    this.signatures         = new HashSet<>();
  }

  @Override public boolean accept(IBinding binding) {
    if (binding == null) return false;

    final String signature = getSignature(binding);
    if (signature != null && signatures.add(signature)) {
      requestedBindings.add(binding);
    }

    return false;
  }

  /**
   * @return the list of requested-by-signature bindings.
   */
  public List<IBinding> getRequestedBindings() {
    if (Scopes.isVisibilityFlagAvailable(flags)) {
      for (int i = requestedBindings.size() - 1; i >= 0; i--) {
        final IBinding binding = requestedBindings.get(i);
        if (!isVisible(binding, parentTypeBinding)) {
          requestedBindings.remove(i);
        }
      }
    }

    return requestedBindings;
  }

}
