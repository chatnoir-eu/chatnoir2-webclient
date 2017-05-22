/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.resources;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loader and provider singleton for configuration directives.
 */
public class ConfigLoader
{
    private static final String DEFAULT_FILE_NAME = "config.yml";

    private static final ConfigLoader mInstance = new ConfigLoader();

    private final HashMap<String, Object> mConfigObjects = new HashMap<>();

    /**
     * Return Singleton ConfigLoader instance.
     *
     * @return ConfigLoader instance
     */
    public static ConfigLoader getInstance()
    {
        return mInstance;
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
        if (null == mConfigObjects.get(configFileName)) {
            loadConfigFile(configFileName);
        }

        return new Config(mConfigObjects.get(configFileName));
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

        Path configFile = Paths.get("/etc/chatnoir2/" + configFileName);
        if (!Files.exists(configFile)) {
            configFile = Paths.get(configFileName);
        }

        if (!Files.exists(configFile)) {
            final URL configFileURL = this.getClass().getClassLoader().getResource(configFileName);
            if (null!= configFileURL) {
                configFile = Paths.get(configFileURL.getPath());
            }
        }

        if (!Files.exists(configFile)) {
            throw new IOException(String.format("Config file '%s' not found", configFileName));
        }

        try {
            byte[] fileContents = Files.readAllBytes(configFile);
            Tuple<XContentType, Map<String, Object>> xContent = XContentHelper
                    .convertToMap(new BytesArray(fileContents), false,
                            configFileName.endsWith(".json") ? XContentType.JSON : XContentType.YAML);

            mConfigObjects.put(configFileName, xContent.v2());
        } catch (Exception e) {
            throw new ParseException(e.getMessage());
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
        private final Object configObject;

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
        protected Config(Object configObject)
        {
            this.configObject = configObject;
        }

        /**
         * Check if config object contains a given key.
         * You can use dot notation to check nested configuration options.
         *
         * @param name key
         * @return true if key exists
         */
        public boolean contains(String name)
        {
            assert configObject != null;
            return ((Map) configObject).get(name) != null || get(name).configObject != null;

        }

        /**
         * Get configuration directive by name.
         * You can chain multiple calls or use dot notation for the name to
         * reference nested configuration options.
         *
         * @param name name of the configuration directive or a directive path divided by dots
         * @return the Config object
         */
        public Config get(String name)
        {
            if (null == configObject || !(configObject instanceof Map)) {
                return this;
            }

            Object obj = configObject;
            try {
                String[] splits = name.split("\\.");
                for (String s : splits) {
                    obj = ((Map) obj).get(s);
                }
            } catch (Exception e) {
                return this;
            }

            return new Config(obj);
        }

        /**
         * Get configuration directive as String.
         *
         * @param name configuration directive name
         * @return the directive as String, null if no such directive found
         */
        public String getString(final String name)
        {
            return getString(name, null);
        }

        /**
         * Get configuration directive as String or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as String or defaultValue if no such directive found
         */
        public String getString(final String name, final String defaultValue)
        {
            try {
                return get(name).configObject.toString();
            } catch (NullPointerException e) {
                return defaultValue;
            }
        }

        /**
         * Get configuration directive as Long.
         *
         * @param name configuration directive name
         * @return the directive as Long, null if no such directive found
         */
        public Long getLong(final String name)
        {
            return getLong(name, null);
        }

        /**
         * Get configuration directive as Long or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Long or defaultValue if no such directive found
         */
        public Long getLong(final String name, final Long defaultValue)
        {
            try{
                return ((Number) (get(name).configObject)).longValue();
            } catch (NullPointerException e) {
                return defaultValue;
            }
        }


        /**
         * Get configuration directive as Integer.
         *
         * @param name configuration directive name
         * @return the directive as Integer, null if no such directive found
         */
        public Integer getInteger(String name)
        {
            return getInteger(name, null);
        }

        /**
         * Get configuration directive as Integer or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Integer or defaultValue if no such directive found
         */
        public Integer getInteger(final String name, final Integer defaultValue)
        {
            return getLong(name, null != defaultValue ? defaultValue.longValue() : 0).intValue();
        }

        /**
         * Get configuration directive as Double.
         *
         * @param name configuration directive name
         * @return the directive as Double, null if no such directive found
         */
        public Double getDouble(final String name)
        {
            return getDouble(name, null);
        }

        /**
         * Get configuration directive as Double or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Double or defaultValue if no such directive found
         */
        public Double getDouble(final String name, final Double defaultValue)
        {
            if (!contains(name)) {
                return defaultValue;
            }

            try {
                return ((Number) get(name).configObject).doubleValue();
            } catch (NullPointerException e) {
                return defaultValue;
            }
        }

        /**
         * Get configuration directive as Float.
         *
         * @param name configuration directive name
         * @return the directive as Float, null if no such directive found
         */
        public Float getFloat(final String name)
        {
            return getFloat(name, null);
        }

        /**
         * Get configuration directive as Float or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Float or defaultValue if no such directive found
         */
        public Float getFloat(final String name, final Float defaultValue)
        {
            return getDouble(name, null != defaultValue ? defaultValue.doubleValue() : 0.0).floatValue();
        }

        /**
         * Get configuration directive as Boolean.
         *
         * @param name configuration directive name
         * @return the directive as Boolean, null if no such directive found
         */
        public Boolean getBoolean(final String name)
        {
            return getBoolean(name, null);
        }

        /**
         * Get configuration directive as Boolean or a given default value if directive does not exist.
         *
         * @param name configuration directive name
         * @return the directive as Boolean or defaultValue if no such directive found
         */
        public Boolean getBoolean(final String name, final Boolean defaultValue)
        {
            if (!contains(name)) {
                return defaultValue;
            }

            Object obj = get(name).configObject;
            return null != obj && obj instanceof Boolean && (Boolean) obj;
        }

        /**
         * Get configuration directive as array of sub Config objects.
         *
         * @param name configuration directive name
         * @return the directive as array (empty if no such directive found)
         */
        public Config[] getArray(final String name)
        {
            if (!contains(name)) {
                return new Config[0];
            }

            assert configObject != null;
            List list = (List) get(name).configObject;
            final int len = list.size();

            final Config[] configArr = new Config[len];
            for (int i = 0; i < len; ++i) {
                configArr[i] = new Config(list.get(i));
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
            final ArrayList<String> a = getTypedArrayList(name, String.class);
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
            final ArrayList<Long> a = getTypedArrayList(name, Long.class);
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
            final ArrayList<Double> a = getTypedArrayList(name, Double.class);
            return a.toArray(new Double[a.size()]);
        }

        /**
         * Helper method for generating typed arrays from config.
         *
         * @param name configuration directive name
         * @param type class object of type <T>
         * @param <T> array type
         * @return ArrayList of type <T> (empty if no such entry exists)
         */
        @SuppressWarnings("unchecked")
        private <T> ArrayList<T> getTypedArrayList(final String name, final Class<T> type)
        {
            if (!contains(name)) {
                return new ArrayList<>(0);
            }

            assert configObject != null;
            final ArrayList<T> typedArrayList = new ArrayList<>();
            final List cfg = (List) get(name).configObject;

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
