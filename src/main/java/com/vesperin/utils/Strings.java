package com.vesperin.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Huascar Sanchez
 */
public class Strings {

  private Strings() {}

  // thx to http://stackoverflow.com/questions/2559759/#2560017
  public static List<String> splitCamelCase(String content) {
    return Immutable.listOf(Arrays.stream(content.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")));
  }

  public static String firstNonNullString(String text, String orElse){
    return text == null ? orElse : text;
  }

  public static String textWithin(final String text, final String open, final String close) {

    if (open == null || close == null) throw new IllegalArgumentException("invalid opening or closing symbols");
    if (text == null || text.isEmpty()) return null;

    final String toChange = Objects.requireNonNull(text);

    final int openStart = toChange.indexOf(open);
    if (openStart != -1) {
      int closeEnd = toChange.indexOf(close, openStart + open.length());

      final int farEnd = toChange.lastIndexOf(close);
      final int delta = (farEnd - closeEnd);

      if (delta >= 1) {
        closeEnd = farEnd;
      }

      if (closeEnd != -1) {
        // not inclusive
        return toChange.substring(openStart + 1, (closeEnd + open.length() - 1));
      }
    }

    return null;
  }

  public static Optional<String> getFileExtension(File file) {
    final String fileName = Optional.ofNullable(file)
        .map(File::getName)
        .orElseThrow(NullPointerException::new);

    return Optional.ofNullable(fileName)
        .filter(f -> f.contains("."))
        .map(f -> f.substring(fileName.lastIndexOf(".") + 1));
  }

  public static String fileNameWithoutExtension(File file){
    final String fileName = Optional.ofNullable(file)
        .map(File::getName)
        .orElseThrow(NullPointerException::new);

    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

}
