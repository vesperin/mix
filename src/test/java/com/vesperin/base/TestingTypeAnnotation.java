package com.vesperin.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface TestingTypeAnnotation {
    TestEnumValue[] value();

    public enum TestEnumValue {
        TOP;
    }
}

