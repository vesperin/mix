package com.vesperin.common;

import com.vesperin.common.matchers.ContextMatcher;
import com.vesperin.common.matchers.MatchMaker;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public interface JavaParser {
  /**
   * Parses a source file.
   *
   * @param code the source file to be parsed.
   * @return a parsed context.
   */
  default Context parseJava(Source code){
    final Source     nonNullSource  = Objects.requireNonNull(code);
    final ParsedUnit unit           = parseJava(Context.createContext(nonNullSource));
    return unit.getContext();
  }


  /**
   * Parses the context for a given source file. Note that the parsing mode is set
   * to ASTParser.K_COMPILATION_UNIT.
   *
   * @param context the context to be parsed.
   * @return the parsed program unit for the context; useful for post processing.
   * @throws RuntimeException if there is a parsing error.
   */
  default ParsedUnit parseJava(Context context){
    for(ContextMatcher each : MatchMaker.generateUnitMatchers()){
      final ParsedUnit unit = each.matches(context);
      if(!unit.isEmptyUnit()){
        return unit;
      }
    }

    return ParsedUnit.empty();
  }


  /**
   * Parses the {@link Source} pointed to by the given context and using a
   * given mode (K_COMPILATION_UNIT, K_STATEMENTS, etc.).
   *
   * @param context the context pointing to the file to be parsed, typically
   *        via {@link Context#getSourceContent()} but the file handle (
   *        {@link Context#getSource()} can also be used to map to an existing
   *        editor buffer in the surrounding tool, etc)
   * @param mode the parsing mode: PARSE_COMPILATION_UNIT or PARSE_STATEMENTS
   * @return the parsed program unit for the context; useful for post processing.
   * @throws RuntimeException if there is a parsing error.
   */
  ParsedUnit parseJava(Context context, int mode);
}
