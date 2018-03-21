package com.vesperin.base;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;

import java.util.List;
import java.util.Map;

/**
 * A sharable configuration type. Once a configuration is created, then
 * it does not matter how many parsers one creates, there is only
 * one configuration that all of them can share (gracefully). This
 * a type of builder that loves double dispatching.
 *
 * @author Huascar Sanchez
 */
public interface Configuration {
  /**
   * Configures a JavaParser object.
   *
   * WARNING: One can either call {@link #defaultSettings(JavaParser)}
   * or each of the individual configureXXXX methods. If the latter
   * workflow is chosen, then PLEASE end the configuration with a
   * call to {@link #cleanupAfter(JavaParser)}. This method guarantees
   * the proper resetting of {@link ASTParser} object.
   *
   * @param parser the parser to configure.
   */
  void configure(JavaParser parser);


  /**
   * Parser's default settings.
   * @param parser a JavaParser to be configured.
   */
  default void defaultSettings(JavaParser parser){
    configureCompilerOptions(parser);
    configureEnvironment(parser);
    configureBindings(parser);

    cleanupAfter(parser);

  }

  /**
   * Binding resolution settings.
   *
   * @param parser a JavaParser to be configured.
   */
  default void configureBindings(JavaParser parser){
    parser.getAstParser().setResolveBindings(true);
  }

  /**
   * Compiler options settings.
   *
   * @param parser a JavaParser to be configured.
   */
  default void configureCompilerOptions(JavaParser parser){
    final Map options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    parser.getAstParser().setCompilerOptions(options);
  }

  /**
   * Null environment settings.
   *
   * @param parser a JavaParser to be configured.
   */
  default void configureEnvironment(JavaParser parser){
    configureEnvironment(null, null, null, parser);
  }

  /**
   * Environment settings.
   *
   * @param cpEntries classpath entries
   * @param spEntries sourcepath entries
   * @param encodings list of encodings
   * @param parser a JavaParser to be configured.
   */
  default void configureEnvironment(List<String> cpEntries,
          List<String> spEntries, List<String> encodings, JavaParser parser){

    final String[] cp = (cpEntries == null || cpEntries.isEmpty())
      ? null : cpEntries.toArray(new String[cpEntries.size()]);

    final String[] scEntries = (spEntries == null || spEntries.isEmpty())
      ? null : spEntries.toArray(new String[spEntries.size()]);

    final String[] parserEncodings = (encodings == null || encodings.isEmpty())
      ? null : encodings.toArray(new String[encodings.size()]);

    parser.getAstParser().setEnvironment(
      cp,
      scEntries,
      parserEncodings,
      true
    );
  }

  /**
   * Resets {@link ASTParser} object.
   * @param javaParser to be reset.
   */
  default void cleanupAfter(JavaParser javaParser){

    javaParser.getAstParser().setKind(ASTParser.K_COMPILATION_UNIT);
    javaParser.getAstParser().setStatementsRecovery(true);
    javaParser.getAstParser().setBindingsRecovery(true);
    javaParser.getAstParser().setUnitName(null);
    javaParser.getAstParser().setSource((char[]) null);
  }
}
