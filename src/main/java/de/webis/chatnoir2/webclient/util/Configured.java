/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.util;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Base interface for classes which depend on the application configuration
 * or on other components which require a certain configuration.
 */
public class Configured
{
    private static ConfigLoader.Config mConf = null;
    private static TransportClient mClient = null;

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
            final String clusterName = cfg.get("cluster").getString("cluster_name", "");
            final String hostName    = cfg.get("cluster").getString("host", "localhost");
            final int port           = cfg.get("cluster").getInteger("port", 9300);

            final Settings settings = Settings.builder()
                    .put("cluster.name", clusterName)
                    .put("client.transport.sniff", true)
                    .build();

            mClient = new PreBuiltTransportClient(settings).addTransportAddress(
                    new InetSocketTransportAddress(new InetSocketAddress(hostName, port)));
        }
        return mClient;
    }
}
