package com.backend.constructor.common.validator;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Unique {
    String[] group() default {};

    String err() default "ERROR_0002";

    boolean ignoreCase() default false;
}