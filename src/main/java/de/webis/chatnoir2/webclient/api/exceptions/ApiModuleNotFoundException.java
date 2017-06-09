/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.exceptions;

/**
 * Exception to be thrown when an API module could not be found.
 */
public class ApiModuleNotFoundException extends UserErrorException
{
    public ApiModuleNotFoundException(String message)
    {
        super(message);
    }
}
