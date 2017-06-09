/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.exceptions;

/**
 * Exception to be thrown when trying to instantiate an invalid API version.
 */
public class InvalidApiVersionException extends UserErrorException
{
    public InvalidApiVersionException(String message)
    {
        super(message);
    }
}
