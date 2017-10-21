package com.vesperin.base.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Huascar Sanchez
 */
public class Immutable {
  private Immutable() {}

  /**
   * Creates a new immutable list.
   *
   * @return an immutable list.
   */
  public static <T> List<T> list() {
    return Immutable.listOf(Collections.emptyList());
  }

  /**
   * Converts stream of objects into an immutable list.
   *
   * @param stream stream of objects
   * @param <T>    type parameter
   * @return an immutable list.
   */
  public static <T> List<T> listOf(Stream<? extends T> stream) {
    return stream == null ? list() : stream.collect(toImmutableList());
  }

  /**
   * Converts a mutable list into an immutable one.
   *
   * @param list mutable list
   * @param <T>  type parameter
   * @return an immutable list.
   */
  public static <T> List<T> listOf(Collection<? extends T> list) {
    return list == null ? list() : list.stream().collect(toImmutableList());
  }

  /**
   * Creates a new immutable list.
   *
   * @return an immutable list.
   */
  public static <T> Set<T> set() {
    return Immutable.setOf(Collections.emptyList());
  }


  /**
   * Converts a mutable set into an immutable one.
   *
   * @param set mutable set
   * @param <T>  type parameter
   * @return an immutable set.
   */
  public static <T> Set<T> setOf(Collection<? extends T> set) {
    return set == null ? set() : set.stream().collect(toImmutableSet());
  }

  /**
   * Converts stream of object into an immutable set.
   *
   * @param stream stream of objects
   * @param <T>    type parameter
   * @return an immutable list.
   */
  public static <T> Set<T> setOf(Stream<? extends T> stream) {
    return stream == null ? set() : stream.collect(toImmutableSet());
  }

  /** split a list into non-view sublists of length size **/
  public static <T> List<List<T>> split(List<T> list, final int size) {
    if(list == null || list.isEmpty()) return Immutable.list();

    final List<List<T>> parts = new ArrayList<>();
    final int N = list.size();

    for (int i = 0; i < N; i += size) {
      parts.add(Immutable.listOf(list.subList(i, Math.min(N, i + size))));
    }

    return Immutable.listOf(parts);
  }


  /**
   * Creates a collector that transforms a mutable list into an immutable one.
   *
   * @param <T> the type parameter.
   * @return a new collector object.
   */
  private static <T> Collector<T, ?, List<T>> toImmutableList() {
    return Collectors.collectingAndThen(
      Collectors.toList(),
      Collections::unmodifiableList
    );
  }

  /**
   * Creates a collector that transforms a mutable set into an immutable one.
   *
   * @param <T> the type parameter.
   * @return a new collector object.
   */
  private static <T> Collector<T, ?, Set<T>> toImmutableSet() {
    return Collectors.collectingAndThen(
      Collectors.toSet(),
      Collections::unmodifiableSet
    );
  }

  public static void main(String[] args) {
    final Set<Integer> a = new HashSet<>(Arrays.asList(1, 2, 3));
    final Set<Integer> b = new HashSet<>(Arrays.asList(2, 3, 4));

    final Set<Integer> union = Sets.union(a, b);
    union.forEach(System.out::println);

  }
}