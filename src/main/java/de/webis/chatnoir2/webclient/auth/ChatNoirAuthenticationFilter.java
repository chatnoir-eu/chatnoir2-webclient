/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Abstract base class for ChatNoir 2 authentication filters.
 */
public abstract class ChatNoirAuthenticationFilter extends AuthenticatingFilter
{
    /**
     * Authentication filter annotation.
     *
     * {@link ChatNoirAuthenticationFilter}s annotated with this interface are automatically loaded in the order
     * returned by their {@link ChatNoirAuthenticationFilter#getOrder()} members.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface AuthFilter
    {
    }

    /**
     * @return filter name
     */
    public abstract String getName();

    /**
     * @return ANT path pattern to apply this filter to
     */
    public abstract String getPathPattern();

    /**
     * @return order in which to execute the filter. The higher the name, the later the filter will be executed.
     */
    public abstract int getOrder();
}
