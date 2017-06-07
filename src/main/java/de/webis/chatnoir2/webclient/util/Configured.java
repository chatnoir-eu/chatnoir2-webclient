/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.util;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import org.apache.log4j.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Condition;

/**
 * Base interface for classes which depend on the application configuration
 * or on other components which require a certain configuration.
 */
public class Configured
{
    private static Configured mInstance = null;
    private static ConfigLoader.Config mConf = null;
    private static TransportClient mClient = null;

    /**
     * @return stand-alone singleton instance.
     */
    public synchronized static Configured getInstance()
    {
        if (null == mInstance) {
            mInstance = new Configured();
        }
        return mInstance;
    }

    protected Configured()
    {
    }

    /**
     * Get system configuration.
     *
     * @return loaded configuration
     */
    public ConfigLoader.Config getConf()
    {
        if (null == mConf) {
            try {
                mConf = ConfigLoader.getInstance().getConfig();
            } catch (IOException | ConfigLoader.ParseException e) {
                e.printStackTrace();
                mConf = new ConfigLoader.Config();
            }
        }

        return mConf;
    }

    /**
     * Get a connected Elasticsearch {@link org.elasticsearch.client.transport.TransportClient} instance.
     *
     * @return configured TransportClient
     */
    public TransportClient getClient()
    {
        if (null == mClient) {
            final ConfigLoader.Config cfg = getConf();
            final String clusterName = cfg.getString("cluster.cluster_name", "");
            final String[] hosts     = cfg.getStringArray("cluster.hosts");
            final int port           = cfg.getInteger("cluster.port", 9300);

            final Settings settings = Settings.builder()
                    .put("cluster.name", clusterName)
                    .put("client.transport.sniff", true)
                    .build();

            mClient = new PreBuiltTransportClient(settings);
            for (String host: hosts) {
                mClient.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(host, port)));
            }
        }
        return mClient;
    }

    /**
     * Get a configured {@link Logger} instance for this class.
     *
     * @return configured Logger
     */
    public Logger getLogger()
    {
        return Logger.getLogger(getClass());
    }

    /**
     * Get a configured {@link Logger} instance for this class with a custom category tag.
     *
     * @param tag tag to be appended to the logger category
     * @return configured Logger
     */
    public Logger getLogger(String tag)
    {
        return Logger.getLogger(getClass().getName() + "#" + tag);
    }

    /**
     * Get an instance of the global system logger.
     *
     * @return system Logger
     */
    public Logger getSysLogger()
    {
        return Logger.getLogger("de.webis.chatnoir2.webclient");
    }
}
