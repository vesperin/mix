package com.vesperin.base.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Sets {
  private Sets(){}


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
    final Iterator<? extends T> itr1 = a.iterator();
    final Iterator<? extends T> itr2 = b.iterator();

    while(itr1.hasNext()) { result.add(itr1.next()); }
    while(itr2.hasNext()) { T next =  itr2.next(); if(!result.contains(next)) { result.add(next); } }


    return Immutable.setOf(result);
  }
}
