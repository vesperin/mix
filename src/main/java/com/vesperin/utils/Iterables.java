package com.vesperin.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Huascar Sanchez
 */
public class Iterables {
  private Iterables(){}

  /**
   * Joins two iterable objects containing objects of similar type.
   */
  public static <T> Stream<T> mergeSorted(Iterable<T> a, Iterable<T> b){
    return Stream.of(a, b).flatMap(Iterables::toStream).sorted();
  }

  /**
   * Joins two iterable objects containing objects of similar type.
   */
  public static <T> Stream<T> merge(Iterable<T> a, Iterable<T> b){
    return Stream.of(a, b).flatMap(Iterables::toStream);
  }


  /**
   * Returns an iterable object as a stream object.
   * thx to stackoverflow.com/questions/23932061/convert-iterable-to-stream-using-java-8-jdk
   */
  public static <T> Stream<T> toStream(Iterable<T> iterable){
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  /**
   * Gets an element stored at a given position
   * in some non empty iterable.
   *
   * @param iterable the iterable object
   * @param position the index
   * @param <R> the type of element in the iterable
   * @return the object (of type R) in the iterable
   */
  public static <R> R get(Iterable<R> iterable, int position){
    final Iterator<R> iterator = iterable.iterator();

    for (int skipped = 0; skipped < position && iterator.hasNext(); skipped++) {
      iterator.next();
    }

    if (!iterator.hasNext()) {
      throw new IndexOutOfBoundsException("position must be less than the number of elements in the iterable");
    }

    return iterator.next();
  }

  public static <T> int indexOf(Iterable<T> iterable, Predicate<? super T> predicate) {
    final Iterable<T> nonNullIterable = Objects.requireNonNull(iterable);
    final Predicate<? super T> nonNullPredicate = Objects.requireNonNull(predicate);

    final Iterator<T> iterator = nonNullIterable.iterator();

    for (int idx = 0; iterator.hasNext(); idx++) {
      final T currentElement = iterator.next();

      if (nonNullPredicate.test(currentElement)) {
        return idx;
      }
    }

    return -1; // nothing found
  }

  public static <R> List<Integer> indexList(Iterable<R> iterable, Predicate<? super R> predicate){
    final Iterable<R> nonNullIterable = Objects.requireNonNull(iterable);
    final Predicate<? super R> nonNullPredicate = Objects.requireNonNull(predicate);

    final List<Integer> indices = new ArrayList<>();
    final Iterator<R> iterator = nonNullIterable.iterator();

    for (int idx = 0; iterator.hasNext(); idx++) {
      final R currentElement = iterator.next();

      if (nonNullPredicate.test(currentElement)) {
        indices.add(idx);
      }
    }

    return Immutable.listOf(indices);
  }
}