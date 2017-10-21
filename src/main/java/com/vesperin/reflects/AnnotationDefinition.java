package com.vesperin.reflects;

import java.lang.annotation.Annotation;

/**
 * @author Huascar Sanchez
 */
public class AnnotationDefinition {
  private final String value;

  /**
   * Creates a new annotation definition.
   *
   * @param value the {@link Annotation} object.
   */
  public AnnotationDefinition(final Annotation value) {
    this(value.toString());
  }

  /**
   * Creates a new annotation.
   *
   * @param value the value of the {@link Annotation} object.
   */
  public AnnotationDefinition(final String value) {
    this.value = value;
  }

  @Override public String toString() {
    return this.value;
  }
}
