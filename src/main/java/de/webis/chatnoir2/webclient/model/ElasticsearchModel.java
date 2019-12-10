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

package de.webis.chatnoir2.webclient.model;

import de.webis.chatnoir2.webclient.util.Configured;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Model base class which uses Elasticsearch as storage backend.
 */
public abstract class ElasticsearchModel extends ValidatingModel<String, Object>
{
    private static boolean sExistenceChecked = false;

    /**
     * Index name.
     */
    protected final String mIndexName;

    /**
     * Index mapping type.
     */
    protected final String mType;

    /**
     * Elasticsearch document ID.
     */
    protected String mDocumentId;

    /**
     * @param indexName Elasticsearch index name
     * @param type Elasticsearch mapping type
     */
    public ElasticsearchModel(String indexName, String type, String mappingFile)
    {
        mIndexName  = indexName;
        mType       = type;
        mDocumentId = null;
        ensureIndexCreated(mappingFile);
    }

    /**
     * Load model data by Elasticsearch document ID.
     *
     * @param documentId Elasticsearch document ID
     * @return true if record exists and was loaded successfully, otherwise false
     */
    public boolean loadById(String documentId)
    {
        GetResponse response = Configured.getClient().prepareGet(mIndexName, mType, documentId).get();
        if (!response.isExists()) {
            return false;
        }

        setId(response.getId());
        putAll(response.getSource());

        return true;
    }

    @Override
    public Map<String, Object> getAll()
    {
        final Map<String, Object> data = super.getAll();
        final Map<String, Object> stringData = new HashMap<>();
        for (String k : data.keySet()) {
            // Explicitly convert everything to a HashMap to avoid serialization issues with Netty
            if (data.get(k) instanceof ElasticsearchModel) {
                stringData.putAll(((ElasticsearchModel) data.get(k)).getAll());
            } else {
                stringData.put(k, data.get(k));
            }
        }
        return stringData;
    }

    @Override
    public String getId()
    {
        return mDocumentId;
    }

    /**
     * Set a new record ID.
     *
     * @param id new record ID
     */
    public void setId(String id)
    {
        mDocumentId = id;
    }

    @Override
    protected boolean doCommit()
    {
        IndexResponse response = Configured
                .getClient()
                .prepareIndex(mIndexName, mType, mDocumentId)
                .setSource(getAll())
                .get();

        if (null == mDocumentId) {
            setId(response.getId());
        }

        RestStatus status = response.status();
        return status == RestStatus.OK || status == RestStatus.CREATED;
    }

    /**
     * Ensure that the backing index exists.
     * The actual check is only performed once and then cached statically in memory.
     *
     * @param mappingFile path to file containing Elasticsearch index mapping template for creating a new index
     */
    private synchronized void ensureIndexCreated(String mappingFile)
    {
        if (sExistenceChecked) {
            return;
        }

        IndicesExistsRequest request = new IndicesExistsRequest(mIndexName);
        try {
            TransportClient client = Configured.getClient();
            IndicesExistsResponse response = client.admin().indices().exists(request).get();
            if (!response.isExists()) {
                Configured.getSysLogger().info(String.format(
                        "Index '%s' does not exist, creating it.", mIndexName));

                URL mappingFileURL = getClass().getClassLoader().getResource(mappingFile);
                assert mappingFileURL != null;
                Path mappingFilePath = Paths.get(mappingFileURL.toURI());
                final String mapping = Files.lines(mappingFilePath).reduce("", (a, b) -> a + b + "\n");
                client
                        .admin()
                        .indices()
                        .prepareCreate(mIndexName)
                        .setSource(mapping, XContentType.JSON)
                        .get();

                onAfterCreate();
            }
        } catch (Exception e) {
            Configured.getSysLogger().error("Error creating API key index", e);
            return;
        }

        sExistenceChecked = true;
    }

    /**
     * Called after a fresh new index has been created.
     * Can be overridden by sub classes to perform some setup actions on the new index.
     */
    protected void onAfterCreate()
    {
        // default implementation does nothing
    }
}
