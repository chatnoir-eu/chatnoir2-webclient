/*
 * ChatNoir 2 Web Frontend.
 * Copyright (C) 2014-2017 Janek Bevendorff, Webis Group
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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

/**
 * Base interface for classes which depend on the application configuration
 * or on other components which require a certain configuration.
 */
public class Configured
{
    private static ConfigLoader.Config sConf = null;
    private static TransportClient sClient = null;
    private static final Object sClientMutex = new Object();

    protected Configured()
    {
    }

    /**
     * Get system configuration.
     *
     * @return loaded configuration
     */
    public static synchronized ConfigLoader.Config getConf()
    {
        if (null == sConf) {
            try {
                sConf = ConfigLoader.getInstance().getConfig();
            } catch (IOException | ConfigLoader.ParseException e) {
                e.printStackTrace();
                sConf = new ConfigLoader.Config();
            }
        }

        return sConf;
    }

    /**
     * Get a connected Elasticsearch {@link TransportClient} instance.
     *
     * @return configured TransportClient
     */
    public static TransportClient getClient()
    {
        synchronized (sClientMutex) {
            if (null == sClient) {
                final ConfigLoader.Config cfg = getConf();
                final String clusterName = cfg.getString("cluster.cluster_name", "");
                final String[] hosts = cfg.getStringArray("cluster.hosts");
                final int port = cfg.getInteger("cluster.port", 9300);

                final Settings settings = Settings.builder()
                        .put("cluster.name", clusterName)
                        .put("client.transport.sniff", cfg.getBoolean("cluster.sniff", true))
                        .build();

                sClient = new PreBuiltTransportClient(settings);
                for (String host : hosts) {
                    sClient.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(host, port)));
                }
            }
            return sClient;
        }
    }

    /**
     * Cleanly shut down and reset the Elasticsearch {@link TransportClient}.
     * The next call to {@link #getClient()} will create a new transport client.
     */
    public static void shutdownClient()
    {
        synchronized (sClientMutex) {
            if (null != sClient) {
                sClient.close();
            }
            sClient = null;
        }
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
    public static Logger getSysLogger()
    {
        return Logger.getLogger("de.webis.chatnoir2.webclient");
    }
}
