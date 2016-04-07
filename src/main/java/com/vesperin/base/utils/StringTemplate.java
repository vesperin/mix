package com.vesperin.base.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class StringTemplate {
  static final String   FIELD_START  = "\\$\\{";
  static final String   FIELD_END    = "\\}";
  static final String   REGEX        = FIELD_START + "([^}]+)" + FIELD_END;
  static final Pattern  PATTERN      = Pattern.compile(REGEX);

  private StringTemplate(){
    throw new Error("Cannot be instantiated");
  }

  /**
   * String replacement in java, similar to a velocity template.
   *
   * @param raw the original string (before processing)
   * @param objects a mapping between holes and actual values.
   * @return the processed string.
   */
  public static String process(String raw, Map<String, String> objects){
    final Matcher matcher = PATTERN.matcher(raw);

    String result = raw;
    while (matcher.find()) {
      String[] found  = matcher.group(1).split("\\.");
      String   object = objects.get(found[0]);
      result = result.replaceFirst(REGEX, object);
    }

    return result;
  }

  /**
   * Checks if this text needs processing.
   * @param raw the text to be checked.
   * @return true if the text needs processing; false otherwise.
   */
  public static boolean needsProcessing(String raw){
    final Matcher matcher = PATTERN.matcher(raw);
    return matcher.find();
  }

}