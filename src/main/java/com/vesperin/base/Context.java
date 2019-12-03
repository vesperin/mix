package com.vesperin.base;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.ClassUnit;
import com.vesperin.base.locators.FieldUnit;
import com.vesperin.base.locators.MethodUnit;
import com.vesperin.base.locators.ProgramUnit;
import com.vesperin.base.locators.ProgramUnitLocator;
import com.vesperin.base.locators.SelectedUnit;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.base.locators.UnitLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;


/**
 * @author Huascar Sanchez
 */
public class Context {
  private final Source file;

  private Location          scope;
  private CompilationUnit   compilationUnit;
  private List<SyntaxIssue> syntaxProblems;
  private List<Exception>   syntaxExceptions;


  private static Set<Integer> BLACK_LIST;
  static {
    final Set<Integer> blackList = new HashSet<>();

    blackList.add(IProblem.FieldRelated);
    blackList.add(IProblem.MethodRelated);
    blackList.add(IProblem.Internal);
    blackList.add(IProblem.ConstructorRelated);
    blackList.add(IProblem.IllegalPrimitiveOrArrayTypeForEnclosingInstance);
    blackList.add(IProblem.MissingEnclosingInstanceForConstructorCall);
    blackList.add(IProblem.MissingEnclosingInstance);
    blackList.add(IProblem.IncorrectEnclosingInstanceReference);
    blackList.add(IProblem.IllegalEnclosingInstanceSpecification);
    blackList.add(IProblem.CannotDefineStaticInitializerInLocalType);
    blackList.add(IProblem.OuterLocalMustBeFinal);
    blackList.add(IProblem.CannotDefineInterfaceInLocalType);

    BLACK_LIST = Collections.unmodifiableSet(blackList);
  }

  /**
   * Construct a new {@link Context} object.
   *
   * @param file {@link Source} object.
   */
  public Context(Source file){
    this.file             = file;
    this.syntaxProblems   = new ArrayList<>();
    this.syntaxExceptions = new ArrayList<>();
  }


  /**
   * Accepts an AST visitor
   *
   * @param visitor The ASTVisitor
   */
  public void accept(ASTVisitor visitor){
    getCompilationUnit().accept(visitor);
  }


  private static List<Exception> addSyntaxErrors(CompilationUnit unit, Source code, List<SyntaxIssue> syntaxRelatedProblems) {
    final List<Exception> cachedErrors = new ArrayList<>();
    final IProblem[] problems = unit.getProblems();
    if(problems.length > 0){
      for(IProblem each : problems){
        final boolean hasSyntaxProblem  = (each.getID() & IProblem.Syntax) != 0;

        // HACK (due to OpenJDK 1.6)
        final boolean isJava15RelatedIssue = each.
            toString()
            .contains("Syntax error")
            && each.toString().contains("source level")
            && each.toString().contains("1.5");

        if(isJava15RelatedIssue) continue;

        if(each.isError() && (hasSyntaxProblem || inBlackList(each))){
          final SyntaxIssue issue = buildSyntaxIssue(each, code);
          syntaxRelatedProblems.add(issue);
          cachedErrors.add(new Exception(issue.getMessage()));
        }
      }
    }

    return cachedErrors;
  }

  private static SyntaxIssue buildSyntaxIssue(IProblem problem, Source code) {
    final int start     = problem.getSourceStart();
    final int end       = problem.getSourceEnd();
    final int line      = problem.getSourceLineNumber();
    final String msg    = problem.getMessage();
    final Location location = Locations.createLocation(code, code.getContent(), start, end);

    assert location.getStart().getLine() + 1 /*we start from line 0*/ == line /*they start from 1*/;

    return new SyntaxIssue(msg, location);

  }

  public static Context createContext(Source code){
    final Source      nonNull = Objects.requireNonNull(code);
    return new Context(nonNull);
  }

  public static Context bindContext(Context context, ParsedUnit unit){
    if (unit.isEmptyUnit()) {
      throw new IllegalStateException(
          "Error: Unable to parse source file."
      );
    }

    final ASTNode node = unit.getParsedNode();
    context.setCompilationUnit(CommonJdt.parent(CompilationUnit.class, node));
    return context;
  }

  /**
   * @return a program unit locator for this context.
   */
  public UnitLocator getUnitLocator(){
    return new ProgramUnitLocator(this);
  }

  /**
   * @return a new ScopeAnalyzer object.
   */
  public ScopeAnalyser getScopeAnalyser() {
    return new ScopeAnalyser(getCompilationUnit());
  }

  /**
   * @see UnitLocator#locate(ProgramUnit) for additional information
   */
  public List<UnitLocation> locate(ProgramUnit unit){
    return getUnitLocator().locate(unit);
  }

  /**
   * @return list of methods in the parsed source file.
   */
  public List<UnitLocation> locateMethods(){
    return getUnitLocator().locate(new MethodUnit());
  }

  /**
   * @return list of fields in the parsed source file.
   */
  public List<UnitLocation> locateFields(){
    return getUnitLocator().locate(new FieldUnit());
  }

