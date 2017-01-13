/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Loader and provider singleton for configuration directives.
 */
public class ConfigLoader
{
    private static final String DEFAULT_FILE_NAME = "config.json";

    private static final ConfigLoader instance = new ConfigLoader();

    private final HashMap<String, JSONObject> configObjects = new HashMap<>();

    /**
     * Return Singleton ConfigLoader instance.
     *
     * @return ConfigLoader instance
     */
    public static ConfigLoader getInstance()
    {
        return instance;
    }


    /**
     * Private constructor, only to be called from within Singleton class.
     */
    private ConfigLoader()
    {
    }

    /**
     * Get default configuration directive as Config object.
     *
     * @return the Config object
     * @throws IOException
     * @throws ParseException
     */
    public Config getConfig() throws IOException, ParseException
    {
        return getConfig(DEFAULT_FILE_NAME);
    }

    /**
     * Get configuration directive as Config object from specified file.
     *
     * @param configFileName name of the config file to retrieve the configuration from
     * @return the Config object
     * @throws IOException
     * @throws ParseException
     */
    public synchronized Config getConfig(final String configFileName) throws IOException, ParseException
    {
        if (null == configObjects.get(configFileName)) {
            loadConfigFile(configFileName);
        }

        return new Config(configObjects.get(configFileName));
    }

