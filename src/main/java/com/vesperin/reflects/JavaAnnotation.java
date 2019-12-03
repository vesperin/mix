package com.vesperin.reflects;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Java Annotation
 */
public class JavaAnnotation {
  private final String value;

  private final Map<String, Set<String>> memberValues;

  /**
   * Creates a new annotation definition.
   *
   * @param value the {@link Annotation} object.
   */
  public JavaAnnotation(final Annotation value) {
    this(value.toString());
  }

  /**
   * Creates a new annotation.
   *
   * @param value the value of the {@link Annotation} object.
   */
  public JavaAnnotation(final String value) {
    this(value, new HashMap<>());
  }

  private JavaAnnotation(final String value, Map<String, Set<String>> memberValues) {
    this.value = value;
    this.memberValues = memberValues;
  }

  static JavaAnnotation annotationDefinition(IAnnotationBinding annotationBinding){
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

        memberValues.put(key, Arrays.stream(arrayObj).map(Object::toString).collect(Collectors.toSet()));
      } else {
        val = String.valueOf(valObj);
        memberValues.put(key, new HashSet<>(Collections.singletonList(val)));
      }
      final String entry = key + "=" + val;
      entries.add(entry);

    }

    annotation.append(entries.toString().replace("[", "").replace("]", ""));

    annotation.append(")");

    return new JavaAnnotation(annotation.toString(), memberValues);
  }

  @Override public String toString() {
    return this.value;
  }

  public Map<String, Set<String>> getMemberValues() {
    return memberValues;
  }
}
