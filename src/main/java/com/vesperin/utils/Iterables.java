package com.vesperin.utils;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author Huascar Sanchez
 */
public class Iterables {
  private Iterables(){}

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
    int skipped;

    for (skipped = 0; skipped < position && iterator.hasNext(); skipped++) {
      iterator.next();
    }

    Expect.validArgument(
      iterator.hasNext(),
      "position must be less than the number of elements in the sequence."
    );

    return iterator.next();
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
}