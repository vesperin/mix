package com.vesperin.base.matchers;

import com.google.common.collect.ImmutableList;
import com.vesperin.base.Context;
import com.vesperin.base.ParsedUnit;
import com.vesperin.base.utils.Jdt;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.TypeDeclaration;

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

  public static List<ContextMatcher> generateUnitMatchers(){
    return ImmutableList.of(
        new ValidCompilationUnitMatching(),
        new MissingClassDeclaration(),
        new MissingClassAndMethodBodyDeclarations()
    );
  }

  static class ValidCompilationUnitMatching extends AbstractContextMatcher {
    ValidCompilationUnitMatching() {
      super(ASTParser.K_COMPILATION_UNIT);
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
    MissingClassDeclaration(){
      super(ASTParser.K_CLASS_BODY_DECLARATIONS);
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
    MissingClassAndMethodBodyDeclarations(){
      super(ASTParser.K_STATEMENTS);
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
