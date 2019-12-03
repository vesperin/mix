package com.vesperin.base.matchers;

import com.vesperin.base.*;
import com.vesperin.base.locations.Locations;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * @author Huascar Sanchez
 */
public abstract class AbstractContextMatcher implements ContextMatcher {
  private final int mode;
  private final JavaParser parser;


  /**
   * A constructor that everybody extending this class should
   * use.
   *
   * @param mode the parsing mode.
   */
  protected AbstractContextMatcher(int mode, Configuration configuration){
    this.mode     = mode;
    this.parser   = new EclipseJavaParser(configuration);
  }

  protected static ParsedUnit bindProgramUnitToContext(Context context, ParsedUnit unit){
    Context.bindContext(context, unit);
    context.setScope(Locations.locate(context.getSource(), unit.getParsedNode()));
    unit.setContext(context);
    return unit;
  }

  protected ParsedUnit parseJava(Context context){
    return parser.parseJava(context, this.getParsingMode());
  }


  protected int getParsingMode(){
    return this.mode;
  }

  protected static boolean isWellConstructedCompilationUnit(int mode, ASTNode node){
    return ASTParser.K_COMPILATION_UNIT == mode
        && CommonJdt.isWellConstructedCompilationUnit(node);
  }


  protected static boolean isMissingTypeBodyDeclaration(int mode, ASTNode node){
    return mode == ASTParser.K_CLASS_BODY_DECLARATIONS
        && CommonJdt.isMissingTypeDeclarationUnit(node);
  }

  @Override public String toString() {
    return "ContextMatcher(mode="
        + this.getParsingMode()
        + ")";
  }
}
