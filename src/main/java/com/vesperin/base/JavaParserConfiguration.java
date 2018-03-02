package com.vesperin.base;

import com.vesperin.utils.Expect;
import com.vesperin.utils.Immutable;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class JavaParserConfiguration {
  private ASTParser     astParser;
  private List<String>  classpathEntries;
  private List<String>  sourcepathEntries;
  private List<String>  encodings;
  private boolean       resolveBindings;

  /**
   * Constructs a new Java Parser Configuration, having Java8, and
   * resolve bindings as default options. If these default options
   * must be changed, then one must call {@link #setNewParser(ASTParser)} and
   * {@link #setBindingResolution(boolean)} methods.
   */
  public JavaParserConfiguration(){
    this.astParser = ASTParser.newParser(AST.JLS8);
    this.astParser.setResolveBindings(true);
    this.classpathEntries = new ArrayList<>();
    this.sourcepathEntries = new ArrayList<>();
    this.encodings = new ArrayList<>();
    this.resolveBindings = true;
  }

  /**
   * @return a new and configured EclipseJavaParser
   */
  public JavaParser configure(){

    if (!isResolvingBindings()){
      this.getAstParser().setResolveBindings(false);
    }


    final String[] cpEntries = getClasspathEntries().isEmpty()
      ? null : getClasspathEntries().toArray(new String[getClasspathEntries().size()]);

    final String[] scEntries = getSourcepathEntries().isEmpty()
      ? null : getSourcepathEntries().toArray(new String[getSourcepathEntries().size()]);

    final String[] parserEncodings = getEncodings().isEmpty()
      ? null : getEncodings().toArray(new String[getEncodings().size()]);

    this.getAstParser().setEnvironment(
      cpEntries,
      scEntries,
      parserEncodings,
      true
    );

    return new EclipseJavaParser(this);
  }

  public ASTParser getAstParser() {
    return astParser;
  }

  public List<String> getClasspathEntries() {
    return classpathEntries;
  }

  public List<String> getSourcepathEntries() {
    return sourcepathEntries;
  }

  public List<String> getEncodings() {
    return encodings;
  }

  public boolean isResolvingBindings() {
    return resolveBindings;
  }

  public JavaParserConfiguration setClasspathEntries(List<String> entries){
    this.classpathEntries = Immutable.listOf(entries);
    return this;
  }

  public JavaParserConfiguration setEncodings(List<String> entries){
    this.encodings = Immutable.listOf(entries);
    return this;
  }

  public JavaParserConfiguration setNewParser(ASTParser astParser){
    if (!Objects.equals(this.astParser, astParser)){
      this.astParser = Expect.nonNull(astParser);
      this.astParser.setResolveBindings(true);
      this.setBindingResolution(true);
    }
    return this;
  }

  public JavaParserConfiguration setBindingResolution(boolean answer){
    this.resolveBindings = answer;
    return this;
  }

  public JavaParserConfiguration setSourcepathEntries(List<String> entries){
    this.sourcepathEntries = Immutable.listOf(entries);
    return this;
  }
}
