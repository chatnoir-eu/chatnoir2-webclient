/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.v1;

import com.sun.org.apache.xpath.internal.operations.Bool;
import de.webis.chatnoir2.webclient.ChatNoirServlet;
import de.webis.chatnoir2.webclient.resources.ApiKeyManager;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for ChatNoir REST API modules.
 */
public abstract class ApiModuleBase extends ChatNoirServlet
{
    private HttpServletRequest mLastRequest = null;
    private JSONObject mParsedPayload = null;

    /**
     * Check if request is a valid request or write an error response if not.
     * Overriding methods should always call the parent method before running custom checks.
     * Servlets should not continue if this method returns false.
     *
     * @param request HTTP request
     * @return true if request is valid
     */
    public boolean validateRequest(final HttpServletRequest request, final HttpServletResponse response)
    {
        final String keyParameter = getTypedNestedParameter(String.class, "apiKey", request);
        if (null != keyParameter && ApiKeyManager.getInstance().isApiKeyValid(keyParameter)) {
            return true;
        } else {
            final JSONObject errorResponse = generateErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API key");
            try {
                writeResponse(response, errorResponse, HttpServletResponse.SC_UNAUTHORIZED);
            } catch (IOException ignored) { }
            return false;
        }
    }

    /**
     * Generate a JSON response object to indicate an error.
     *
     * @param errorCode API/HTTP error code
     * @param errorMessage error description
     * @return generated JSONObject
     */
    @SuppressWarnings("unchecked")
    public final JSONObject generateErrorResponse(final int errorCode, final String errorMessage)
    {
        final JSONObject responseObj = new JSONObject();
        final JSONObject errorObj = new JSONObject();
        errorObj.put("code", errorCode);
        errorObj.put("msg", errorMessage);
        responseObj.put("error", errorObj);
        return responseObj;
    }

    /**
     * Write an error response for rejecting the used HTTP request method.
     *
     * @param response HTTP response object
     */
    public final void rejectMethod(final HttpServletResponse response)
    {
        final JSONObject errorObj = generateErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Unsupported request method");
        try {
            writeResponse(response, errorObj, HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException ignored) { }
    }

    /**
     * Write API output to HTTP response with default status code 200.
     *
     * @param response HTTP response object
     * @param responseObject JSON answer
     * @throws IOException
     */
    public void writeResponse(final HttpServletResponse response, final JSONObject responseObject) throws IOException
    {
        writeResponse(response, responseObject, HttpServletResponse.SC_OK);
    }

    /**
     * Write API output to HTTP response with default status code 200.
     *
     * @param response HTTP response object
     * @param responseObject JSON answer
     * @param responseCode HTTP response status code
     * @throws IOException
     */
    public void writeResponse(final HttpServletResponse response, final JSONObject responseObject, final int responseCode) throws IOException
    {
        response.setStatus(responseCode);
        response.setContentType("application/json");
        response.getWriter().write(responseObject.toString());
        response.getWriter().flush();
    }

    /**
     * Handle GET request to API endpoint.
     * If GET is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    @Override
    public abstract void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Handle POST request to API endpoint.
     * If POST is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    @Override
    public abstract void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Return and parse POST data payload.
     *
     * @param request HTTP request
     * @return parsed JSON payload
     * @throws IOException if failed to parse payload
     */
    protected JSONObject getPayload(HttpServletRequest request) throws IOException {
        if (mParsedPayload != null && request == mLastRequest) {
            return mParsedPayload;
        }

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse request payload");
        }

        try {
            JSONObject json = new JSONObject(sb.toString());
            mParsedPayload = json;
            mLastRequest = request;
            return json;
        } catch (JSONException e) {
            throw new IOException("Invalid JSON specified. Message: " + e.getMessage());
        }
    }

    /**
     * Get typed parameter value from URI string or POST body (if available).
     * If both are available, POST body takes precedence.
     * The parameter name can specify a dot-separated object path.
     * If the type parameter is a JSONArray, but the actual value is a string,
     * it will be split on commas.
     *
     * Valid types are JSONObject, JSONArray, String, Integer, Double, Float, Boolean.
     *
     * @param name parameter name
     * @return parameter value or null if parameter does not exist or is of the wrong type
     */
    @SuppressWarnings("unchecked")
    protected <T> T getTypedNestedParameter(Class<T> type, String name, final HttpServletRequest request) {
        try {
            JSONObject body = getPayload(request);
            String[] nameSplit = name.split("\\.");
            for (int i = 0; i < nameSplit.length; ++i) {
                if (!body.has(nameSplit[i])) {
                    break;
                }

                if (i == nameSplit.length - 1) {
                    if (type.isInstance(body.get(nameSplit[i]))) {
                        // return if parameter already has correct type
                        return (T) body.get(nameSplit[i]);
                    } else if (type.getSuperclass() == Number.class && body.get(nameSplit[i]) instanceof Number) {
                        // convert to correct Number type if we are dealing with numeric types
                        return (T) body.get(nameSplit[i]);
                    } else if (body.get(nameSplit[i]) instanceof String && type.isAssignableFrom(JSONArray.class)) {
                        // split value if a JSONArray is wanted, but have a string
                        JSONArray arr = new JSONArray();
                        if (type.isInstance(arr)) {
                            String[] split = body.getString(nameSplit[i]).split(",");
                            for (String s: split) {
                                arr.put(s);
                            }
                            return (T) arr;
                        }
                    }
                } else if (body.get(nameSplit[i]) instanceof JSONObject) {
                    body = body.getJSONObject(nameSplit[i]);
                }
            }
        } catch (IOException ignored) {}

        // fall back to URI parameters (we can't parse native JSONObjects or JSONArrays here)
        String value = super.getParameter(name, request);
        if (null != value) {
            // return as is, if a String is wanted and this is already a String
            if (type.isInstance(value)) {
                return (T) value;
            }
            // if a Boolean is wanted, check if we can convert the value
            if (type.isAssignableFrom(Boolean.class) &&
                    new ArrayList(Arrays.asList("true", "false", "1", "0")).contains(value.toLowerCase())) {
                return (T) Boolean.valueOf(value.equalsIgnoreCase("true") ||
                        value.equalsIgnoreCase("1"));
            }
            // convert to correct Number if dealing with numeric types
            if (NumberUtils.isNumber(value)) {
                try {
                    if (type.getSuperclass() == Number.class) {
                        return (T) Double.valueOf(value);
                    }
                } catch (NumberFormatException ignored) {}
            }
            // otherwise split value if JSONArray is wanted
            if (type.isAssignableFrom(JSONArray.class)) {
                JSONArray arr = new JSONArray();
                String[] split = value.split(",");
                for (String s: split) {
                    arr.put(s);
                }
                return (T) arr;
            }
        }

        // no valid parameter or conversion found
        return null;
    }
}