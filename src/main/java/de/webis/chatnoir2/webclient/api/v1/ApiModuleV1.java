/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.v1;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ChatNoir API module annotation v1.
 *
 * All callable API modules need to be annotated with this @interface. As a constructor parameter you
 * need to specify the API module name with which the user will call the module in the URI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ApiModuleV1
{
    /**
     * The URL patterns of the API module
     */
    String[] value() default {};
}
