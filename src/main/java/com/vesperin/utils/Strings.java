package com.vesperin.utils;

import java.io.File;

/**
 * @author Huascar Sanchez
 */
public class Strings {
  private static final String SINGLE_SPACE  = " ";
  private static final String BLANK_SPACE   = "";

  private Strings(){}

  public static String singleSpace(){
    return SINGLE_SPACE;
  }

  public static String blankSpace(){
    return BLANK_SPACE;
  }

  // thx to http://stackoverflow.com/questions/2559759/#2560017
  public static String splitCamelCase(String content) {
    return content.replaceAll("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])", SINGLE_SPACE);
  }

  public static String substringWithin(final String text, final String open, final String close) {
    if (text == null || open == null || close == null) {
      return BLANK_SPACE;
    }

    final String toChange = Expect.nonNull(text);

    final int start = toChange.indexOf(open);
    if (start != -1) {
      final int end = toChange.indexOf(close, start + open.length());
      if (end != -1) {
        return toChange.substring(start, end + open.length());
      }
    }

    return toChange.equals(text) ? BLANK_SPACE : toChange;
  }

  public static String fileNameWithoutExtension(File file){
    final String fileName = Expect.nonNull(file).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

}
