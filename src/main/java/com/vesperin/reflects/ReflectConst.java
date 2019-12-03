package com.vesperin.reflects;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ReflectConst {

  static final String CLASS_PATH_ATTRIBUTE_SEPARATOR = " ";
  static final Predicate<Path> IS_JAR_FILE = (path -> path.toFile().getName().endsWith(".jar"));
  static final Pattern EXCLUDING_MODIFIERS = Pattern.compile(
      "\\b(?:public|protected|private|class|interface|enum|abstract|native|static|strictfp|final|synchronized) \\b"
  );
  static final int USE_LAMBDA_EXPRESSION = 0;
  static final int NOT_USE_LAMBDA_EXPRESSION = 1;
  static final String MISSING = "MISSING";
  static final String DEFAULT_NAMESPACE = "";
  static final String JAVA_LANG_NAMESPACE = "java.lang";

  private ReflectConst(){}
}
