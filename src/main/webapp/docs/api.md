---
title: ChatNoir 2 API Documentation
---

# ChatNoir 2 API Documentation

ChatNoir 2 exposes its search interface via a REST API which you can use in
your own software to query search results programmatically.

To access the REST API, an API key is required, which we issue upon request to
interested parties.

## Indices
At the moment we provide three different indices to search from:

- [ClueWeb09](https://lemurproject.org/clueweb09/) (short name: *cw09*)
- [ClueWeb12](http://lemurproject.org/clueweb12/) (short name: *cw12*)
- [CommonCrawl 11/2015](https://commoncrawl.org/2015/11/) (short name: *cc1511*)

## API Basics
The current API version is provided at `/api/v1/`. Requests can be sent as either
`GET` or `POST` requests and parameters can be passed either via the `GET` query
string or as a JSON object in the `POST` body.

A list of values can be specified in a `GET` query string by separating values
with commas.

All requests take the **required** `apiKey` parameter and the optional boolean
parameter `pretty` to format the response in a more human-readable way.

For example, the following two API requests are equivalent:

```
GET /api/v1/_search?apiKey=...&query=hello%20world&index=cw09,cw12&pretty
```

```
POST /api/v1/_search
{
    "apiKey: "...",
    "query": "hello world",
    "index": ["cw09", "cw12"],
    "pretty": true
}
```

It is also possible to mix both forms. If parameters conflict, the `POST` body
parameter takes precedence.

## Simple Search
The default search module provides a flexible and generic search interface,
which supports the standard operators known from other web search services.

The simple module is the same module that our end-user web search services uses.
That means you can use all operators supported by the web interface (*AND* , *OR*,
*-*, *"…"*, *site:…* etc.) also in your API query string.

### API Endpoint:
The API endpoint for the simple search module is: `/api/v1/_search`.

### Parameters:
- `query`, `q`: query string (**required**)
- `index`: list of indices to search (see above)
- `from`: result pagination begin
- `size`: number of results per page
- `explain`: return additional scoring information (boolean flag)

### Response Data:
- `meta`: global result meta information
    - `query_time`: query time in milliseconds
    - `total_results`: number of total hits
    - `indices`: list of indices that were searched
- `results`: list of search results
    - each entry has the following properties:
        - `score`: ranking score of this result
        - `uuid`: Webis UUID of this document
        - `index`: index the document was retrieved from
        - `trec_id`: TREC ID of the result if available (`null` otherwise)
        - `target_hostname`: web host this document was crawled from
        - `target_uri`: full web URI
        - `page_rank`: page rank of this document if available (`null` otherwise)
        - `spam_rank`: spam rank of this document if available (`null` otherwise)
        - `title`: document title with highlights
        - `snippet`: document body snippet with highlights
        - `explanation`: additional scoring information if `explain` was set to `true`

### Example:
#### Request:
```
POST /api/v1/_search
{
    "apiKey": "...",
    "query": "hello world",
    "index": ["cw12", "cc1511"],
    "size": 1,
    "pretty": true
}
```
#### Response:
```
{
  "meta" : {
    "query_time" : 366,
    "total_results" : 5740000,
    "indices" : [
      "cw12",
      "cc1511"
    ]
  },
  "results" : [
    {
      "score" : 592.4078,
      "uuid" : "47405d9b-4630-5b14-a0f7-cc2484a7bdda",
      "index" : "cc1511",
      "trec_id" : null,
      "target_hostname" : "www.perlmonks.org",
      "target_uri" : "http://www.perlmonks.org/index.pl?node_id=145765",
      "page_rank" : null,
      "spam_rank" : null,
      "title" : "<em>Hello</em> <em>World</em>",
      "snippet" : "Not really sure if this should be listed as an obfu or a CUFP, as it really does have qualities of both. On the one hand, it&#x27;s sorta kinda unreadable, and certainly not what you&#x27;d expect a &quot;<em>Hello</em> <em>World</em>&quot; program to be like. On the other, it demonstrates perl&#x27;s usefulness and power when dealing with",
      "explanation" : null
    }
  ]
}
```

## Phrase Search
The phrase search module can be used to retrieve snippets containing certain
fixed phrases from our indices.

### API Endpoint:
The API endpoint for the phrase search module is: `/api/v1/_phrases`.

### Parameters:
- `query`, `q`: query phrase string (**required**)
- `slop`: how far terms in a phrase may be apart (valid values: 0, 1, 2; default: 0)
- `index`: list of indices to search (see above)
- `from`: result pagination begin
- `size`: result page size
- `snippetOnly`: reduce result list to `score` and `snippet` for each hit (boolean flag)
- `explain`: return additional scoring information (boolean flag)

### Response Data:
- `meta`: global result meta information
    - `query_time`: query time in milliseconds
    - `total_results`: number of total hits
    - `indices`: list of indices that were searched
- `results`: list of search results
    - each entry has the following properties:
        - `score`: ranking score of this result
        - `uuid`: Webis UUID of this document **\***
        - `index`: index the document was retrieved from **\***
        - `trec_id`: TREC ID of the result if available (`null` otherwise) **\***
        - `target_hostname`: web host this document was crawled from **\***
        - `target_uri`: full web URI **\***
        - `page_rank`: page rank of this document if available (`null` otherwise) **\***
        - `spam_rank`: spam rank of this document if available (`null` otherwise) **\***
        - `title`: document title with highlights **\***
        - `snippet`: document body snippet with highlights
        - `explanation`: additional scoring information if `explain` was set to `true` **\*\***

**\*** fields are not returned if `snippetOnly` is set. \
**\*\*** `explanation` is only returned if `snippetOnly` is not set or `explain` is `true`.

### Example:
#### Request:
```
POST /api/v1/_phrases
{
    "apiKey": "...",
    "query": "hello world",
    "index": ["cw12", "cc1511"],
    "size": 1,
    "pretty": true
    "snippetOnly": true
}
```
#### Response:
```
{
  "meta" : {
    "query_time" : 314,
    "total_results" : 334060,
    "indices" : [
      "cw12",
      "cc1511"
    ]
  },
  "results" : [
    {
      "score" : 13.186971,
      "snippet" : "JavaScript <em>Hello</em> <em>World</em>: This is the first program in JavaScript, and you will learn how to run a simple program of JavaScript in a web browser <em>Hello</em> <em>World</em> Program in JavaScript To write and run a JavaScript program all we.;&#x2F;body&gt; &lt;&#x2F;html&gt; Output: <em>Hello</em> <em>World</em>. the JavaScript code within html"
    }
  ]
}
```