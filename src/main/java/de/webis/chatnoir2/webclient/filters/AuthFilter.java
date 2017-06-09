/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.filters;

import org.apache.shiro.web.servlet.ShiroFilter;
import javax.servlet.annotation.WebFilter;

/**
 * Filter requests to serve static content.
 */
@WebFilter(filterName="AuthFilter", urlPatterns = AuthFilter.ROUTE)
public class AuthFilter extends ShiroFilter
{
    static final String ROUTE = "/*";
}
