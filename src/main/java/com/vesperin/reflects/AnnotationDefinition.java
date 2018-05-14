package com.vesperin.reflects;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class AnnotationDefinition {
  private final String value;

  private final Map<String, Set<String>> memberValues;

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
    this(value, new HashMap<>());
  }

  private AnnotationDefinition(final String value, Map<String, Set<String>> memberValues) {
    this.value = value;
    this.memberValues = memberValues;
  }

  static AnnotationDefinition annotationDefinition(IAnnotationBinding annotationBinding){
    final StringBuilder annotation = new StringBuilder(annotationBinding.getAnnotationType().getQualifiedName());
    annotation.append("(");

    final List<String> entries = new ArrayList<>();

    final Map<String, Set<String>> memberValues = new HashMap<>();

    for(IMemberValuePairBinding each : annotationBinding.getDeclaredMemberValuePairs()){
      final String key = each.getName();
      final Object valObj = each.getValue();

      String val;
      if(valObj != null && valObj.getClass().isArray()){
        final Object[] arrayObj = (Object[]) valObj;
        val = Arrays.toString(arrayObj);

        memberValues.put(key, Arrays.asList(arrayObj).stream().map(Object::toString).collect(Collectors.toSet()));
      } else {
        val = String.valueOf(valObj);
        memberValues.put(key, new HashSet<>(Arrays.asList(val)));
      }
      final String entry = key + "=" + val;
      entries.add(entry);

    }

    annotation.append(entries.toString().replace("[", "").replace("]", ""));

    annotation.append(")");

    return new AnnotationDefinition(annotation.toString(), memberValues);
  }

  @Override public String toString() {
    return this.value;
  }

  public Map<String, Set<String>> getMemberValues() {
    return memberValues;
  }
}
