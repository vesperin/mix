package com.vesperin.base;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.visitors.ImportReferencesVisitor;
import com.vesperin.utils.Immutable;
import com.vesperin.utils.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * CommonJDT provides commonly used JDT helper methods.
 */
public class CommonJdt {
  public static final String SOURCE_FILE_PROPERTY = "codepacking.source_file.source_file_property";

  private static final Map<Integer, String> AST_NODE_TYPE_TO_NAME;
  private static final Map<Integer, String> STATEMENTS_TO_NAME;
  static {
    final Map<Integer, String> local = new HashMap<>();

    local.put(ASTNode.ANONYMOUS_CLASS_DECLARATION, "ANONYMOUS_CLASS_DECLARATION");
    local.put(ASTNode.ARRAY_ACCESS, "ARRAY_ACCESS");
    local.put(ASTNode.ARRAY_CREATION, "ARRAY_CREATION");
    local.put(ASTNode.ARRAY_INITIALIZER, "ARRAY_INITIALIZER");
    local.put(ASTNode.ARRAY_TYPE, "ARRAY_TYPE");
    local.put(ASTNode.ASSERT_STATEMENT, "ASSERT_STATEMENT");
    local.put(ASTNode.ASSIGNMENT, "ASSIGNMENT");
    local.put(ASTNode.BLOCK, "BLOCK");
    local.put(ASTNode.BOOLEAN_LITERAL, "BOOLEAN_LITERAL");
    local.put(ASTNode.BREAK_STATEMENT, "BREAK_STATEMENT");
    local.put(ASTNode.CAST_EXPRESSION, "CAST_EXPRESSION");
    local.put(ASTNode.CATCH_CLAUSE, "CATCH_CLAUSE");
    local.put(ASTNode.CHARACTER_LITERAL, "CHARACTER_LITERAL");
    local.put(ASTNode.CLASS_INSTANCE_CREATION, "CLASS_INSTANCE_CREATION");
    local.put(ASTNode.COMPILATION_UNIT, "COMPILATION_UNIT");
    local.put(ASTNode.CONDITIONAL_EXPRESSION, "CONDITIONAL_EXPRESSION");
    local.put(ASTNode.CONSTRUCTOR_INVOCATION, "CONSTRUCTOR_INVOCATION");
    local.put(ASTNode.CONTINUE_STATEMENT, "CONTINUE_STATEMENT");
    local.put(ASTNode.DO_STATEMENT, "DO_STATEMENT");
    local.put(ASTNode.EMPTY_STATEMENT, "EMPTY_STATEMENT");
    local.put(ASTNode.EXPRESSION_STATEMENT, "EXPRESSION_STATEMENT");
    local.put(ASTNode.FIELD_ACCESS, "FIELD_ACCESS");
    local.put(ASTNode.FIELD_DECLARATION, "FIELD_DECLARATION");
    local.put(ASTNode.FOR_STATEMENT, "FOR_STATEMENT");
    local.put(ASTNode.IF_STATEMENT, "IF_STATEMENT");
    local.put(ASTNode.IMPORT_DECLARATION, "IMPORT_DECLARATION");
    local.put(ASTNode.INFIX_EXPRESSION, "INFIX_EXPRESSION");
    local.put(ASTNode.INITIALIZER, "INITIALIZER");
    local.put(ASTNode.JAVADOC, "JAVADOC");
    local.put(ASTNode.LABELED_STATEMENT, "LABELED_STATEMENT");
    local.put(ASTNode.METHOD_DECLARATION, "METHOD_DECLARATION");
    local.put(ASTNode.METHOD_INVOCATION, "METHOD_INVOCATION");
    local.put(ASTNode.NULL_LITERAL, "NULL_LITERAL");
    local.put(ASTNode.NUMBER_LITERAL, "NUMBER_LITERAL");
    local.put(ASTNode.PACKAGE_DECLARATION, "PACKAGE_DECLARATION");
    local.put(ASTNode.PARENTHESIZED_EXPRESSION, "PARENTHESIZED_EXPRESSION");
    local.put(ASTNode.POSTFIX_EXPRESSION, "POSTFIX_EXPRESSION");
    local.put(ASTNode.PREFIX_EXPRESSION, "PREFIX_EXPRESSION");
    local.put(ASTNode.PRIMITIVE_TYPE, "PRIMITIVE_TYPE");
    local.put(ASTNode.QUALIFIED_NAME, "QUALIFIED_NAME");
    local.put(ASTNode.RETURN_STATEMENT, "RETURN_STATEMENT");
    local.put(ASTNode.SIMPLE_NAME, "SIMPLE_NAME");
    local.put(ASTNode.SIMPLE_TYPE, "SIMPLE_TYPE");
    local.put(ASTNode.SINGLE_VARIABLE_DECLARATION, "SINGLE_VARIABLE_DECLARATION");
    local.put(ASTNode.STRING_LITERAL, "STRING_LITERAL");
    local.put(ASTNode.SUPER_CONSTRUCTOR_INVOCATION, "SUPER_CONSTRUCTOR_INVOCATION");
    local.put(ASTNode.SUPER_FIELD_ACCESS, "SUPER_FIELD_ACCESS");
    local.put(ASTNode.SUPER_METHOD_INVOCATION, "SUPER_METHOD_INVOCATION");
    local.put(ASTNode.SWITCH_CASE, "SWITCH_CASE");
    local.put(ASTNode.SWITCH_STATEMENT, "SWITCH_STATEMENT");
    local.put(ASTNode.SYNCHRONIZED_STATEMENT, "SYNCHRONIZED_STATEMENT");
    local.put(ASTNode.THIS_EXPRESSION, "THIS_EXPRESSION");
    local.put(ASTNode.THROW_STATEMENT, "THROW_STATEMENT");
    local.put(ASTNode.TRY_STATEMENT, "TRY_STATEMENT");
    local.put(ASTNode.TYPE_DECLARATION, "TYPE_DECLARATION");
    local.put(ASTNode.TYPE_DECLARATION_STATEMENT, "TYPE_DECLARATION_STATEMENT");
    local.put(ASTNode.TYPE_LITERAL, "TYPE_LITERAL");
    local.put(ASTNode.VARIABLE_DECLARATION_EXPRESSION, "VARIABLE_DECLARATION_EXPRESSION");
    local.put(ASTNode.VARIABLE_DECLARATION_FRAGMENT, "VARIABLE_DECLARATION_FRAGMENT");
    local.put(ASTNode.VARIABLE_DECLARATION_STATEMENT, "VARIABLE_DECLARATION_STATEMENT");
    local.put(ASTNode.WHILE_STATEMENT, "WHILE_STATEMENT");
    local.put(ASTNode.INSTANCEOF_EXPRESSION, "INSTANCEOF_EXPRESSION");
    local.put(ASTNode.LINE_COMMENT, "LINE_COMMENT");
    local.put(ASTNode.BLOCK_COMMENT, "BLOCK_COMMENT");
    local.put(ASTNode.TAG_ELEMENT, "TAG_ELEMENT");
    local.put(ASTNode.TEXT_ELEMENT, "TEXT_ELEMENT");
    local.put(ASTNode.MEMBER_REF, "MEMBER_REF");
    local.put(ASTNode.METHOD_REF, "METHOD_REF");
    local.put(ASTNode.METHOD_REF_PARAMETER, "METHOD_REF_PARAMETER");
    local.put(ASTNode.ENHANCED_FOR_STATEMENT, "ENHANCED_FOR_STATEMENT");
    local.put(ASTNode.ENUM_DECLARATION, "ENUM_DECLARATION");
    local.put(ASTNode.ENUM_CONSTANT_DECLARATION, "ENUM_CONSTANT_DECLARATION");
    local.put(ASTNode.TYPE_PARAMETER, "TYPE_PARAMETER");
    local.put(ASTNode.PARAMETERIZED_TYPE, "PARAMETERIZED_TYPE");
    local.put(ASTNode.QUALIFIED_TYPE, "QUALIFIED_TYPE");
    local.put(ASTNode.WILDCARD_TYPE, "WILDCARD_TYPE");
    local.put(ASTNode.NORMAL_ANNOTATION, "NORMAL_ANNOTATION");
    local.put(ASTNode.MARKER_ANNOTATION, "MARKER_ANNOTATION");
    local.put(ASTNode.SINGLE_MEMBER_ANNOTATION, "SINGLE_MEMBER_ANNOTATION");
    local.put(ASTNode.MEMBER_VALUE_PAIR, "MEMBER_VALUE_PAIR");
    local.put(ASTNode.ANNOTATION_TYPE_DECLARATION, "ANNOTATION_TYPE_DECLARATION");
    local.put(ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION, "ANNOTATION_TYPE_MEMBER_DECLARATION");
    local.put(ASTNode.MODIFIER, "MODIFIER");
    local.put(ASTNode.UNION_TYPE, "UNION_TYPE");
    local.put(ASTNode.DIMENSION, "DIMENSION");
    local.put(ASTNode.LAMBDA_EXPRESSION, "LAMBDA_EXPRESSION");
    local.put(ASTNode.INTERSECTION_TYPE, "INTERSECTION_TYPE");
    local.put(ASTNode.NAME_QUALIFIED_TYPE, "NAME_QUALIFIED_TYPE");
    local.put(ASTNode.CREATION_REFERENCE, "CREATION_REFERENCE");
    local.put(ASTNode.EXPRESSION_METHOD_REFERENCE, "EXPRESSION_METHOD_REFERENCE");
    local.put(ASTNode.SUPER_METHOD_REFERENCE, "SUPER_METHOD_REFERENCE");
    local.put(ASTNode.TYPE_METHOD_REFERENCE, "TYPE_METHOD_REFERENCE");

    AST_NODE_TYPE_TO_NAME = Immutable.mapOf(local);

    final Set<Integer> statementTypes = new HashSet<>();
    statementTypes.add(ASTNode.ASSERT_STATEMENT);
    statementTypes.add(ASTNode.BLOCK);
    statementTypes.add(ASTNode.BREAK_STATEMENT);
    statementTypes.add(ASTNode.CONSTRUCTOR_INVOCATION);
    statementTypes.add(ASTNode.CONTINUE_STATEMENT);
    statementTypes.add(ASTNode.DO_STATEMENT);
    statementTypes.add(ASTNode.EMPTY_STATEMENT);
    statementTypes.add(ASTNode.ENHANCED_FOR_STATEMENT);
    statementTypes.add(ASTNode.EXPRESSION_STATEMENT);
    statementTypes.add(ASTNode.FOR_STATEMENT);
    statementTypes.add(ASTNode.IF_STATEMENT);
    statementTypes.add(ASTNode.LABELED_STATEMENT);
    statementTypes.add(ASTNode.RETURN_STATEMENT);
    statementTypes.add(ASTNode.SUPER_CONSTRUCTOR_INVOCATION);
    statementTypes.add(ASTNode.SWITCH_CASE);
    statementTypes.add(ASTNode.SWITCH_STATEMENT);
    statementTypes.add(ASTNode.SYNCHRONIZED_STATEMENT);
    statementTypes.add(ASTNode.THROW_STATEMENT);
    statementTypes.add(ASTNode.TRY_STATEMENT);
    statementTypes.add(ASTNode.TYPE_DECLARATION_STATEMENT);
    statementTypes.add(ASTNode.VARIABLE_DECLARATION_STATEMENT);
    statementTypes.add(ASTNode.WHILE_STATEMENT);

    final Map<Integer, String> local2 = local.entrySet()
        .stream().filter(e -> statementTypes.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    STATEMENTS_TO_NAME    = Immutable.mapOf(local2);
  }

  private CommonJdt(){}

  public static String getNodeTypeName(int nodeType){
    return AST_NODE_TYPE_TO_NAME
        .getOrDefault(nodeType, "Unknown");
  }

  public static String getStatementTypeName(int statementType){
    return STATEMENTS_TO_NAME
        .getOrDefault(statementType, "Unknown");
  }


  /**
   * Returns the name of a method declaration.
   *
   * @param node the method declaration node.
   * @return method's name
   */
  public static Optional<String> findMethodName(ASTNode node){
    if (node.getNodeType() != ASTNode.METHOD_DECLARATION) return Optional.empty();

    return findMethodDeclaration(node)
        .map(MethodDeclaration::getName)
        .map(CommonJdt::getSimpleNameIdentifier);
  }


  /**
   * Downcast a method node to its method declaration.
   *
   * @param node node to downcast.
   * @return a method declaration
   */
  public static Optional<MethodDeclaration> findMethodDeclaration(ASTNode node){
    return Optional.ofNullable(node)
        .filter(n -> n.getNodeType() == ASTNode.METHOD_DECLARATION)
        .map(n -> (MethodDeclaration)n);
  }


  public static Optional<ASTNode> findStatement(final ASTNode node){
    ASTNode current = node;

    if (current == null || current.getParent() == null) return Optional.empty();

    if (Statement.class.isAssignableFrom(current.getParent().getClass())){
      return Optional.of(current.getParent());
    }

    do {
      current = current.getParent();
      if (current == null) {
        return Optional.empty();
      }
    } while (!Statement.class.isAssignableFrom(current.getClass()));

    return Optional.of(current);
  }


  public static Set<String> getImportStatements(Context context) {
    return getImportStatements(context, null/*entire source code*/);
  }


  public static Set<String> getImportStatements(Context context, Location selection) {
    final ImportReferencesVisitor visitor = createImportReferencesVisitor(
      CommonJdt.processJavadocComments(
        context.getCompilationUnit()
      ), selection
    );

    context.accept(visitor);

    final Set<String> instanceTypes = visitor.getImportNameIdentifiers();
    final Set<String> staticTypes   = visitor.getStaticImportNameIdentifiers();

    return Sets.union(instanceTypes, staticTypes);
  }

  public static ImportReferencesVisitor createImportReferencesVisitor(boolean processJavaDocs, Location range) {
    return new ImportReferencesVisitor(processJavaDocs, range);
  }

  private static List<ASTNode> convert(List<?> list) {
    final List<ASTNode> result = new ArrayList<>();
    for (Object each : list) {
      final ASTNode node = (ASTNode) each;
      if (!(node instanceof Javadoc)) {
        result.add(node);
      }
    }

    return result;
  }


  /**
   * Normalizes a type binding received from an expression to a type binding
   * one can use in a declaration signature.
   *
   * @param binding the binding to be normalized
   * @return the normalized binding
   */
  public static ITypeBinding normalizeTypeBinding(ITypeBinding binding) {
    if (binding != null && !binding.isNullType() && !isVoidType(binding)) {
      if (binding.isAnonymous()) {
        final ITypeBinding[] baseBindings = binding.getInterfaces();
        if (baseBindings.length > 0) {
          return baseBindings[0];
        }

        return binding.getSuperclass();
      }

      if (binding.isCapture()) {
        return binding.getWildcard();
      }

      return binding;
    }

    return null;
  }

  private static boolean isVoidType(ITypeBinding binding) {
    return "void".equals(binding.getName());
  }


  public static Optional<ASTNode> findASTNodeDeclaration(IBinding binding, ASTNode node) {
    node = (node instanceof CompilationUnit) ? node : node.getRoot();
    if (node != null) {
      return Optional.of(((CompilationUnit) node).findDeclaringNode(binding));
    }

    return Optional.empty();
  }


  public static Statement findParentStatement(ASTNode node) {
    while ((node != null) && (!(node instanceof Statement))) {
      node = node.getParent();
      if (node instanceof BodyDeclaration) {
        return null;
      }
    }
    return (Statement) node;
  }


  public static List<ASTNode> getChildren(ASTNode node) {
    final List<ASTNode> result = new ArrayList<>();

    if (node == null) {
      return result;
    }

    @SuppressWarnings("rawtypes")
    List list = node.structuralPropertiesForType(); // unchecked warning

    for (Object each : list) {
      final StructuralPropertyDescriptor descriptor = (StructuralPropertyDescriptor) each;
      final Object child = node.getStructuralProperty(descriptor);

      if (child instanceof List) {
        final List<?> castChild = (List<?>) child;
        result.addAll(convert(castChild));
      } else if (child instanceof ASTNode) {
        if (!(child instanceof Javadoc)) {
          result.add((ASTNode) child);
        }
      }

    }

    return result;
  }

  public static IBinding getBindingDeclaration(IBinding binding) {
    if (binding instanceof ITypeBinding) {
      return ((ITypeBinding) binding).getTypeDeclaration();
    } else if (binding instanceof IMethodBinding) {
      IMethodBinding methodBinding = (IMethodBinding) binding;
      if (methodBinding.isConstructor()) { // link all constructors with their type
        return methodBinding.getDeclaringClass().getTypeDeclaration();
      } else {
        return methodBinding.getMethodDeclaration();
      }
    } else if (binding instanceof IVariableBinding) {
      return ((IVariableBinding) binding).getVariableDeclaration();
    }
    return binding;
  }

  public static String getSimpleNameIdentifier(Name name) {

    if (name.isQualifiedName()) {
      final QualifiedName qualifiedName = (QualifiedName) name;
      return qualifiedName.getName().getIdentifier();
    } else {
      final SimpleName simpleName = (SimpleName) name;
      return simpleName.getIdentifier();
    }
  }

  public static List<TypeDeclaration> getTypeDeclarations(Context context) {
    final CompilationUnit unit = context.getCompilationUnit();
    return CommonJdt.typeSafeList(TypeDeclaration.class, unit.types());
  }

  public static List<ASTNode> getSwitchCases(SwitchStatement node) {
    final List<ASTNode> result = new ArrayList<>();
    final List<ASTNode> elements = typeSafeList(ASTNode.class, node.statements());
    elements.stream().filter(element -> element instanceof SwitchCase).forEach(element -> {
      final SwitchCase switchCase = (SwitchCase) element;
      result.add(switchCase);

    });
    return result;
  }


  public static boolean isNodeWithinSelection(Source src, ASTNode node, Location selection) {

    final Location nodeLocation = Locations.locate(src, node);

    return (Locations.inside(nodeLocation, selection))
      || (Locations.covers(selection, nodeLocation))
      || selection.same(nodeLocation);
  }

  public static boolean isParent(ASTNode node, ASTNode parent) {

    ASTNode a = Objects.requireNonNull(node);

    if (parent == null) return false;

    do {
      a = a.getParent();
      if (a == parent) return true;
    } while (a != null);

    return false;
  }

  public static boolean isMissingTypeDeclarationUnit(ASTNode parsed) {
    if (parsed instanceof TypeDeclaration) {
      final TypeDeclaration unit = (TypeDeclaration) parsed;
      return !"".equals(unit.toString()) && "MISSING".equals(unit.getName().getIdentifier());
    }

    return false;
  }

  public static boolean isWellConstructedCompilationUnit(ASTNode parsed) {
    if (parsed instanceof CompilationUnit) {
      final CompilationUnit unit = (CompilationUnit) parsed;
      return !"".equals(unit.toString()) && !unit.types().isEmpty();
    }

    return false;
  }

  public static <T extends ASTNode> T parent(final Class<T> thatClass, final ASTNode node) {

    ASTNode parent = node;

    if (parent.getClass() == thatClass) {
      // if both classes are the same, then no point on
      // executing the do-while code.
      return thatClass.cast(parent);
    }

    do {
      parent = parent.getParent();
      if (parent == null) {
        return null;
      }
    } while (parent.getClass() != thatClass);

    return thatClass.cast(parent);
  }

  public static boolean processJavadocComments(CompilationUnit astRoot) {
    return !(astRoot != null && astRoot.getTypeRoot() != null)
      || !"package-info.java".equals(astRoot.getTypeRoot().getElementName());
  }


  public static boolean requiresMainMethod(TypeDeclaration bodyDeclaration) {
    final List<ASTNode> children = CommonJdt.getChildren(bodyDeclaration);
    for (ASTNode eachNode : children) {
      if (eachNode instanceof MethodDeclaration) { // if it has at least one method ... then we don't need a main method
        return false;
      }
    }

    return true;
  }



  public static <T> List<T> typeSafeList(Class<T> klass, List<?> raw) {
    final List<T> result = new ArrayList<>();
    raw.forEach(r -> result.add(klass.cast(r)));
    return result;
  }

  public static List<Object> typeSafeList(List<?> raw) {
    return new ArrayList<>(raw);
  }


  /**
   * Creates a Source object from an ASTNode object.
   *
   * @param astNode The ASTNode from where the Source object will be extracted.
   * @return the new Source file.
   */
  public static Source from(ASTNode astNode){
    final ASTNode nonNullNode = Objects.requireNonNull(astNode.getRoot());
    return (Source) nonNullNode.getProperty(SOURCE_FILE_PROPERTY);
  }
}
