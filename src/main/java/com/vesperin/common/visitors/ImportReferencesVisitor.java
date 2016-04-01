package com.vesperin.common.visitors;

import com.vesperin.common.locations.Location;
import com.vesperin.common.locations.Locations;
import com.vesperin.common.utils.Jdt;
import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class ImportReferencesVisitor extends SkeletalVisitor {
  CompilationUnit root;

  private final Set<String>     typeNameImports;
  private final Set<SimpleName> typeImports;
  private final Set<String>     staticImports;
  private final boolean         skipMethodBodies;
  private final Location        selection;

  /**
   * Construct an ImportReferencesVisitor object.
   *
   * @param visitJavadocTags visit JavaDoc tags.
   * @param rangeLimit code range limit.
   */
  public ImportReferencesVisitor(boolean visitJavadocTags, Location rangeLimit){
    this(visitJavadocTags, rangeLimit, false);
  }

  /**
   * Construct an ImportReferencesVisitor object.
   *
   * @param visitJavadocTags visit JavaDoc tags.
   * @param rangeLimit code range limit.
   * @param skipMethodBodies skip method bodies during node visiting.
   */
  public ImportReferencesVisitor(boolean visitJavadocTags, Location rangeLimit, boolean skipMethodBodies) {
    super(visitJavadocTags);

    this.typeNameImports    = new HashSet<>();
    this.typeImports        = new HashSet<>();
    this.staticImports      = new HashSet<>();
    this.root               = null;
    this.skipMethodBodies   = skipMethodBodies;
    this.selection          = rangeLimit;
  }

  private void addReference(SimpleName name) {
    if (isNodeAffected(name)) {
      typeImports.add(name);
    }
  }

  private void evaluateQualifyingExpression(Expression expression, Name selector) {
    if (expression != null) {
      if (expression instanceof Name) {
        final Name name = (Name) expression;
        possibleTypeRefFound(name);
        confirmStaticImportIsFound(name);
      } else {
        expression.accept(this);
      }
    } else if (selector != null) {
      confirmStaticImportIsFound(selector);
    }
  }


  /**
   * @return the list of import identifiers.
   */
  public Set<String> getImportNameIdentifiers(){
    typeNameImports.addAll(getImportSimpleNames().stream()
        .map(Jdt::getSimpleNameIdentifier)
        .collect(Collectors.toList()));

    return typeNameImports;
  }

  /**
   * @return a list of import names.
   */
  public Set<SimpleName> getImportSimpleNames(){
    return typeImports;
  }


  /**
   * @return a list of static import identifiers.
   */
  public Set<String> getStaticImportNameIdentifiers(){
    return staticImports;
  }


  private boolean isNodeAffected(ASTNode node) {
    return (this.selection == null
        || Jdt.isNodeWithinSelection(Jdt.from(root), node, this.selection)
    );
  }

  private void confirmStaticImportIsFound(Name name) {
    if (staticImports == null || root == null) {
      return;
    }

    while (name.isQualifiedName()) {
      name  = ((QualifiedName) name).getQualifier();
    }

    if (!isNodeAffected(name)) {
      return;
    }

    final IBinding  binding = name.resolveBinding();
    if (binding == null
        || binding instanceof ITypeBinding
        || !Modifier.isStatic(binding.getModifiers())
        || ((SimpleName) name).isDeclaration()) {

      return;
    }

    if (binding instanceof IVariableBinding) {
      IVariableBinding variableBinding  = (IVariableBinding) binding;
      if (variableBinding.isField()) {
        variableBinding = variableBinding.getVariableDeclaration();
        final ITypeBinding declaringClass = variableBinding.getDeclaringClass();
        if (declaringClass != null && !declaringClass.isLocal()) {

          final ScopeAnalyser scope                     = new ScopeAnalyser(root);
          final boolean       isVariableDeclaredInScope  = scope.isElementDeclaredInScope(
              variableBinding, (SimpleName) name,
              Scopes.VARIABLES | Scopes.CHECK_VISIBILITY
          );

          if (isVariableDeclaredInScope) {
            return;
          }

          staticImports.add(Jdt.getSimpleNameIdentifier(name));
        }
      }
    } else if (binding instanceof IMethodBinding) {
      final IMethodBinding  methodBinding   = ((IMethodBinding) binding).getMethodDeclaration();
      final ITypeBinding    declaringClass  = methodBinding.getDeclaringClass();

      if (declaringClass != null && !declaringClass.isLocal()) {
        final ScopeAnalyser scope = new ScopeAnalyser(root);

        final boolean isMethodDeclaredInScope  = scope.isElementDeclaredInScope(
            methodBinding, (SimpleName) name,
            Scopes.METHODS | Scopes.CHECK_VISIBILITY
        );

        if (isMethodDeclaredInScope) {
          return;
        }

        staticImports.add(Jdt.getSimpleNameIdentifier(name));
      }
    }

  }

  private void doVisitChildren(List elements) {
    for (Object eachObject : elements) {
      final ASTNode eachNode = (ASTNode) eachObject;
      eachNode.accept(this);
    }
  }

  private void doVisitNode(ASTNode node) {
    if (node != null) {
      node.accept(this);
    }
  }

  private void possibleTypeRefFound(Name node) {
    while (node.isQualifiedName()) {
      node  = ((QualifiedName) node).getQualifier();
    }

    final IBinding binding  = node.resolveBinding();
    if (binding == null || binding.getKind() == IBinding.TYPE) {
      // if the binding is null, then we cannot determine if
      // we have a type binding or not, so we will assume
      // we do.
      addReference((SimpleName) node);
    }
  }

  @Override public boolean visit(CompilationUnit node) {
    if(node.getTypeRoot() == null){
      if(root == null){
        this.root = node;
        return true;
      }
    }

    return super.visit(node);
  }

  protected boolean visitNode(ASTNode node) {
    return isNodeAffected(node);
  }

  @Override public boolean visit(ArrayType node) {
    doVisitNode(node.getElementType());
    return false;
  }

  @Override public boolean visit(SimpleType node) {
    typeReferenceFound(node.getName());
    return false;
  }

  @Override public boolean visit(QualifiedType node) {
    // do nothing here, let the qualifier be visited
    return true;
  }

  @Override public boolean visit(QualifiedName node) {
    possibleTypeRefFound(node); // possible reference
    confirmStaticImportIsFound(node);
    return false;
  }

  @Override public boolean visit(ImportDeclaration node) {
    return false;
  }

  @Override public boolean visit(PackageDeclaration node) {
    doVisitNode(node.getJavadoc());
    doVisitChildren(node.annotations());
    return false;
  }


  @Override public boolean visit(ThisExpression node) {
    typeReferenceFound(node.getQualifier());
    return false;
  }


  @Override public boolean visit(ClassInstanceCreation node) {
    doVisitChildren(node.typeArguments());
    doVisitNode(node.getType());
    evaluateQualifyingExpression(node.getExpression(), null);

    if (node.getAnonymousClassDeclaration() != null) {
      node.getAnonymousClassDeclaration().accept(this);
    }

    doVisitChildren(node.arguments());

    return false;
  }

  @Override public boolean visit(MethodInvocation node) {
    evaluateQualifyingExpression(node.getExpression(), node.getName());
    doVisitChildren(node.typeArguments());
    doVisitChildren(node.arguments());
    return false;
  }


  @Override public boolean visit(SuperConstructorInvocation node) {
    if (!isNodeAffected(node)) {
      return false;
    }

    evaluateQualifyingExpression(node.getExpression(), null);
    doVisitChildren(node.typeArguments());
    doVisitChildren(node.arguments());
    return false;
  }

  @Override public boolean visit(FieldAccess node) {
    evaluateQualifyingExpression(node.getExpression(), node.getName());
    return false;
  }

  @Override public boolean visit(SimpleName node) {
    // if the call gets here, it can only be a variable reference
    confirmStaticImportIsFound(node);
    return false;
  }


  @Override public boolean visit(MarkerAnnotation node) {
    typeReferenceFound(node.getTypeName());
    return false;
  }


  @Override public boolean visit(NormalAnnotation node) {
    typeReferenceFound(node.getTypeName());
    doVisitChildren(node.values());
    return false;
  }


  @Override public boolean visit(SingleMemberAnnotation node) {
    typeReferenceFound(node.getTypeName());
    doVisitNode(node.getValue());
    return false;
  }

  @Override public boolean visit(TypeDeclaration node) {
    if(this.selection == null) return true;
    final Location typeLocation = Locations.locate(node);
    return Locations.inside(this.selection, typeLocation)
        ||  typeLocation.same(this.selection)
        || isNodeAffected(node);
  }


  @Override public boolean visit(MethodDeclaration node) {
    if (!isNodeAffected(node)) {
      return false;
    }

    doVisitNode(node.getJavadoc());

    doVisitChildren(node.modifiers());
    doVisitChildren(node.typeParameters());

    if (!node.isConstructor()) {
      doVisitNode(node.getReturnType2());
    }

    doVisitChildren(node.parameters());

    for (Object eachName : node.thrownExceptionTypes()) {
      if(eachName instanceof SimpleType) {
        final SimpleType type = (SimpleType) eachName;
        typeReferenceFound(type.getName());
      } else {
        typeReferenceFound((Name) eachName);
      }
    }

    if (!skipMethodBodies) {
      doVisitNode(node.getBody());
    }

    return false;
  }



  @Override public boolean visit(TagElement node) {
    final String        tagName    = node.getTagName();
    final List<Object>  fragments  = Jdt.typeSafeList(node.fragments());

    int idx = 0;

    if (tagName != null && !fragments.isEmpty()) {
      final Object first  = fragments.get(0);

      if (first instanceof Name) {
        if ("@throws".equals(tagName) || "@exception".equals(tagName)) {
          typeReferenceFound((Name) first);
        } else if ("@see".equals(tagName)
            || "@link".equals(tagName)
            || "@linkplain".equals(tagName)) {

          possibleTypeRefFound((Name) first);
        }

        idx++;
      }
    }

    for (int i  = idx; i < fragments.size(); i++) {
      doVisitNode((ASTNode) fragments.get(i));
    }

    return false;
  }

  @Override public boolean visit(MemberRef node) {
    final Name qualifier  = node.getQualifier();

    if (qualifier != null) {
      typeReferenceFound(qualifier);
    }

    return false;
  }

  @Override public boolean visit(MethodRef node) {
    final Name qualifier  = node.getQualifier();
    if (qualifier != null) {
      typeReferenceFound(qualifier);
    }

    final List parameters = node.parameters();
    if (parameters != null) {
      // visit MethodRefParameter with Type
      doVisitChildren(parameters);
    }

    return false;
  }

  private void typeReferenceFound(Name node) {
    if (node != null) {

      while (node.isQualifiedName()) {
        node= ((QualifiedName) node).getQualifier();
      }

      addReference((SimpleName) node);
    }
  }
}
