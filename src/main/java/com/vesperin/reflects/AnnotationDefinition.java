package com.vesperin.reflects;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  static AnnotationDefinition annotationDefinition(IAnnotationBinding annotationBinding){
    final StringBuilder annotation = new StringBuilder(annotationBinding.getAnnotationType().getQualifiedName());
    annotation.append("(");

    final List<String> entries = new ArrayList<>();

    for(IMemberValuePairBinding each : annotationBinding.getDeclaredMemberValuePairs()){
      final String key = each.getName();
      final Object valObj = each.getValue();

      String val;
      if(valObj != null && valObj.getClass().isArray()){
        final Object[] arrayObj = (Object[]) valObj;
        val = Arrays.toString(arrayObj);
      } else {
        val = String.valueOf(valObj);
      }
      final String entry = key + "=" + val;
      entries.add(entry);

    }

    annotation.append(entries.toString().replace("[", "").replace("]", ""));

    annotation.append(")");

    return new AnnotationDefinition(annotation.toString());
  }

  @Override public String toString() {
    return this.value;
  }
}