  /**
   * @return list of classes in the parsed source file (main, inner, static nested).
   */
  public List<UnitLocation> locateClasses(){
    return getUnitLocator().locate(new ClassUnit());
  }

  /**
   * Determine what program unit is located at some source code location.
   *
   * @param start start offset
   * @param end end offset
   * @return the unit location
   */
  public List<UnitLocation> locateUnit(int start, int end){
    return locateUnit(
      Locations.createLocation(getSource(), getSourceContent(), start, end)
    );
  }

  /**
   * Determine what program unit is located at some source code location.
   *
   * @param at a source code location.
   * @return the unit location
   */
  public List<UnitLocation> locateUnit(Location at){
    return getUnitLocator().locate(new SelectedUnit(at));
  }

  /**
   * Get the compilation unit that belongs to the content of the source file's file.
   *
   * @return The source file's compilation unit
   */
  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * Get the current scope (i.e., Location)
   *
   * @return The {@code Location}
   */
  public Location getScope(){
    return Objects.requireNonNull(
        this.scope,
        "Context's scope is null"
    );
  }


  /**
   * Get the content of the source file.
   *
   * @return The source file's content.
   */
  public String getSourceContent(){
    return getSource().getContent();
  }


  /**
   * Gets a list of syntax related problems found during the compilation of
   * the {@code Source}.
   *
   * @return A list of syntax related problems or []
   */
  public List<SyntaxIssue> getSyntaxProblems(){
    return syntaxProblems;
  }

  /**
   * Gets the context's {@code Source}.
   *
   * @return The context's {@code Source}.
   */
  public Source getSource() {
    return file;
  }

  /**
   * Checks whether this context is malformed or not.
   *
   * @return {@code true} if the context is malformed; meaning that {@link #getSyntaxProblems()}
   * is non empty.
   */
  public boolean isMalformed(boolean ignoredTypeResolutionErrors){

    final List<SyntaxIssue> errors = filterSyntaxIssues(
        getSyntaxProblems(),
        ignoredTypeResolutionErrors
    );

    return !errors.isEmpty();
  }

  public static List<SyntaxIssue> filterSyntaxIssues(List<SyntaxIssue> issues, boolean ignoredTypeResolutionErrors){
    final List<SyntaxIssue> errors = new ArrayList<>(issues);
    if(ignoredTypeResolutionErrors){
      errors.removeIf(s -> s.getMessage().contains("cannot be resolved"));
    }

    return errors;
  }

  private static boolean inBlackList(IProblem each){
    for(Integer eachID : BLACK_LIST){
      if((each.getID() & eachID) != 0){
        return true;
      }
    }

    return false;
  }


  /**
   * Set the CompilationUnit that belongs to the content of the Source's file.
   *
   * @param compilationUnit The compilation unit.
   */
  public void setCompilationUnit(CompilationUnit compilationUnit) {
    if(compilationUnit == null){
      throw new IllegalArgumentException(
          "Error: CompilationUnit is null"
      );
    }

    this.compilationUnit = compilationUnit;
    this.compilationUnit.setProperty(
        CommonJdt.SOURCE_FILE_PROPERTY,
        this.getSource()
    );

    this.syntaxExceptions.addAll(
        addSyntaxErrors(
            this.compilationUnit, this.getSource(),
            this.syntaxProblems
        )
    );
  }

  /**
   * Sets the context scope (if any).
   *
   * @param scope The new scope.
   */
  public void setScope(Location scope) {
    this.scope = scope;
  }

  public static Context throwSyntaxErrorIfMalformed(Context context, boolean ignoredTypeResolutionErrors){
    if(context.isMalformed(ignoredTypeResolutionErrors)){
      throw new SyntaxException("Syntax Error", context.syntaxExceptions);
    }

    return context;
  }


  @Override public String toString() {
    return "Context(source=" + getSource() + ")";
  }


  static class SyntaxException extends RuntimeException {
    final String          title;
    final List<Exception> errors;

    protected SyntaxException(String title, List<Exception> errors){
      super();

      this.title  = title;
      this.errors = new ArrayList<>(errors);

      if(!this.errors.isEmpty()){
        this.errors.sort(new ExceptionComparator());
      }
    }

    @Override public String getMessage() {
      return createErrorMessage(this.title, this.errors);
    }

    static String createErrorMessage(String title, List<Exception> errors){
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format(title + ":%n%n");

      int index = 1;
      for(Exception each : errors){
        final String message  = each.getLocalizedMessage();
        final String line     = ("line " + message.substring(
            message.lastIndexOf("line") + 5,
            message.lastIndexOf("line") + 6
        ));

        errorFormatter
            .format("%s) Error at %s:%n", index++, line)
            .format(" %s%n%n", message);
      }

      return errorFormatter
          .format("%s error[s]", errors.size())
          .toString();
    }
  }


  static class ExceptionComparator implements Comparator<Exception> {
    @Override public int compare(Exception a, Exception b) {
      return a.getMessage().compareTo(b.getMessage());
    }
  }
}