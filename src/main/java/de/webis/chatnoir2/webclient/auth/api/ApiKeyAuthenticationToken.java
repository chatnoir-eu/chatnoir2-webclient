/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth.api;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Shiro authentication token for API keys.
 */
public class ApiKeyAuthenticationToken implements AuthenticationToken
{
    private final String mApiKey;

    public ApiKeyAuthenticationToken(String apiKey)
    {
        mApiKey = apiKey;
    }

    @Override
    public Object getPrincipal()
    {
        return mApiKey;
    }

    @Override
    public Object getCredentials()
    {
        return mApiKey;
    }
}
