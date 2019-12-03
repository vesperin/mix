package com.vesperin.base.matchers;

import com.vesperin.base.*;
import com.vesperin.utils.Immutable;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class MatchMaker {
  private MatchMaker(){
    throw new Error("" +
        "Utility class"
    );
  }

  public static List<ContextMatcher> generateUnitMatchers(JavaParser javaParser){

    final Configuration configuration = javaParser.getConfiguration();

    final List<ContextMatcher> safeList = Arrays.asList(
      new ValidCompilationUnitMatching(configuration),
      new MissingClassDeclaration(configuration),
      new MissingClassAndMethodBodyDeclarations(configuration)
    );

    return Immutable.listOf(safeList);
  }

  static class ValidCompilationUnitMatching extends AbstractContextMatcher {
    ValidCompilationUnitMatching(Configuration configuration) {
      super(ASTParser.K_COMPILATION_UNIT, configuration);
    }

    @Override public ParsedUnit matches(Context context) {

      final ParsedUnit unit = parseJava(context);
      final ASTNode node = unit.getParsedNode();
      if(isWellConstructedCompilationUnit(getParsingMode(), node)){

        return bindProgramUnitToContext(
            context,
            ParsedUnit.makeUnit(node, false)
        );
      }

      return ParsedUnit.empty();
    }

  }

  static class MissingClassDeclaration extends AbstractContextMatcher {
    MissingClassDeclaration(Configuration configuration){
      super(ASTParser.K_CLASS_BODY_DECLARATIONS, configuration);
    }

    @Override public ParsedUnit matches(Context context) {
      final ParsedUnit unit = parseJava(context);
      final ASTNode node = unit.getParsedNode();

      if(isMissingTypeBodyDeclaration(getParsingMode(), node)){
        return bindProgramUnitToContext(
            context,
            ParsedUnit.makeUnit(node, true)
        );
      }

      return ParsedUnit.empty();
    }
  }

  static class MissingClassAndMethodBodyDeclarations extends AbstractContextMatcher {
    MissingClassAndMethodBodyDeclarations(Configuration configuration){
      super(ASTParser.K_STATEMENTS, configuration);
    }

    @Override public ParsedUnit matches(Context context) {
      final ParsedUnit unit = parseJava(context);
      final ASTNode node = unit.getParsedNode();

      final TypeDeclaration parent = Objects.requireNonNull(
          CommonJdt.parent(TypeDeclaration.class, node));
      final String          name   = parent.getName().getIdentifier();

      if("MISSING".equals(name)){
        if(CommonJdt.requiresMainMethod(parent)){
          return bindProgramUnitToContext(
              context,
              ParsedUnit.makeUnit(node, true)
          );
        } else {
          return bindProgramUnitToContext(
              context,
              ParsedUnit.makeUnit(node, true)
          );
        }
      }

      return ParsedUnit.empty();
    }
  }

}
