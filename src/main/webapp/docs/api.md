---
title: ChatNoir 2 API Documentation
---

# ChatNoir 2 API Documentation

ChatNoir 2 exposes its search interface via a REST API which you can use in
your own software to query search results programmatically.

To access the REST API, an API key is required, which we issue upon request to
interested parties.

## API Basics
The current API version is provided at `/api/v1/`. Requests can be sent as either
`GET` or `POST` requests and parameters can be passed either via the `GET` query
string or as a JSON object in the `POST` body.

All requests take the **required** `apiKey` parameter and the optional boolean
parameter `pretty` to format the response in a more human-readable way.

For example, the following two API requests are equivalent:

```
GET /api/v1/_search?apiKey=...&query=hello%20world&pretty
```

```
POST /api/v1/_search
{
    "apiKey: "...",
    "query": "hello world",
    "pretty": true
}
```

It is also possible to mix both forms. If parameters conflict, the `POST` body
parameter takes precedence.

## SimpleSearch
The default search module provides a flexible and generic search interface,
which supports the standard operators known from other web search services.

The simple module is the same module that our end-user web search services uses.

### API Endpoint:
The API endpoint for the simple search module is: `/api/v1/_search`.

### Parameters:
