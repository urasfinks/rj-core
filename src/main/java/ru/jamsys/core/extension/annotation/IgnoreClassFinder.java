package ru.jamsys.core.extension.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreClassFinder {
    boolean value() default true;
}