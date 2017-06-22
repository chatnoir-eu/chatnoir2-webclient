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

package de.webis.chatnoir2.webclient.hdfs;

import de.webis.WebisUUID;
import de.webis.chatnoir2.webclient.util.Configured;
import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.Text;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Tools for retrieving WARC records from Webis MapFiles.
 */
public class MapFileReader extends Configured
{
    private static final String DATA_OUTPUT_NAME = "data";
    private static final String URI_OUTPUT_NAME  = "uri";

    private static Configuration mHadoopConfig = new Configuration();
    private static HashMap<Path, MapFile.Reader> mMapfileReaders = new HashMap<>();

    private static MapFileReader mInstance = null;

    private MapFileReader() {}

    public static void init()
    {
        if (null != mInstance) {
            cleanUp();
        }

        mInstance = new MapFileReader();
        mHadoopConfig.set("fs.defaultFS", mInstance.getConf().getString("hdfs.defaultFS"));
    }

    public static boolean isInitialized()
    {
        return null != mInstance;
    }

    private static void cleanUp()
    {
        if (null == mInstance) {
            return;
        }

        for (Path p : mMapfileReaders.keySet()) {
            try {
                mMapfileReaders.get(p).close();
            } catch (IOException ignored) {}
        }
        mMapfileReaders.clear();
    }

    @Override
    protected void finalize() throws Throwable
    {
        cleanUp();
        super.finalize();
    }

    /**
     * Retrieve document from MapFile using its original identifier (e.g. the ClueWeb ID).
     *
     * @param origId original ID of the document (e.g. the ClueWeb ID)
     * @param index Elasticsearch index for which this MapFile provides documents
     * @return retrieved document as a JSONObject or null
     */
    public static JSONObject getDocument(final String origId, final String index)
    {
        final UUID uuid = WebisUUID.generateUUID(mInstance.getConf()
                .get("hdfs.mapfiles")
                .get(index)
                .getString("prefix"), origId);

        return getDocument(uuid, index);
    }

    /**
     * Retrieve document from MapFile using its UUID.
     *
     * @param recordUUID UUID of the document
     * @param index Elasticsearch index for which this MapFile provides documents
     * @return retrieved document as a JSONObject or null
     */
    public static JSONObject getDocument(final UUID recordUUID, final String index)
    {
        if (null == mInstance.getConf()) {
            throw new RuntimeException("MapFileReader not initialized");
        }

        final ConfigLoader.Config mapfileConfig = mInstance.getConf().get("hdfs.mapfiles").get(index);
        final int partition = getPartition(recordUUID.toString(), mapfileConfig.getInteger("partitions"));
        String inputPathStr = String.format("%s/%s-r-%05d", mapfileConfig.getString("path"),
                DATA_OUTPUT_NAME, partition);

        try {
            final Path inputPath  = new Path(inputPathStr);
            MapFile.Reader reader = mMapfileReaders.get(inputPath);
            if (null == reader) {
                    reader = new MapFile.Reader(inputPath, mHadoopConfig);
                mMapfileReaders.put(inputPath, reader);
            }

            return new JSONObject(reader.get(new Text(recordUUID.toString()), new Text()).toString());

        } catch (IOException | JSONException | NullPointerException e) {
            return null;
        }
    }

    /**
     * Retrieve document UUID for given URL.
     *
     * @param url Web URL of the document
     * @param index Elasticsearch index for which this MapFile provides documents
     * @return retrieved UUID
     */
    public static UUID getUUIDForUrl(final String url, final String index)
    {
        if (null == mInstance.getConf()) {
            throw new RuntimeException("MapFileReader not initialized");
        }

        final ConfigLoader.Config mapfileConfig = mInstance.getConf().get("hdfs.mapfiles").get(index);
        final int partition = getPartition(url, mapfileConfig.getInteger("partitions"));
        String inputPathStr = String.format("%s/%s-r-%05d", mapfileConfig.getString("path"),
                URI_OUTPUT_NAME, partition);

        try {
            final Path inputPath  = new Path(inputPathStr);
            MapFile.Reader reader = mMapfileReaders.get(inputPath);
            if (null == reader) {
                reader = new MapFile.Reader(inputPath, mHadoopConfig);
                mMapfileReaders.put(inputPath, reader);
            }
            String uuidStr = reader.get(new Text(url), new Text()).toString();
            if (uuidStr.startsWith(DATA_OUTPUT_NAME))
                uuidStr = uuidStr.substring(DATA_OUTPUT_NAME.length());
            return UUID.fromString(uuidStr);

        } catch (IOException | NullPointerException e) {
            return null;
        }
    }

    /**
     * Get MapFile partition number.
     *
     * @param key MapFile entry key
     * @param numPartitions total number of partitions
     * @return calculated partition number
     */
    private static int getPartition(final String key, final int numPartitions)
    {
        return (key.hashCode() % numPartitions + numPartitions) % numPartitions;
    }
}
