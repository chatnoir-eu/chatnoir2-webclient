/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.WebisUUID;
import de.webis.chatnoir2.webclient.CacheServlet;
import de.webis.chatnoir2.webclient.hdfs.MapFileReader;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
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
public class DocumentRetriever extends Configured
{
    private boolean mRewriteURIs;

    public DocumentRetriever()
    {
        this(false);
    }

    /**
     * @param rewriteURIs whether to rewrite URIs in the retrieved documents.
     */
    public DocumentRetriever(final boolean rewriteURIs)
    {
        if (!MapFileReader.isInitialized()) {
            MapFileReader.init();
        }
        mRewriteURIs = rewriteURIs;
    }

    public void setRewriteURIs(final boolean rewriteURIs)
    {
        mRewriteURIs = rewriteURIs;
    }

    /**
     * Retrieve plain text rendering of a document from given Elasticsearch index.
     *
     * @param indexName index to retrieve the document from
     * @param docID Elasticsearch Flake ID of the document
     * @return plain text rendering of the requested document, null if it does not exit
     */
    public String getPlainText(final String indexName, final String docID)
    {
        final GetResponse response = getClient().prepareGet(indexName, "warcrecord", docID)
                .execute()
                .actionGet();
        if (!response.isExists()) {
            return null;
        }
        final Map<String, Object> s = response.getSource();
        return s.get(String.format("body_lang_%s", s.get("lang"))).toString();
    }

    /**
     * Retrieve document by its UUID.
     *
     * @param indexName name of the index from which to retrieve the document
     * @param docUUID document UUID inside the MapFile
     * @return retrieved document, null if no matching document exists
     */
    public Document getByUUID(final String indexName, final UUID docUUID)
    {
        final JSONObject doc = MapFileReader.getDocument(docUUID, indexName);
        if (null == doc) {
            return null;
        }
        return new Document(docUUID, indexName, doc);
    }

    /**
     * Retrieve document by its index-internal Elasticsearch document ID.
     *
     * @param indexName name of the index from which to retrieve the document
     * @param docID Elasticsearch Flake ID
     * @return retrieved document, null if no matching document exists
     */
    public Document getByIndexDocID(final String indexName, final String docID)
    {
        final GetResponse response = getClient().prepareGet(indexName, "warcrecord", docID)
                .execute()
                .actionGet();
        if (!response.isExists()) {
            return null;
        }
        String recordIDKey = "warc_record_id";
        if (indexName.contains("clueweb")) {
            recordIDKey = "warc_trec_id";
        }
        String recordID = (String) response.getSource().get(recordIDKey);
        return getByWarcID(indexName, recordID);
    }

    /**
     * Retrieve document by its WARC record ID.
     *
     * @param indexName name of the index from which to retrieve the document
     * @param warcID document WARC ID
     * @return retrieved document, null if no matching document exists
     */
    public Document getByWarcID(final String indexName, final String warcID)
    {
        try {
            String prefix = getConf().get("mapfiles").get(indexName).getString("prefix");
            final UUID uuid = WebisUUID.generateUUID(prefix, warcID);
            return getByUUID(indexName, uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Retrieve document by its URI.
     *
     * @param indexName name of the index from which to retrieve the document
     * @param uri document URI
     * @return retrieved document, null if no matching document exists
     */
    public Document getByURI(final String indexName, final String uri)
    {
        final UUID docUUID = MapFileReader.getUUIDForUrl(uri, indexName);
        if (null == docUUID) {
            return null;
        }
        return getByUUID(indexName, docUUID);
    }
    public class Document
    {
        private UUID mDocUUID;
        private String mIndexName;
        private Map<String, String> mMetadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private Map<String, String> mHttpHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private String mBody;
        private String mEncoding;

        Document(final UUID uuid, final String indexName, final JSONObject document)
        {
            mDocUUID = uuid;
            mIndexName = indexName;

            JSONObject m = (JSONObject) document.get("metadata");
            for (Object k : m.keySet()) {
                mMetadata.put((String) k, (String) m.get(k));
            }

            JSONObject p = (JSONObject) document.get("payload");
            JSONObject h = (JSONObject) p.get("headers");
            for (Object k : h.keySet()) {
                mHttpHeaders.put((String) k, (String) m.get(k));
            }

            mBody     = (String) p.get("body");
            mEncoding = (String) p.get("encoding");
            if (mEncoding.equals("base64")) {
                try {
                    mBody = new String(Base64.getDecoder().decode(mBody), "ISO-8859-1");
                } catch (UnsupportedEncodingException ignored) {}
            }
        }

        public boolean getURIsRewritten()
        {
            return mRewriteURIs;
        }

        public UUID getDocUUID()
        {
            return mDocUUID;
        }

        public String getIndexName()
        {
            return mIndexName;
        }

        public String getRecordID()
        {
            if (mIndexName.contains("clueweb")) {
                return mMetadata.get("WARC-TREC-ID");
            }
            return mMetadata.get("WARC-Record-ID");
        }

        public String getTargetURI()
        {
            return mMetadata.get("WARC-Target-URI");
        }

        public Map<String, String> getMetadata()
        {
            return mMetadata;
        }

        public Map<String, String> getHttpHeaders()
        {
            return mHttpHeaders;
        }

        public String getBody()
        {
            if (mRewriteURIs) {
                return rewriteURIs(mBody);
            }
            return mBody;
        }

        private String rewriteURIs(final String html)
        {
            org.jsoup.nodes.Document doc = Jsoup.parse(html);

            Elements anchors = doc.select("a[href]");
            for (Element a : anchors) {
                a.attr("href", rewriteURL(a.attr("href"), true));
            }

            Elements links = doc.select("link[href]");
            for (Element l : links) {
                l.attr("href", rewriteURL(l.attr("href"), false, true));
            }

            Elements images = doc.select("img[src]");
            for (Element img : images) {
                img.attr("src", rewriteURL(img.attr("src"), false, true));
            }

            Elements scripts = doc.select("script[src]");
            for (Element script : scripts) {
                script.attr("src", rewriteURL(script.attr("src"), false, true));
            }

            return doc.toString();
        }


        private String rewriteURL(String uriStr, boolean redirectOther)
        {
            return rewriteURL(uriStr, redirectOther, false);
        }

        private String rewriteURL(String uriStr, boolean redirectOther, boolean skipMapFileCheck)
        {
            try {
                URI uri = new URI(uriStr);
                String host = uri.getHost();
                String scheme = uri.getScheme();
                int port = uri.getPort();

                if (scheme != null && !scheme.equals("http") && !scheme.equals("https"))
                    return uriStr;

                URI thisURI = new URI(getTargetURI());
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
            } catch (URISyntaxException ignored) {
            }

            if (!skipMapFileCheck) {
                final UUID id = MapFileReader.getUUIDForUrl(uriStr, mIndexName);
                try {
                    if (null != id) {
                        uriStr = CacheServlet.ROUTE + "?uuid=" + id.toString() + "&i=" + URLEncoder.encode(mIndexName, "UTF-8");
                    } else if (redirectOther) {
                        // TODO: don't hardcode path
                        uriStr = "/redirect?uri=" + URLEncoder.encode(uriStr, "UTF-8");
                    }
                } catch (UnsupportedEncodingException ignored) {}
            }

            return uriStr;
        }
    }
}
