package com.vesperin.utils;

import java.util.Objects;

public class Expect {
  private Expect(){}

  public static <T> T nonNull(T object){
    return Objects.requireNonNull(object);
  }

  private static void ensureNonEmptyErrorMessage(String message){
    Objects.requireNonNull(message, "Error message is null");
    if(message.isEmpty()) {
      throw new IllegalArgumentException("Error message is empty");
    }
  }

  public static void validArgument(boolean condition){
    validArgument(condition, "Invalid argument used as input.");
  }

  public static void validArgument(boolean condition, String errorMessage){
    ensureNonEmptyErrorMessage(errorMessage);
    if(!condition) throw new IllegalArgumentException(errorMessage);
  }
}
