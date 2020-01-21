package com.vesperin.utils;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Optionals {
  private Optionals() {
  }

  public static <T, R extends Optional<T>> Stream<T> optionalToStream(R optional) {
    return optional.map(Stream::of).orElseGet(Stream::empty);
  }

  public static <T> Stream<T> supplyStream(Supplier<Stream<T>> supplier) {
    // generate once and flat
    return Stream.generate(supplier).limit(1).flatMap(Function.identity());
  }
}
