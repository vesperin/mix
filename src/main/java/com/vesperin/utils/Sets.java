package com.vesperin.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Set operations
 */
public class Sets {
  private Sets(){}

  /**
   * Computes the Jaccard similarity between two sets where 0 means totally
   * dissimilar (nothing in common), 1.0 totally similar, and anything in
   * between 0 and 1 somewhat similar.
   *
   * Warning: elements in these sets are expected to be
   * {@link java.lang.Comparable comparable} objects.
   *
   * @param a a first set of {@link java.lang.Comparable} instances
   * @param b a second set of {@link java.lang.Comparable} instances
   * @param <T> type param
   * @return a value between 0 and 1 indicating the similarity of the
   * collections passed in.
   */
  public static <T> double similarityCoefficient(final Set<T> a, final Set<T> b){
    final Set<T> union         = Sets.union(a, b);
    final Set<T> intersection  = Sets.intersection(a, b);

    final double unionSize = 1.0 * union.size();
    final double intersectSize = 1.0 * intersection.size();

    return (intersectSize/unionSize);
  }

  /**
   * Returns a sub set of two set objects. This sub set
   * contains elements shared by both set objects.
   *
   * @param a first set
   * @param b second set
   * @param <T> type of elements in first set
   *
   * @return an intersected set of elements
   */
  public static <T> Set<T> intersection(final Set<T> a, final Set<?> b){
    final Iterator<? extends T> itr = a.iterator();

    final Set<T> result = new HashSet<>();
    while (itr.hasNext()) {
      T e = itr.next();
      if (b.contains(e)) {
        result.add(e);
      }
    }

    return Immutable.setOf(result);
  }

  /**
   * Returns the union of two set objects. This set
   * contains all elements in both set objects.
   *
   * @param a first set
   * @param b second set
   * @param <T> type of elements in first set
   *
   * @return a joined set of elements
   */
  public static <T> Set<T> union(Set<? extends T> a, Set<? extends T> b){

    final Set<T> result = new HashSet<>();
    final Iterator<? extends T> x = a.iterator();
    final Iterator<? extends T> y = b.iterator();

    while(x.hasNext()) { result.add(x.next());                  }
    while(y.hasNext()) { T next =  y.next(); result.add(next);  }

    return Immutable.setOf(result);
  }
}
