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
import de.webis.chatnoir2.webclient.util.TextCleanser;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Provider for single-document retrieval.
 *
 * @author Janek Bevendorff
 */
public class DocumentRetriever extends IndexRetrievalOperator
{
    private boolean mRewriteURIs = true;
    private boolean mCleanEncodingErrors = true;

    public DocumentRetriever()
    {
        this(false, false);
    }

    /**
     * @param rewriteURIs whether to rewrite URIs in the retrieved documents
     * @param cleanEncodingErrors whether to clean up question marks caused by encoding errors
     */
    public DocumentRetriever(final boolean rewriteURIs, boolean cleanEncodingErrors)
    {
        super(null);

        if (!MapFileReader.isInitialized()) {
            MapFileReader.init();
        }
        mRewriteURIs = rewriteURIs;
        mCleanEncodingErrors = cleanEncodingErrors;
    }

    public void setRewriteURIs(final boolean rewriteURIs)
    {
        mRewriteURIs = rewriteURIs;
    }

    public boolean getRewriteURIs()
    {
        return mRewriteURIs;
    }

    public void setCleanEncodingErrors(final boolean cleanEncodingErrors)
    {
        mCleanEncodingErrors = cleanEncodingErrors;
    }

    public boolean getCleanEncodingErrors()
    {
        return mCleanEncodingErrors;
    }

    /**
     * Retrieve plain text rendering of a document from given Elasticsearch index.
     *
     * @param indexName index to retrieve the document from
     * @param docID Elasticsearch Flake ID of the document
     * @return plain text rendering of the requested document, null if it does not exist
     */
    public String getPlainText(final String indexName, final String docID)
    {
        if (!isIndexAllowed(indexName)) {
            return null;
        }

        final GetResponse response = getClient().prepareGet(indexName, "warcrecord", docID).get();
        if (!response.isExists()) {
            return null;
        }
        final Map<String, Object> s = response.getSource();
        return s.get(String.format("body_lang.%s", s.get("lang"))).toString();
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
        if (!isIndexAllowed(indexName)) {
            return null;
        }

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
     * @param docID Elasticsearch document ID
     * @return retrieved document, null if no matching document exists
     */
    public Document getByIndexDocID(final String indexName, final String docID)
    {
        if (!isIndexAllowed(indexName)) {
            return null;
        }

        final GetResponse response = getClient().prepareGet(indexName, "warcrecord", docID).get();
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
        if (!isIndexAllowed(indexName)) {
            return null;
        }

        try {
            String prefix = getConf().get("mapfiles").get(indexName).getString("prefix");
            final UUID uuid = WebisUUID.generateUUID(prefix, warcID);
            return getByUUID(indexName, uuid);
        } catch (Exception e) {
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
        if (!isIndexAllowed(indexName)) {
            return null;
        }

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

            JSONObject m = document.getJSONObject("metadata");
            for (String k : m.keySet()) {
                mMetadata.put(k, m.getString(k));
            }

            JSONObject p = document.getJSONObject("payload");
            JSONObject h = p.getJSONObject("headers");
            for (String k : h.keySet()) {
                mHttpHeaders.put(k, h.getString(k));
            }

            mBody     = p.getString("body");
            mEncoding = p.getString("encoding");
            if (mEncoding.equals("base64")) {
                try {
                    mBody = new String(Base64.getDecoder().decode(mBody), "ISO-8859-1");
                } catch (UnsupportedEncodingException ignored) {}
            }
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
            String body = mBody;
            if (mRewriteURIs) {
                body = rewriteURIs(body);
            }
            if (mCleanEncodingErrors) {
                body = TextCleanser.cleanseEncodingErrors(body);
            }

            return body;
        }

        private String rewriteURIs(final String html)
        {
            org.jsoup.nodes.Document doc = Jsoup.parse(html);

            // save attribute values, delete the attributes and add them again
            // to circumvent problems with uppercase attribute names
            // see https://github.com/jhy/jsoup/issues/815
            String attrValue;
            Elements elements = doc.select("a[href], area[href]");
            for (Element e : elements) {
                attrValue = rewriteURL(e.attr("href"), true);
                e.removeAttr("href");
                e.attr("href", attrValue);
            }

            elements = doc.select("link[href]");
            for (Element e : elements) {
                attrValue = rewriteURL(e.attr("href"), false);
                e.removeAttr("href");
                e.attr("href", attrValue);
            }

            elements = doc.select("img[src], script[src], iframe[src], video[src], audio[src]");
            for (Element e : elements) {
                attrValue = rewriteURL(e.attr("src"), false);
                e.removeAttr("src");
                e.attr("src", attrValue);
            }

            elements = doc.select("object[data]");
            for (Element e : elements) {
                attrValue = rewriteURL(e.attr("data"), false);
                e.removeAttr("data");
                e.attr("data", attrValue);
            }

            // remove base tags
            elements = doc.select("head base");
            for (Element e: elements) {
                e.remove();
            }

            return doc.toString();
        }

        private String rewriteURL(String uriStr, boolean addRedirect)
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
                Path p1 = Paths.get("/", thisURI.getPath());
                Path p2 = Paths.get(uri.getPath());

                uriStr = new URIBuilder(uri)
                        .setScheme(scheme)
                        .setPort(port)
                        .setHost(host)
                        .setPath(p1.resolveSibling(p2).normalize().toString())
                        .build()
                        .toString();
            } catch (URISyntaxException ignored) {}

            if (addRedirect) {
                try {
                    uriStr = CacheServlet.ROUTE + "?uri=" + URLEncoder.encode(uriStr, "UTF-8")
                            + "&index=" + URLEncoder.encode(mIndexName, "UTF-8") + "&raw";
                } catch (UnsupportedEncodingException ignored) {
                }
            }

            return uriStr;
        }
    }
}
