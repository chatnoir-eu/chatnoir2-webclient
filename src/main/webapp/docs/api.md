---
title: General Search API
---

# General Search API

ChatNoir exposes its search interface via a REST API which you can use in
your own software to query search results programmatically.

To access the REST API, an API key is required, which we issue upon request to
interested parties.

## Indices
At the moment we provide three different indices to search from:

- [ClueWeb09](https://lemurproject.org/clueweb09/) (short name: *cw09*)
- [ClueWeb12](http://lemurproject.org/clueweb12/) (short name: *cw12*)
- [CommonCrawl 11/2015](https://commoncrawl.org/2015/11/) (short name: *cc1511*)

## API Basics
The current API version is provided at `/api/v1/`. Search requests can be sent as either
`GET` or `POST` requests and parameters can be passed either via the `GET` query
string or as a JSON object in the `POST` body.

A list of values can be specified in a `GET` query string by separating values
with commas.

All requests take the **required** `apikey` parameter and the optional boolean
parameter `pretty` to format the response in a more human-readable way.

For example, the following two API requests are equivalent:

```
GET /api/v1/_search?apikey=<apikey>&query=hello%20world&index=cw09,cw12&pretty
```

```
POST /api/v1/_search
{
    "apikey": "<apikey>",
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
    "apikey": "<apikey>",
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
    "query_time" : 345,
    "total_results" : 5740000,
    "indices" : [
      "cw12",
      "cc1511"
    ]
  },
  "results" : [
    {
      "score" : 621.297,
      "uuid" : "e635baa8-7341-596a-b3cf-b33c05954361",
      "index" : "cc1511",
      "trec_id" : null,
      "target_hostname" : "www.perlmonks.org",
      "target_uri" : "http://www.perlmonks.org/index.pl?node=329174",
      "page_rank" : null,
      "spam_rank" : null,
      "title" : "<em>hello</em> <em>world</em>",
      "snippet" : "Wowjust . wow.you could make a poster out of that and sell quite a few i bet. A T-Shirt of this would rock. And it&#x27;d save me the trouble of stapling multiple posters together to wear. :) Very cool script! How mean, we beginners just figure out how to write the &quot;<em>hello</em> <em>world</em>&quot; script the",
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
- `size`: number of results per page
- `minimal`: reduce result list to `score`, `uuid`, `target_uri` and `snippet` for each
  hit (boolean flag)
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
        - `index`: index the document was retrieved from **\***
        - `trec_id`: TREC ID of the result if available (`null` otherwise) **\***
        - `target_hostname`: web host this document was crawled from **\***
        - `target_uri`: full web URI
        - `page_rank`: page rank of this document if available (`null` otherwise) **\***
        - `spam_rank`: spam rank of this document if available (`null` otherwise) **\***
        - `title`: document title with highlights **\***
        - `snippet`: document body snippet with highlights
        - `explanation`: additional scoring information if `explain` was set to `true` **\*\***

**\*** field is not returned if `minimal` is set. \
**\*\*** `explanation` is only returned if `minimal` is not set or `explain` is `true`.

### Example:
#### Request:
```
POST /api/v1/_phrases
{
    "apikey": "<apikey>",
    "query": "hello world",
    "index": ["cw12", "cc1511"],
    "size": 1,
    "pretty": true,
    "minimal": true
}
```
#### Response:
```
{
  "meta" : {
    "query_time" : 575,
    "total_results" : 267741,
    "indices" : [
      "cw12",
      "cc1511"
    ]
  },
  "results" : [
    {
      "score" : 194.76102,
      "uuid" : "caccc982-ed46-51c6-a935-1d91fefbc166",
      "target_uri" : "http://cboard.cprogramming.com/brief-history-cprogramming-com/46831-hello-world.html",
      "snippet" : "This is a discussion on <em>Hello</em> <em>World</em>! within the A Brief History of Cprogramming.com forums, part of the Community Boards category; <em>Hello</em> <em>World</em>! I thought people might find this link intresting, it&#x27;s a collection of how to say <em>Hello</em> <em>World</em> in . <em>Hello</em> <em>World</em>! I thought people might find this link"
    }
  ]
}
```

## Retrieving Full Documents
The full HTML contents of a search result can be retrieved from

```
GET /cache?uuid=$UUID&index=$INDEX&raw
```
Where `$UUID` is the document UUID returned by the search API and `$INDEX`
is the index name this document is from. No API key is required for this request.

A plain text rendering with basic HTML-subset formatting can be retrieved from
```
GET /cache?uuid=$UUID&index=$INDEX&raw&plain
```
