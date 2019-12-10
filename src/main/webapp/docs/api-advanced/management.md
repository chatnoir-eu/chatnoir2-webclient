---
title: Key Management API
breadcrumbs: ["Advanced API Documentation"]
---

# Key Management API
The API key management endpoint is: `/api/v1/_manage_keys`
 
This endpoint can be used by privileged clients to view or update stored
information for an API key and issue new keys.

## Query API key user info
By sending a `GET` request to the management API module, you can retrieve the stored
user information for your API key. 

### Required roles:
*None*

### Allowed methods:
`GET`

### Action:
*None*

### Parameters:
*None*

### Response Data:
- `apikey`: API key which this info is for
- `revoked`: whether this key has been revoked
- `expires`: expiry date of this key as ISO datetime (null for no expiration)
- `user`: stored user data for this key
    - `common_name`: user's full name
    - `organization`: user's organization
    - `email`: user's email address
    - `address`: user's postal address
    - `zip_code`: user's ZIP code
    - `state`: user's state or province
    - `country`: user's country code
- `roles`: list of assigned user roles
- `remote_hosts`: allowed remote IP addresses for this key (empty if no restriction applies)
- `limits`: API request limits for this key
    - `day`: daily limit (-1 for unlimited)
    - `week`: weekly limit (-1 for unlimited)
    - `month`: monthly limit (-1 for unlimited)

### Example:
#### Request:
```
GET /api/v1/_manage_keys?apikey=<apikey>
```
#### Response:
```
{
    "apikey": "<apikey>",
    "expires": "2018-11-14T12:05:37.95",
    "revoked": false,
    "user": {
        "country": "DE",
        "state": "TH",
        "address": "Example Address,
        "organization": "Example Organization",
        "common_name": "John Doe",
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

## Issue a new API key
By sending a `POST` request to the `/create` action of the management module,
you can issue a new API key.

**Note:** You cannot assign request limits to the new API key that would exceed
your own limits. Similarly, you cannot assign user roles to the new key which
you don't belong to.

If you pass `null` values as request limits, the new key will inherit your
current request limits.

You can assign an optional expiry date to the key, but it cannot be further
in the future than your own key's expiry date.

### Required roles:
- *keycreate*

### Allowed methods:
`POST`

### Action:
`/create`

### Parameters:
- `user`: stored user data for this key
    - `common_name`: user's full name (**required**)
    - `email`: user's email address (**required**)
    - `organization`: user's organization
    - `address`: user's postal address
    - `zip_code`: user's ZIP code
    - `state`: user's state or province
    - `country`: user's country code
- `roles`: list of assigned user roles
- `limits`: API request limits for this key  (**required**)
    - `day`: daily limit (-1 for unlimited)
    - `week`: weekly limit (-1 for unlimited)
    - `month`: monthly limit (-1 for unlimited)
- `remote_hosts`: allowed remote IP addresses for this key (empty for no restriction)
- `expires`: optional expiry date of this key as ISO datetime

### Response Data:
- `message`: human-readable status message
- `apikey`: issued API key

### Example:
#### Request:
```
POST /api/v1/_manage_keys/create
{
    "apikey": "<apikey>",
    "user": {
        "common_name": "John Doe",
        "email": "email@example.com",
        "organization": "Example Organization",
        "address": "Example Address,
        "zip_code": 00000
        "state": "TH",
        "country": "DE",
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
#### Response:
```
{
    "message": "API key created",
    "apikey": "<new-apikey>"
}
```

## Update API key user information
By sending a `PUT` request to the `/update` action of the management module,
you can update an existing API key.

**Note:** You cannot update API keys you haven't issued yourself (i.e., which aren't
children or grand children of your API key). The same restrictions apply as for
creating new API keys.

It may take several minutes for the changes to go live.

### Required roles:
- *keycreate*

### Allowed methods:
`PUT`

### Action:
`/update/<target-apikey>`

### Parameters:
- `user`: stored user data for this key
    - `common_name`: user's full name (**required**)
    - `email`: user's email address (**required**)
    - `organization`: user's organization
    - `address`: user's postal address
    - `zip_code`: user's ZIP code
    - `state`: user's state or province
    - `country`: user's country code
- `roles`: list of assigned user roles
- `limits`: API request limits for this key  (**required**)
    - `day`: daily limit (-1 for unlimited)
    - `week`: weekly limit (-1 for unlimited)
    - `month`: monthly limit (-1 for unlimited)
- `remote_hosts`: allowed remote IP addresses for this key (empty for no restriction)
- `expires`: optional expiry date of this key as ISO datetime

### Response Data:
- `message`: human-readable status message
- `apikey`: updated API key

### Example:
#### Request:
```
PUT /api/v1/_manage_keys/update/<target-apikey>
{
    "apikey": "<apikey>",
    "user": {
        "common_name": "Jane Doe",
        "email": "email@example.com",
        "organization": "Example Organization",
        "address": "Example Address,
        "zip_code": 00000
        "state": "WA",
        "country": "US",
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
#### Response:
```
{
    "message": "API key updated",
    "apikey": "<target-apikey>"
}
```

## Revoke an API key
By sending a `PUT` request to the `/revoke` action of the management module,
you can revoke an API key. Revoking a key will also revoke all its child keys.

**Note:** You cannot revoke API keys you haven't issued yourself (i.e., which aren't
children or grand children of your API key).

It may take several minutes for the changes to go live. Revoked API keys may be
deleted after some time.

### Required roles:
- *keycreate*

### Allowed methods:
`PUT`

### Action:
`/revoke/<target-apikey>`

### Parameters:
*None*

### Response Data:
- `message`: human-readable status message
- `apikey`: revoked API key

### Example:
#### Request:
```
PUT /api/v1/_manage_keys/revoke/<target-apikey>
{
    "apikey": "<apikey>"
}
```
#### Response:
```
{
    "message": "API key revoked",
    "apikey": "<target-apikey>"
}
```
