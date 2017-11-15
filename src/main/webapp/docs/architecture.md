---
title: Architecture Overview
---

# Architecture Overview
ChatNoir's architecture consists of three individual components:

1. Map file generator
2. Elasticsearch indexer
3. Web frontend

### 1. Map File Generator
The map file generator parses the raw web archive (WARC) files, tries to detect the encoding and MIME type of each individual record and generates a JSON-encoded HDFS map file using Hadoop MapReduce. The map file allows random access to each document and its meta data in the collection using a specially calculated UUID key.

![ChatNoir Architecture](/static/img/architecture.png)\
*Figure 1: ChatNoir indexing flow and architecture*

### 2. Elasticsearch Indexer
As a next step, the indexer (using [Elasticsearch-Hadoop](https://www.elastic.co/products/hadoop)) uses this map file and (if available) other external data such as page rank information, as input to generate the actual [Elasticsearch](https://www.elastic.co/products/elasticsearch) index. The extracted main content of a document is indexed in multiple ways into separate fields for each language using the **BM25** retrieval model. Other meta data such as URL, host name, title, extracted meta keywords, page ranks etc. are indexed into separate fields. The original HTML body of a document is analyzed, but not stored for retrieval inside the index, as it can be retrieved from the map file at any time.

### 3. Web Frontend
Finally, the only component the user interacts with is the ChatNoir web frontend. The frontend accepts the user's search query and retrieves matching results from the Elasticsearch index. It also provides an API for programmatic REST access.

For retrieving the original HTML document for a result, the web frontend accesses the formerly created map file, rewrites references and links as needed or renders and caches a plain text version if requested. The extracted main text content from the Elasticsearch index is only used for snippet generation at this time, but may be used for additional features in the future.


# Hardware and Index Statistics
Indexing billions of web documents and providing a fast web search service for them isn't possible without some beefy hardware.

ChatNoir runs on the **145-node [Webis Betaweb Cluster](https://www.uni-weimar.de/en/media/chairs/computer-science-and-media/webis/facilities/#betaweb)** at Bauhaus-Universität Weimar (Germany), which provides a total of over **1700 CPU threads**, almost **30 TB RAM** and more than **4 PB of storage**. Thanks to this large amount of fast main memory, we can serve search requests in only a few milliseconds despite the considerable index size.

The Elasticsearch indices are distributed over **120 nodes** with **40 shards** per index and a **replica count of 2** (resulting in full allocation of one shard per data node). The remaining nodes are used as data-less manager nodes or serve other purposes.

Shards are **between 80 and 250 GB** in size. In total, we have indexed a little over **3 billion documents** with a **total size of 50 TB** (including replicas). Another **41 TB** is needed for storing the **map files**.
