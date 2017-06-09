/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.exceptions;

/**
 * Exception to indicate a user input error.
 *
 * Inherit from RuntimeException, so we can bubble error messages through Shiro's
 * authentication stack.
 */
public class UserErrorException extends RuntimeException
{
    public UserErrorException(String error)
    {
        super(error);
    }
}
