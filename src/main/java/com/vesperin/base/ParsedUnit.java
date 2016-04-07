package com.vesperin.base;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class ParsedUnit {
  private final ASTNode parsedNode;
  private final boolean partial;

  private Context context;

  /**
   * Construct a new ParsedUnit object.
   *
   * @param parsedNode the parsed ASTNode
   * @param partial partial program: it has some missing code elements.
   */
  ParsedUnit(ASTNode parsedNode, boolean partial) {
    this.parsedNode = parsedNode;
    this.partial    = partial;
    this.context    = null;
  }

  public static ParsedUnit empty() {
    return ParsedUnit.makeUnit(null, false);
  }

  public static ParsedUnit makeUnit(ASTNode parsed, boolean partial) {
    return new ParsedUnit(parsed, partial);
  }

  /**
   * Returns the parsed ASTNode.
   */
  public ASTNode getParsedNode() {
    return this.parsedNode;
  }

  public Context getContext(){
    return context;
  }

  /**
   * Returns whether the parsed node corresponds to a partial program; i.e., a block of
   * statements wrapped inside a MISSING class.
   */
  public boolean isCodeSnippet() {
    return parsedNode == null && partial;
  }

  /**
   * Returns whether the parsed code lead to an empty program unit. An empty program unit
   * is a partial program containing a null parsed AST node.
   *
   * @return true if it is an empty program unit; false otherwise.
   */
  public boolean isEmptyUnit(){
    return getParsedNode() == null && !isCodeSnippet();
  }

  public void setContext(Context context){
    this.context  = Objects.requireNonNull(context);
  }

  @Override public String toString() {
    final String nodeName = (getParsedNode() == null
        ? "Unknown"
        : getParsedNode().getClass().getSimpleName()
    );

    return "ParsedUnit("
        + "class=" + nodeName
        + ", isCodeSnippet=" + isCodeSnippet()
        + ")";
  }

}
