/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.exceptions;

import javax.servlet.ServletException;

/**
 * Exception to indicate a user input error.
 */
public class UserErrorException extends ServletException
{
    public UserErrorException(String error)
    {
        super(error);
    }
}
