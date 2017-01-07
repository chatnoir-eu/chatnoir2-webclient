/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.CacheServlet;
import de.webis.chatnoir2.webclient.hdfs.MapFileReader;
import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Provider for single-document retrieval by TREC ID
 *
 * @author Janek Bevendorff
 * @version 1
 */
public class DocumentRetriever extends SearchProvider
{
    /**
     * Elasticsearch TransportClient.
     */
    private final TransportClient client;

    /**
     * The index to search.
     */
    private final String index;

    /**
     * Search response of the last search.
     */
    private GetResponse response = null;

    /**
     * Application configuration.
     */
    private final ConfigLoader.Config config;

    /**
     * Constructor.
     *
     * @param index index to retrieve document from (null means first default index from config).
     *              Indices that are not present in the config will be ignored.
     */
    public DocumentRetriever(final String index)
    {
        config = new Object() {
            public ConfigLoader.Config run()
            {
                try {
                    return ConfigLoader.getInstance().getConfig();
                } catch (IOException | ConfigLoader.ParseException e) {
                    e.printStackTrace();
                    return new ConfigLoader.Config();
                }
            }
        }.run();

        if (!MapFileReader.isInitialized())
            MapFileReader.init();

        final String clusterName = config.get("cluster").getString("cluster_name", "");
        final String hostName    = config.get("cluster").getString("host", "localhost");
        final int port           = config.get("cluster").getInteger("port", 9300);

        final List<String> allowedIndices = Arrays.asList(config.get("cluster").getStringArray("indices"));
        if (null != index && allowedIndices.contains(index)) {
            this.index = index;
        } else {
            this.index = config.get("cluster").getString("default_index", allowedIndices.get(0));
        }


        final Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
        client = new TransportClient.Builder().settings(settings).build().addTransportAddress(
                new InetSocketTransportAddress(new InetSocketAddress(hostName, port)));
    }

    public DocumentRetriever()
    {
        this(null);
    }

    /**
     * Perform a simple search.
     * Expects the following fields present:
     *      'docId' internal ID of the document
     *
     * @param searchFields key-value pairs of search fields
     */
    @Override
    public void doSearch(final HashMap<String, String> searchFields) throws InvalidSearchFieldException
    {
        if (searchFields.get("docId") == null) {
            throw new InvalidSearchFieldException();
        }

        try {
            response = client.prepareGet(index, "warcrecord", searchFields.get("docId")).
                    execute().
                    actionGet();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void finish() {
        if (null != client) {
            client.close();
        }
    }

    @Override
    public ArrayList<SearchResultBuilder.SearchResult> getResults()
    {
        final ArrayList<SearchResultBuilder.SearchResult> results = new ArrayList<>();

        if (null == response || !response.isExists()) {
            return results;
        }

        final Map<String, Object> source = response.getSource();

        final String index = response.getIndex();
        final String docId = source.get("warc_trec_id").toString();

        JSONObject doc = MapFileReader.getDocument(docId, index);
        String html;
        if (null == doc) {
            html = "HTML unavailable";
        } else {
            html = ((JSONObject) doc.get("payload")).get("body").toString();
        }

        results.add(new SearchResultBuilder().
                id(response.getId()).
                trecId(docId).
                title(source.get("title_lang_en").toString()).
                fullBody(source.get("body_lang_en").toString()).
                rawHTML(rewriteURIs(html, doc)).
                build());
        return results;
    }

    @Override
    public long getTotalResultNumber()
    {
        return 1;
    }

    /**
     * Get a List of all indices that are allowed by the config and are therefore actually used.
     *
     * @return List of index names
     */
    public String getEffectiveIndex()
    {
        return this.index;
    }

    private String rewriteURIs(final String html, JSONObject thisDocument)
    {
        Document doc = Jsoup.parse(html);

        Elements anchors = doc.select("a[href]");
        for (Element a : anchors) {
            a.attr("href", rewriteURL(a.attr("href"), thisDocument));
        }

        Elements links = doc.select("link[href]");
        for (Element l : links) {
            l.attr("href", rewriteURL(l.attr("href"), thisDocument, true));
        }

        Elements images = doc.select("img[src]");
        for (Element img : images) {
            img.attr("src", rewriteURL(img.attr("src"), thisDocument, true));
        }

        Elements scripts = doc.select("script[src]");
        for (Element script : scripts) {
            script.attr("src", rewriteURL(script.attr("src"), thisDocument, true));
        }

        return doc.toString();
    }


    private String rewriteURL(String uriStr, final JSONObject thisDocument)
    {
        return rewriteURL(uriStr, thisDocument, false);
    }

    private String rewriteURL(String uriStr, final JSONObject thisDocument, final boolean skipMapFileCheck)
    {
        try {
            URI uri = new URI(uriStr);
            String host   = uri.getHost();
            String scheme = uri.getScheme();
            int port      = uri.getPort();

            if (scheme != null && !scheme.equals("http") && !scheme.equals("https"))
                return uriStr;

            URI thisURI = new URI(( (JSONObject) thisDocument.get("metadata")).get("WARC-Target-URI").toString());
            if (null == scheme) {
                scheme = thisURI.getScheme();
            }
            if (-1 == port) {
                port = thisURI.getPort();
            }
            if (null == host) {
                host = thisURI.getHost();
            }
            // make sure we always have absolute URIs with scheme and hostname
            uriStr = new URIBuilder(uri).setScheme(scheme).setPort(port).setHost(host).build().toString();
        } catch (URISyntaxException ignored) {}

        if (!skipMapFileCheck) {
            final UUID id = MapFileReader.getUUIDForUrl(uriStr, getEffectiveIndex());
            if (null != id) {
                try {
                    uriStr = CacheServlet.ROUTE + "?uuid=" + id.toString() + "&i=" + URLEncoder.encode(this.index, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {}
            }
        }

        return uriStr;
    }
}
