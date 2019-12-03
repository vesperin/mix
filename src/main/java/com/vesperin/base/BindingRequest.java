package com.vesperin.base;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

/**
 * A strategy object that request bindings.
 * @author Huascar Sanchez
 */
public interface BindingRequest {
  /**
   * Visits a binding object.
   *
   * @param binding the binding to be accepted.
   * @return true if the binding is collected; false otherwise.
   */
  boolean accept(IBinding binding);

  /**
   * Gets the signature of the binding.
   *
   * @param binding IBinding object.
   * @return the binding's signature.
   */
  default String getSignature(IBinding binding) {
    if (binding != null) {
      switch (binding.getKind()) {
        case IBinding.METHOD:
          return generateSignature(binding);
        case IBinding.VARIABLE:
          return 'V' + binding.getName();
        case IBinding.TYPE:
          return 'T' + binding.getName();
      }
    }

    return null;
  }

  /**
   * Generates a signature for the binding object.
   *
   * @param binding IBinding object.
   * @return the produced signature.
   */
  static String generateSignature(IBinding binding) {
    if(!(binding instanceof MethodBinding) && IBinding.METHOD != binding.getKind()) {
      throw new IllegalArgumentException(
          "Error: Not an IMethodBinding!"
      );
    }

    final StringBuilder signature   = new StringBuilder();

    signature.append('M');
    signature.append(binding.getName()).append('(');

    final IMethodBinding methodBinding  = ((IMethodBinding) binding);
    final ITypeBinding[] parameters     = methodBinding.getParameterTypes();

    for (int i = 0; i < parameters.length; i++) {
      if (i > 0) {
        signature.append(',');
      }

      final ITypeBinding parameterType  = parameters[i].getErasure();
      final String       qualifiedName  = parameterType.getQualifiedName();

      signature.append(qualifiedName);
    }

    signature.append(')');
    return signature.toString();
  }

  /**
   * Evaluates if the declaration is visible in a certain context.
   *
   * @param binding The binding of the declaration to examine
   * @param context The context to test in
   * @return {@code true} if the declaration is visible; {@code false} otherwise.
   */
  default boolean isVisible(IBinding binding, ITypeBinding context) {
    if (binding.getKind() == IBinding.VARIABLE && !((IVariableBinding) binding).isField()) {
      return true; // all local variables found are visible
    }

    ITypeBinding declaring  = getDeclaringType(binding);
    if (declaring == null) {
      return false;
    }

    declaring = declaring.getTypeDeclaration();

    int modifiers   = binding.getModifiers();
    if (Modifier.isPublic(modifiers) || declaring.isInterface()) { return true; } else if
        (Modifier.isProtected(modifiers) || !Modifier.isPrivate(modifiers)) {

      final boolean sameDeclaringPackage = declaring.getPackage() == context.getPackage();
      final boolean isTypeInScope        = isTypeInScope(
          declaring,
          context,
          Modifier.isProtected(modifiers)
      );

      return sameDeclaringPackage || isTypeInScope;
    }

    // private visibility
    return isTypeInScope(declaring, context, false);
  }

  static ITypeBinding getDeclaringType(IBinding binding) {
    switch (binding.getKind()) {
      case IBinding.VARIABLE:
        return ((IVariableBinding) binding).getDeclaringClass();
      case IBinding.METHOD:
        return ((IMethodBinding) binding).getDeclaringClass();
      case IBinding.TYPE:
        final ITypeBinding typeBinding  = (ITypeBinding) binding;
        if (typeBinding.getDeclaringClass() != null) {
          return typeBinding;
        }

        return typeBinding;
    }

    return null;
  }

  static boolean isTypeInScope(ITypeBinding declaring, ITypeBinding context, boolean includeHierarchy) {
    ITypeBinding typeBinding   = context.getTypeDeclaration();
    while (typeBinding != null && typeBinding != declaring) {
      if (includeHierarchy && isInSuperTypeHierarchy(declaring, typeBinding)) {
        return true;
      }

      typeBinding = typeBinding.getDeclaringClass();
    }

    return typeBinding == declaring;
  }

  /**
   * Checks if a binding is in the super type hierarchy by comparing
   * it against the available type declarations.
   *
   * @param possibleSuperTypeDeclaration ITypeBinding object.
   * @param type the "possibly-a-subtype" binding.
   * @return true if the binding is indeed a sub type of the
   *    given super type declaration; false otherwise.
   */
  static boolean isInSuperTypeHierarchy(ITypeBinding possibleSuperTypeDeclaration, ITypeBinding type) {
    if (type == possibleSuperTypeDeclaration) {
      return true;
    }

    final ITypeBinding superClass = type.getSuperclass();

    if (superClass != null) {
      if (isInSuperTypeHierarchy(possibleSuperTypeDeclaration, superClass.getTypeDeclaration())) {
        return true;
      }
    }

    if (possibleSuperTypeDeclaration.isInterface()) {
      final ITypeBinding[] allSuperInterfaces  = type.getInterfaces();
      for (ITypeBinding eachSuperInterface : allSuperInterfaces) {

        final boolean isInSuperTypeHierarchy = isInSuperTypeHierarchy(
            possibleSuperTypeDeclaration,
            eachSuperInterface.getTypeDeclaration()
        );

        if (isInSuperTypeHierarchy) {
          return true;
        }
      }
    }

    return false;
  }
}