    /**
     * Load configuration from JSON file.
     *
     * @param configFileName the configuration file, null for default config file
     * @throws IOException
     * @throws ParseException
     */
    private synchronized void loadConfigFile(String configFileName) throws IOException, ParseException
    {
        if (configFileName == null) {
            configFileName = DEFAULT_FILE_NAME;
        }
        final URL configFileURL = this.getClass().getClassLoader().getResource(configFileName);
        if (null == configFileURL) {
            throw new IOException(String.format("Config file '%s' not found", configFileName));
        }

        final String fullConfigFileName = configFileURL.getPath();
        try (BufferedReader br = new BufferedReader(new FileReader(fullConfigFileName))) {
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }

            try {
                configObjects.put(configFileName, new JSONObject(sb.toString()));
            } catch (JSONException e) {
                throw new ParseException(e.getMessage());
            }
        }
    }

    /**
     * Configuration data transfer object.
     */
    public static class Config
    {
        /**
         * Raw JSON config object.
         */
        private final JSONObject configObject;

        /**
         * Construct empty (placeholder) Config object.
         * May be used when loading actual config fails. Note: all getters without default values will return null!
         */
        public Config() {
            this.configObject = null;
        }

        /**
         * Constructor.
         * Initialize Config object with JSON data.
         *
         * @param configObject the raw JSON config object
         */
        protected Config(JSONObject configObject)
        {
            this.configObject = configObject;
        }

        /**
         * Get configuration directive by name.
         *
         * @param name name of the configuration directive
         * @return the Config object
         */
        public Config get(String name)
        {
            if (null == configObject || !configObject.has(name)) {
                return this;
            }

            return new Config(configObject.getJSONObject(name));
        }

        /**
         * Get configuration directive as String.
         *
         * @param name configuration directive name
         * @return the directive as String, null if no such directive found
         */
        public String getString(final String name)
        {
            if (null == configObject || !configObject.has(name)) {
                return null;
            }

            return configObject.getString(name);
        }

        /**
         * Get configuration directive as String or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as String or defaultValue if no such directive found
         */
        public String getString(final String name, final String defaultValue)
        {
            if (null == configObject || !configObject.has(name)) {
                return defaultValue;
            }

            return configObject.getString(name);
        }

        /**
         * Get configuration directive as Long.
         *
         * @param name configuration directive name
         * @return the directive as Long, null if no such directive found
         */
        public Long getLong(final String name)
        {
            if (null == configObject || !configObject.has(name)) {
                return null;
            }

            return configObject.getLong(name);
        }

        /**
         * Get configuration directive as Long or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Long or defaultValue if no such directive found
         */
        public Long getLong(final String name, final Long defaultValue)
        {
            if (null == configObject || !configObject.has(name)) {
                return defaultValue;
            }

            return configObject.getLong(name);
        }


        /**
         * Get configuration directive as Integer.
         *
         * @param name configuration directive name
         * @return the directive as Integer, null if no such directive found
         */
        public Integer getInteger(String name)
        {
            return getLong(name).intValue();
        }

        /**
         * Get configuration directive as Integer or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Integer or defaultValue if no such directive found
         */
        public Integer getInteger(final String name, final Integer defaultValue)
        {
            return getLong(name, defaultValue.longValue()).intValue();
        }

        /**
         * Get configuration directive as Double.
         *
         * @param name configuration directive name
         * @return the directive as Double, null if no such directive found
         */
        public Double getDouble(final String name)
        {
            if (null == configObject || !configObject.has(name)) {
                return null;
            }

            return configObject.getDouble(name);
        }

        /**
         * Get configuration directive as Double or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Double or defaultValue if no such directive found
         */
        public Double getDouble(final String name, final Double defaultValue)
        {
            if (null == configObject || !configObject.has(name)) {
                return defaultValue;
            }

            return configObject.getDouble(name);
        }

        /**
         * Get configuration directive as Float.
         *
         * @param name configuration directive name
         * @return the directive as Float, null if no such directive found
         */
        public Float getFloat(final String name)
        {
            return getDouble(name).floatValue();
        }

        /**
         * Get configuration directive as Float or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Float or defaultValue if no such directive found
         */
        public Float getFloat(final String name, final Float defaultValue)
        {
            return getDouble(name, defaultValue.doubleValue()).floatValue();
        }

        /**
         * Get configuration directive as Boolean.
         *
         * @param name configuration directive name
         * @return the directive as Boolean, null if no such directive found
         */
        public Boolean getBoolean(final String name)
        {
            if (null == configObject || !configObject.has(name)) {
                return null;
            }

            return configObject.getBoolean(name);
        }

        /**
         * Get configuration directive as Boolean or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Boolean or defaultValue if no such directive found
         */
        public Boolean getBoolean(final String name, final Boolean defaultValue)
        {
            if (null == configObject || !configObject.has(name)) {
                return defaultValue;
            }

            return configObject.getBoolean(name);
        }

        /**
         * Get configuration directive as array of sub Config objects.
         *
         * @param name configuration directive name
         * @return the directive as array, Config[0] if no such directive found
         */
        public Config[] getArray(final String name)
        {
            if (null == configObject || !configObject.has(name)) {
                return new Config[0];
            }

            final JSONArray jsonArr  = configObject.getJSONArray(name);
            final int len = jsonArr.length();
            final Config[] configArr = new Config[len];
            for (int i = 0; i < len; ++i) {
                configArr[i] = new Config(jsonArr.getJSONObject(i));
            }

            return configArr;
        }

        /**
         * Get configuration directive as String array.
         *
         * @param name configuration directive name
         * @return the directive as String array, String[0] if no such directive found
         */
        public String[] getStringArray(final String name)
        {
            final ArrayList<String> a = getTypeArrayList(name, String.class);
            return a.toArray(new String[a.size()]);
        }

        /**
         * Get configuration directive as Long array.
         *
         * @param name configuration directive name
         * @return the directive as Long array, Long[0] if no such directive found
         */
        public Long[] getLongArray(final String name)
        {
            final ArrayList<Long> a = getTypeArrayList(name, Long.class);
            return a.toArray(new Long[a.size()]);
        }

        /**
         * Get configuration directive as Double array.
         *
         * @param name configuration directive name
         * @return the directive as Double array, Double[0] if no such directive found
         */
        public Double[] getDoubleArray(final String name)
        {
            final ArrayList<Double> a = getTypeArrayList(name, Double.class);
            return a.toArray(new Double[a.size()]);
        }

        /**
         * Helper method for generating typed arrays from config.
         *
         * @param name configuration directive name
         * @param type class object of type <T>
         * @param <T> array type
         * @return array of type <T>
         */
        @SuppressWarnings({"unchecked"})
        private <T> ArrayList<T> getTypeArrayList(final String name, final Class<T> type)
        {
            if (null == configObject || !configObject.has(name)) {
                return new ArrayList<>(0);
            }

            final ArrayList<T> typedArrayList = new ArrayList<>();
            final JSONArray cfg = configObject.getJSONArray(name);

            for (final Object o : cfg) {
                typedArrayList.add((T) o);
            }
            return typedArrayList;
        }
    }

    /**
     * Parser Exception, abstraction wrapper for org.json.simple.parser.ParseException.
     */
    public static class ParseException extends Exception
    {
        public ParseException(final String message)
        {
            super(message);
        }
    }
}
