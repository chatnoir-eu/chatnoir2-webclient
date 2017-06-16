/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.exceptions;

/**
 * Exception to be thrown when a user exceeds their API quota.
 */
public class QuotaExceededException extends UserErrorException
{
    public QuotaExceededException(String message)
    {
        super(message);
    }
}