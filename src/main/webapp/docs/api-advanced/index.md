---
title: Advanced API
---

# Advanced API
In addition to ChatNoir's [general read-only search API](/doc/api/), we provide an
advanced management API. Most of these features can only be used by privileged
users. Privileges are granted by assigning specific user roles. If you need access
to these advanced features, please contact us and state your use case.

While the general syntax and API features are the same as for the read-only search API,
there is usually a functional difference between `GET` and `POST` requests when
using these advanced API features.

As it is for the search API, `apikey` is a required parameter for all API calls.


## Available API Endpoints
- [API Key Management](/doc/api-advanced/management/) \
    The API key management endpoint can be used by privileged clients to view or update
    stored information for an API key and issue new keys.
