package com.vesperin.base.matchers;

import com.vesperin.base.Context;
import com.vesperin.base.JavaParser;
import com.vesperin.base.ParsedUnit;
import com.vesperin.utils.Immutable;
import com.vesperin.base.Jdt;
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

    final List<ContextMatcher> safeList = Arrays.asList(
      new ValidCompilationUnitMatching(javaParser),
      new MissingClassDeclaration(javaParser),
      new MissingClassAndMethodBodyDeclarations(javaParser)
    );

    return Immutable.listOf(safeList);
  }

  static class ValidCompilationUnitMatching extends AbstractContextMatcher {
    ValidCompilationUnitMatching(JavaParser javaParser) {
      super(ASTParser.K_COMPILATION_UNIT, javaParser);
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
    MissingClassDeclaration(JavaParser javaParser){
      super(ASTParser.K_CLASS_BODY_DECLARATIONS, javaParser);
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
    MissingClassAndMethodBodyDeclarations(JavaParser javaParser){
      super(ASTParser.K_STATEMENTS, javaParser);
    }

    @Override public ParsedUnit matches(Context context) {
      final ParsedUnit unit = parseJava(context);
      final ASTNode node = unit.getParsedNode();

      final TypeDeclaration parent = Objects.requireNonNull(Jdt.parent(TypeDeclaration.class, node));
      final String          name   = parent.getName().getIdentifier();

      if("MISSING".equals(name)){
        if(Jdt.requiresMainMethod(parent)){
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
