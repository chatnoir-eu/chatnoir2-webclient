/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.apache.shiro.web.env.EnvironmentLoaderListener;

/**
 * Shiro authentication listener.
 */
@WebListener
public class AuthListener extends EnvironmentLoaderListener
{
    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        event.getServletContext().setInitParameter(ENVIRONMENT_CLASS_PARAM, ChatNoirEnvironment.class.getName());
        super.contextInitialized(event);
    }
}
