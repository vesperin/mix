package com.vesperin.base;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.visitors.ImportReferencesVisitor;
import com.vesperin.utils.Sets;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Jdt {
  public static final String SOURCE_FILE_PROPERTY = "codepacking.source_file.source_file_property";

  public static Set<String> collectImportCandidates(Context context) {
    return collectImportCandidates(context, null);
  }


  public static Set<String> collectImportCandidates(Context context, Location selection) {
    final ImportReferencesVisitor visitor = createImportReferencesVisitor(
      Jdt.processJavadocComments(
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

  private static List<ASTNode> convert(List list) {
    final List<ASTNode> result = new ArrayList<>();
    for (Object each : list) {
      final ASTNode node = (ASTNode) each;
      if (!(node instanceof Javadoc)) {
        result.add(node);
      }
    }

    return result;
  }


  public static ASTNode findASTNodeDeclaration(IBinding binding, ASTNode node) {
    node = (node instanceof CompilationUnit) ? node.getRoot() : node;
    if (node != null) {
      return CompilationUnit.class.cast(node).findDeclaringNode(binding);
    }

    return null;
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

    List list = node.structuralPropertiesForType();

    for (Object each : list) {
      final StructuralPropertyDescriptor descriptor = (StructuralPropertyDescriptor) each;
      final Object child = node.getStructuralProperty(descriptor);

      if (child instanceof List) {
        result.addAll(convert((List) child));
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
      return QualifiedName.class.cast(name)
        .getName().getIdentifier();
    } else {
      return SimpleName.class.cast(name)
        .getIdentifier();
    }
  }

  public static List<TypeDeclaration> getTypeDeclarations(Context context) {
    final CompilationUnit unit = context.getCompilationUnit();
    return Jdt.typeSafeList(TypeDeclaration.class, unit.types());
  }

  public static List<ASTNode> getSwitchCases(SwitchStatement node) {
    final List<ASTNode> result = new ArrayList<>();
    final List<ASTNode> elements = typeSafeList(ASTNode.class, node.statements());
    elements.stream().filter(element -> element instanceof SwitchCase).forEach(element -> {
      final SwitchCase switchCase = SwitchCase.class.cast(element);
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
      if (!"".equals(unit.toString()) && "MISSING".equals(unit.getName().getIdentifier())) {
        return true;
      }
    }

    return false;
  }

  public static boolean isWellConstructedCompilationUnit(ASTNode parsed) {
    if (parsed instanceof CompilationUnit) {
      final CompilationUnit unit = (CompilationUnit) parsed;
      if (!"".equals(unit.toString()) && !unit.types().isEmpty()) {
        return true;
      }
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
    final List<ASTNode> children = Jdt.getChildren(bodyDeclaration);
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
    final List<Object> typeSafeList = new ArrayList<>();
    typeSafeList.addAll(raw);

    return typeSafeList;
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
