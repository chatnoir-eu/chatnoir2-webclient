/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.resources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Load and manager API keys for the ChatNoir REST API.
 */
public class ApiKeyManager
{
    private static final String DEFAULT_FILE_NAME = "api.keys";

    private static final HashMap<String, ApiKeyManager> instances = new HashMap<>();

    private final HashMap<String, ApiKey> apiKeys = new HashMap<>();

    /**
     * Load default 'api.keys' resource file and return Singleton ApiKeyManager instance.
     *
     * @return ApiKeyManager instance
     */
    public static ApiKeyManager getInstance()
    {
        return getInstance(DEFAULT_FILE_NAME);
    }

    /**
     * Load given API keys file and return corresponding Singleton ApiKeyManager instance
     *
     * @param apiKeysFileName API keys file name
     * @return ApiKeyManager instance
     */
    public static synchronized ApiKeyManager getInstance(final String apiKeysFileName)
    {
        if (null == instances.get(apiKeysFileName)) {
            instances.put(apiKeysFileName, new ApiKeyManager(apiKeysFileName));
        }
        return instances.get(apiKeysFileName);
    }


    /**
     * Private constructor.
     */
    private ApiKeyManager(final String apiKeysFileName)
    {
        try {
            loadApiKeysFile(apiKeysFileName);
        } catch (IOException ignored) { }
    }

    /**
     * Check wheter given API key is a valid API key.
     *
     * @param apiKey API key string
     * @return true if key is valid
     */
    public boolean isApiKeyValid(final String apiKey)
    {
        final ApiKey key = apiKeys.get(apiKey);
        return (null != key) && !key.hasExpired() && !key.hasValidityInFuture();
    }

    /**
     * Get details for a given API key string.
     *
     * @param apiKey API key string
     * @return details about API key, null if key does not exist
     */
    public ApiKey getApiKeyDetails(final String apiKey)
    {
        return apiKeys.get(apiKey);
    }

    /**
     * Load API keys CVS file.
     *
     * Lines are expected to have the following format:
     *
     *     CommonName;IssueDate;(ExpiryDate|never);ApiKey
     *
     * IssueDate and ExpiryDate are of the format yyyy-MM-dd.
     * Lines that do not match the format above are ignored.
     *
     * @param apiKeysFileName the API keys file
     * @throws IOException
     */
    private void loadApiKeysFile(final String apiKeysFileName) throws IOException
    {
        final URL apiKeysFileURL = this.getClass().getClassLoader().getResource(apiKeysFileName);
        final String fullApiKeysFileName;
        if (null != apiKeysFileURL) {
            fullApiKeysFileName = apiKeysFileURL.getPath();
        } else {
            throw new IOException("Couldn't load API keys file resource");
        }
        try (final BufferedReader reader = new BufferedReader(new FileReader(fullApiKeysFileName))) {
            String line;
            while (null != (line = reader.readLine())) {
                final String[] fields = line.split(";");
                if (4 != fields.length) {
                    continue;
                }
                final String cn = fields[0];
                final Date issueDate;
                final Date expiryDate;
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    issueDate = dateFormat.parse(fields[1]);
                    if (fields[2].toLowerCase().equals("never")) {
                        expiryDate = null;
                    } else {
                        expiryDate = dateFormat.parse(fields[2]);
                    }
                } catch (ParseException ignored) {
                    continue;
                }
                final String key = fields[3];
                final ApiKey apiKey = new ApiKey(cn, issueDate, expiryDate, key);
                synchronized (apiKeys) {
                    apiKeys.put(key, apiKey);
                }
            }
        }
    }

    /**
     * API key DTO
     */
    public class ApiKey
    {
        private final String commonName;
        private final Date issueDate;
        private final Date expiryDate;
        private final String key;

        public ApiKey(final String commonName, final Date issueDate, final Date expiryDate, final String key)
        {
            this.commonName = commonName;
            this.issueDate = issueDate;
            this.expiryDate = expiryDate;
            this.key = key;
        }

        public String getCommonName()
        {
            return commonName;
        }

        public Date getIssueDate()
        {
            return issueDate;
        }

        public Date getExpiryDate()
        {
            return expiryDate;
        }

        public String getKey()
        {
            return key;
        }

        public boolean hasExpired()
        {
            return (null != expiryDate) && expiryDate.after(new Date());
        }

        public boolean hasValidityInFuture()
        {
            final Date now = new Date();
            return (null != issueDate) && !(issueDate.before(now) || issueDate.equals(now));
        }
    }
}
