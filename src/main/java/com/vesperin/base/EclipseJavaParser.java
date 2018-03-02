package com.vesperin.base;

import com.vesperin.utils.Expect;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import java.util.Map;

/**
 * @author Huascar Sanchez
 */
public class EclipseJavaParser implements JavaParser {
  private static final String JAVA_EXTENSION = ".java";

  private final ASTParser astParser;

  /**
   * Construct a new Eclipse Java parser with a default configuration.
   */
  public EclipseJavaParser(){
    this(new JavaParserConfiguration());
  }

  /**
   * Construct a new Eclipse Java parser.
   *
   * @param configuration JavaParser's configuration
   */
  EclipseJavaParser(JavaParserConfiguration configuration){
    final JavaParserConfiguration nonNull = Expect.nonNull(configuration);
    this.astParser = nonNull.getAstParser();

    this.astParser.setResolveBindings(true);
    this.astParser.setEnvironment(null, null, null, true);

    final Map options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    astParser.setCompilerOptions(options);
  }

  @Override public ParsedUnit parseJava(Context context, int mode) {

    this.astParser.setKind(mode);
    this.astParser.setStatementsRecovery(true);
    this.astParser.setBindingsRecovery(true);
    this.astParser.setUnitName(context.getSource().getName() + JAVA_EXTENSION);

    final String content = context.getSourceContent();
    if(content == null || content.isEmpty()) throw new RuntimeException("Error: No source code to parse!");

    this.astParser.setSource(content.toCharArray());

    ASTNode unit;
    try {
      unit = this.astParser.createAST(null);
      if(unit == null){
        return ParsedUnit.empty();
      }

      return ParsedUnit.makeUnit(
          unit,
          (mode == ASTParser.K_STATEMENTS || mode == ASTParser.K_EXPRESSION)
      );
    } catch (RuntimeException error){
      throw new RuntimeException("Error: Unable to parse!");
    }
  }


}
