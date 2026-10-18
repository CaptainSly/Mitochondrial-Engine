package com.duckcraftian.upf.system.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UPFPlugin {
    String id();

    String name();

    String version();

    String[] dependencies() default {};

    String[] optionalDependencies() default {};
}