package com.sacco.sacco_system.modules.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    String action();   // e.g., "APPROVE_LOAN"
    String category(); // e.g., "LOANS"
}