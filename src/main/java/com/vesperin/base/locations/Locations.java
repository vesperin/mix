package com.vesperin.base.locations;

import com.vesperin.base.Source;
import com.vesperin.base.CommonJdt;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class Locations {
  private Locations(){
    throw new Error(
        "This class cannot be instantiated."
    );
  }


  /**
   * Creates a new location for the given file and starting and ending
   * positions.
   *
   * @param code the {@link Source} object containing the positions
   * @param start the starting position
   * @param end the ending position
   * @return a new location
   */
  public static Location createLocation(
      Source code,
      Position start,
      Position end) {
    return new SourceLocation(code, start, end);
  }

  /**
   * Creates a new location for the given file and source range object.
   *
   * @param code the {@link Source} object containing the positions
   * @param range the coordinates for the source range object.
   * @return a new location
   */
  public static Location createLocation(Source code, ISourceRange range){
    return createLocation(
        code,
        code.getContent(),
        range.getOffset(),
        (range.getOffset() + range.getLength())
    );
  }


  /**
   * Creates a new location for the given file, with the given contents, for
   * the given offset range.
   *
   * @param code the {@link Source} object containing the location
   * @param contents the current contents of the file
   * @param startOffset the starting offset
   * @param endOffset the ending offset
   * @return a new location
   */
  public static Location createLocation(
      Source code,
      String contents,
      int startOffset,
      int endOffset) {

    if (startOffset < 0 || endOffset < startOffset) {
      throw new IllegalArgumentException("Invalid offsets");
    }

    if (contents == null) {
      return createLocation(
          code,
          createPosition(-1, -1, startOffset),
          createPosition(-1, -1, endOffset)
      );
    }

    int size    = contents.length();
    endOffset   = Math.min(endOffset, size);
    startOffset = Math.min(startOffset, endOffset);

    Position start = null;

    int line        = 0;
    int lineOffset  = 0;
    char prev       = 0;

    for (int offset = 0; offset <= size; offset++) {
      if (offset == startOffset) {
        start = createPosition(line, offset - lineOffset, offset);
      }

      if (offset == endOffset) {
        final Position end = createPosition(line, offset - lineOffset, offset);
        return createLocation(code, start, end);
      }

      char c = contents.charAt(offset);

      if (c == '\n') {
        lineOffset = offset + 1;
        if (prev != '\r') {
          line++;
        }
      } else if (c == '\r') {
        line++;
        lineOffset = offset + 1;
      }

      prev = c;
    }

    return createLocation(code);
  }


  /**
   * Creates a new location for the given file
   *
   * @param code the the {@link Source} object to create a location for
   * @return a new location
   */
  public static Location createLocation(Source code) {
    return new SourceLocation(
        code,
        null /*start*/,
        null /*end*/
    );
  }

  /**
   * Creates a new position given a line, column, and offset values.
   *
   * @param line the line number
   * @param column the column number on a given line
   * @param offset the offset value on a given line
   * @return a new position
   */
  public static Position createPosition(int line, int column, int offset){
    return new PositionImpl(line, column, offset);
  }

  /**
   * Returns {@code true} if one node's location (the base) cover another node's location.
   *
   * @param base The base location.
   * @param other The location of another node.
   * @return {@code true} if a code location of a node covers the location of another node.
   */
  public static boolean covers(Location base, Location other){
    final Position start = base.getStart();
    final Position end   = base.getEnd();

    final Position otherStart = other.getStart();
    final Position otherEnd   = other.getEnd();

    final int exclusiveEndOffset = end.getOffset() + 1;

    return start.getOffset() <= otherStart.getOffset()
        && (otherEnd.getOffset()) <= exclusiveEndOffset;
  }


  /**
   * Locates a ASTNode in the {@code Source}.
   *
   * @param node The ASTNode to be located.
   * @return A {@code Location} in the {@code Source} where a ASTNode is found.
   */
  public static Location locate(ASTNode node) {
    final Source src = CommonJdt.from(node);
    return Locations.locate(src, node);
  }

  /**
   * Locates a ASTNode in the {@code Source}.
   *
   * @param src The Source being inspected.
   * @param node The ASTNode to be located.
   * @return A {@code Location} in the {@code Source} where a ASTNode is found.
   */
  public static Location locate(Source src, ASTNode node){
    return createLocation(
        src,
        src.getContent(),
        node.getStartPosition(),
        node.getStartPosition() + node.getLength()
    );
  }

  /**
   * Locates a word in the {@code Source}
   *
   * @param code The {@code Source} to be inspected.
   * @param word The word to be located
   *
   * @return The location of the word in the {@code Source}
   */
  public static List<Location> locateWord(Source code, String word){
    final List<Location> locations = new ArrayList<>();

    final String  REGEX     = "\\b" + word + "\\b";
    final Pattern pattern   = Pattern.compile(REGEX);
    final Matcher matcher   = pattern.matcher(code.getContent());

    while(matcher.find()) {
      final int start = matcher.start();
      final int end   = matcher.end();

      final Location location = createLocation(
          code,
          code.getContent(),
          start,
          end
      );

      locations.add(location);
    }

    return locations;
  }

  public static boolean isBeforeBaseLocation(Location base, Location other){
    final Position otherEnd     = other.getEnd();
    final int nodeEnd           = otherEnd.getOffset();
    final Position start        = base.getStart();

    return (nodeEnd <= start.getOffset());
  }

  /**
   * Returns {@code true} if one node's location (the other) is inside
   * another node's location (base).
   *
   * @param other The location of another node.
   * @param base The base location.
   * @return {@code true} if a code location of a node is
   *    inside the location of another node.
   */
  public static boolean inside(Location other, Location base){
    if(base.same(other)) return true;

    final Position start = base.getStart();
    final Position end   = base.getEnd();

    final Position otherStart = other.getStart();
    final Position otherEnd   = other.getEnd();

//    return end.getOffset() > otherStart.getOffset()
//        && otherEnd.getOffset() > start.getOffset();

    return start.getOffset() < otherStart.getOffset()
        && (otherEnd.getOffset()) < end.getOffset();
  }

  /**
   * Returns {@code true} if a given offset value is inside a given scope.
   *
   * @param scope The location or scope of interest.
   * @param offset The offset value to test
   * @return {@code true} if a position object is inside a given scope.
   */
  public static boolean insideScope(Location scope, int offset){
    final int start = scope.getStart().getOffset();
    final int end   = scope.getEnd().getOffset();

    return start <= offset && offset < end;
  }

  /**
   * Returns {@code true} if the current location's selection intersects with another
   * location.
   *
   * @param other The location of another node.
   * @return {@code true} if <tt>this</tt> location intersects with another location.
   */
  public static boolean intersects(Location base, Location other){
    return !Locations.isBeforeBaseLocation(base, other)     // !before
      && !(covers(base, other))                       // !within
      && !Locations.isAfterBaseLocation(base, other); // !after
  }

  public static boolean isAfterBaseLocation(Location base, Location other){
    final Position otherStart   = other.getStart();
    final Position end          = base.getEnd();

    final int nodeStart     = otherStart.getOffset();
    final int exclusiveEnd  = end.getOffset() + 1;


    return (exclusiveEnd <= nodeStart);
  }


  /**
   * Returns {@code true} if one node's location (base) lies outside another node's location.
   *
   * @param base The base location.
   * @param other The location of another node.
   * @return {@code true} if a given node location lies outside another node's location.
   */
  public static boolean outside(Location base, Location other){
    final Position otherStart   = other.getStart();
    final Position otherEnd     = other.getEnd();
    final Position start        = base.getStart();
    final Position end          = base.getEnd();

    final int nodeStart = otherStart.getOffset();
    final int nodeEnd   = otherEnd.getOffset();

    final int exclusiveEnd = end.getOffset() + 1;

    final boolean nodeBeforeBase = nodeEnd < start.getOffset();
    final boolean baseBeforeNode = exclusiveEnd < nodeStart;

    return nodeBeforeBase || baseBeforeNode;
  }


  /**
   * Returns {@code true} if one node's location ends inside another node's location.
   *
   * @param base The base location.
   * @param other The location of another node.
   * @return {@code true} if a given code location ends in another code location.
   */
  public static boolean endsInside(Location base, Location other){
    final Position otherStart   = other.getStart();
    final Position otherEnd     = other.getEnd();
    final Position end          = base.getEnd();


    final int nodeStart     = otherStart.getOffset();
    final int exclusiveEnd  = end.getOffset() + 1;

    return nodeStart < exclusiveEnd
      && exclusiveEnd < otherEnd.getOffset();
  }


}
