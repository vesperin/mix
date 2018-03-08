package com.vesperin.base;

import com.vesperin.utils.Expect;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * @author Huascar Sanchez
 */
public class EclipseJavaParser implements JavaParser {
  private static final String JAVA_EXTENSION = ".java";

  private final ASTParser astParser;
  private final Configuration configuration;

  /**
   * Construct a new Eclipse Java parser with a default configuration.
   */
  public EclipseJavaParser(){
    this(new DefaultConfiguration());
  }

  /**
   * Construct a new Eclipse Java parser.
   *
   * @param configuration JavaParser's configuration
   */
  public EclipseJavaParser(Configuration configuration){
    this.configuration = Expect.nonNull(configuration);;
    this.astParser = ASTParser.newParser(AST.JLS8);
    configuration.configure(this);
  }



  @Override public Configuration getConfiguration() {
    return configuration;
  }

  @Override public ASTParser getAstParser() {
    return this.astParser;
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

  private static class DefaultConfiguration implements Configuration {
    @Override public void configure(JavaParser parser) {
      defaultSettings(parser);
    }
  }


}
