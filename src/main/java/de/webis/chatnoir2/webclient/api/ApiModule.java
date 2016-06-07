/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ChatNoir API module annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ApiModule
{
    /**
     * The URL patterns of the API module
     */
    String[] value() default {};
}
