---
title: ChatNoir Advanced API Documentation
---

# ChatNoir Advanced API Documentation
In addition to ChatNoir's [general read-only search API](/doc/api), we provide an
advanced management API. Most of these features can only be used by privileged
users. Privileges are granted by assigning specific user roles. If you need access
to these advanced features, please contact us and state your use case.

While the general syntax and API features are the same as for the read-only search API,
there is usually a functional difference between `GET` and `POST` requests when
using these advanced API features.

As it is for the search API, `apikey` is a required parameter for all API calls.


## API Key Management
The API key management endpoint can be used by privileged clients to view the stored
information for an API key and issue new keys.

### API Endpoint:
The API endpoint for the API key management module is: `/api/v1/_manage_keys`.

### Query API key user info
By sending a `GET` request to the management API module, you can retrieve the stored
user information for your API key. 

#### Required roles:
*None*

#### Allowed methods:
`GET`

#### Action:
*None*

#### Parameters:
*None*

#### Response Data:
- `apikey`: API key which this info is for
- `revoked`: whether this key has been revoked
- `expires`: expiry date of this key as ISO datetime (null for no expiration)
- `user`: stored user data for this key
    - `first_name`: user first name
    - `last_name`: user last name
    - `email`: user email address
    - `address`: user postal address
    - `zip_code`: user postal ZIP code
    - `country`: user country code
- `roles`: list of assigned user roles
- `remote_hosts`: allowed remote IP addresses for this key (empty if no restriction applies)
- `limits`: API request limits for this key
    - `day`: daily limit (-1 for unlimited)
    - `week`: weekly limit (-1 for unlimited)
    - `month`: monthly limit (-1 for unlimited)

#### Example:
##### Request:
```
GET /api/v1/_manage_keys?apikey=...
```
##### Response:
```
{
    "apikey": "...",
    "expires": "2018-11-14T12:05:37.95",
    "revoked": false,
    "user": {
        "country": "de",
        "address": "Example Address,
        "last_name": "Doe",
        "first_name": "John",
        "zip_code": 00000
        "email": "email@example.com",
    },
    "roles": [],
    "remote_hosts": [],
    "limits": {
        "week": 10000,
        "month": 70000,
        "day": 310000
    }
}
```

### Create API key
By sending a `POST` request to the `/create` action of the management module,
you can issue new API keys.

**Note:** You cannot assign request limits to the new API key that would exceed
your own limits. Similarly, you cannot assign user roles to the new key which
you don't belong to.

If you pass `null` values as request limits, the new key will inherit your
current request limits.

You can assign an optional expiry date to the key, but it cannot be further
in the future than your own key's expiry date.

#### Required roles:
- *keycreate*

#### Allowed methods:
`POST`

#### Action:
`/create`

#### Parameters:
- `user`: stored user data for this key
    - `first_name`: user first name (**required**)
    - `last_name`: user last name (**required**)
    - `email`: user email address (**required**)
    - `address`: user postal address
    - `zip_code`: user postal ZIP code
    - `country`: user country code
- `roles`: list of assigned user roles
- `limits`: API request limits for this key  (**required**)
    - `day`: daily limit (-1 for unlimited)
    - `week`: weekly limit (-1 for unlimited)
    - `month`: monthly limit (-1 for unlimited)
- `remote_hosts`: allowed remote IP addresses for this key (empty for no restriction)
- `expires`: optional expiry date of this key as ISO datetime

#### Response Data:
- `message`: human-readable status message
- `apikey`: issued API key

#### Example:
##### Request:
```
POST /api/v1/_manage_keys/create
{
    "apikey": "...",
    "user": {
        "first_name": "John",
        "last_name": "Doe",
        "address": "Example address",
        "zip_code": 00000,
        "country": "de",
        "email": "email@example.com"
    },
    "limits": {
        "day": 100,
        "week": 300,
        "month": 1000
    },
    "roles": [],
    "remote_hosts": [],
    "expires": "2020-01-01T00:00:00Z"
}
```
##### Response:
```
{
    "message": "API key created",
    "apikey": "..."
}
```
