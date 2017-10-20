package com.vesperin.base;

import com.vesperin.base.utils.Expect;
import com.vesperin.base.utils.Immutable;
import com.vesperin.base.utils.SourceFormat;
import com.vesperin.base.utils.StringTemplate;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class Source {
  static final String REGEX = "class[^;=\\n]*\\s[\\S\\s]*?";

  static final String DEFAULT_NAME = "MISSING";

  private final String name;
  private final String content;


  /**
   * Construct a source object.
   *
   * @param name the name of the source file
   * @param content the file's content
   */
  Source(String name, String content) {
    this.name     = name;
    this.content  = content;
  }


  @Override public boolean equals(Object o) {
    if(!(o instanceof Source)){
      return false;
    }

    final Source that      = (Source)o;
    final boolean sameName = that.getName().equals(getName());
    final boolean sameCont = that.getContent().equals(getContent());

    return sameName && sameCont;
  }


  /**
   * Creates a Source object from some content.
   *
   * @param content the content of the source file.
   * @return a new source file containing the provided content.
   */
  public static Source from(String content) {
    return from(DEFAULT_NAME, content);
  }

  /**
   * Creates a Source using a name and some code.
   *
   * @param name the name of the source file
   * @param code the content of the source file
   * @return a new source file.
   */
  public static Source from(String name, String code) {
    return new Source(name, code);
  }


  /**
   * Creates a Source object using both a previous version of it (used as a reference) and
   * some new code.
   *
   * @param seed the reference Source.
   * @param newCode the new code.
   * @return a new source file.
   */
  public static Source from(Source seed, String newCode) {
    final Pattern pattern = Pattern.compile(REGEX);
    final Matcher matcher = pattern.matcher(newCode);

    final String tentativeName  = !matcher.find() ? pullClassname(newCode) : seed.getName();
    final String seedName       = seed.getName();

    final String name = Objects.equals(seedName, tentativeName) ? seedName : tentativeName;

    return from(name, newCode);
  }


  /**
   * Process a source file with holes (template). No Java parsing is
   * required for this operation.
   *
   * @param template Source file template
   * @param options a mapping between holes in the source and the holes' actual values.
   * @return the processed source file.
   */
  public static Source from(Source template, Map<String, String> options) {
    if(!StringTemplate.needsProcessing(template.getContent())) return template;
    if(Objects.isNull(options) || options.isEmpty()) return template;

    final String newContent = StringTemplate.process(template.getContent(), options);
    return Source.from(template, newContent);
  }

  /**
   * @return reformatted content according to {@link SourceFormat}'s specified coding style.
   */
  public Source reformat(){
    return from(getName(), SourceFormat.format(getContent()));
  }

  /**
   * @return the name of the source file.
   */
  public String getName(){
    return name;
  }

  /**
   *
   * @return the content of the source file
   */
  public String getContent() {
    return this.content;
  }

  @Override public int hashCode() {
    return Objects.hash(getName(), getContent());
  }


  static String pullClassname(String fromContent) {

    final Pattern pattern = Pattern.compile(REGEX);
    final Matcher matcher = pattern.matcher(fromContent);

    if(matcher.find()){

      final String line = fromContent.substring(matcher.start(), matcher.end());
      final List<String> chunks = Immutable.listOf(Arrays.stream(line.split(" ")).map(String::trim));
      final int targetIndex = indexOf(chunks, Pattern.compile("class").asPredicate()) + 1;

      return chunks.get(targetIndex);
    }


    throw new NoSuchElementException("Error: Name not found");
  }

  public static <T> int indexOf(Iterable<T> iterable, Predicate<? super T> predicate) {
    Expect.nonNull(iterable);
    Expect.nonNull(predicate);

    final Iterator<T> iterator = iterable.iterator();

    for (int idx = 0; iterator.hasNext(); idx++) {
      final T currentElement = iterator.next();

      if (predicate.test(currentElement)) {
        return idx;
      }
    }

    return -1; // nothing found
  }

  @Override public String toString() {
    return "Source(name=" + getName() + ", code=...)";
  }
}