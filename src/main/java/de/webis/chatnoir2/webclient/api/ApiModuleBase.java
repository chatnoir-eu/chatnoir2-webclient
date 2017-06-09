/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api;

import de.webis.chatnoir2.webclient.ChatNoirServlet;
import de.webis.chatnoir2.webclient.api.exceptions.UserErrorException;
import de.webis.chatnoir2.webclient.auth.api.ApiKeyAuthenticationToken;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Base class for ChatNoir REST API modules.
 */
public abstract class ApiModuleBase extends ChatNoirServlet
{

    /**
     * Base name for request attributes
     */
    private final String REQUEST_ATTRIBUTE_BASE_NAME = ApiModuleBase.class.getName();

    /**
     * Initialize API request.
     * This method is called on every request before anything else.
     *
     * @param request HTTP request
     */
    public void initApiRequest(final HttpServletRequest request, final HttpServletResponse reponse) throws ServletException
    {
        setPrettyPrint(request, isNestedParameterSet("pretty", request));
    }

    /**
     * Get the user API key / token from the request.
     *
     * @param request HTTP request
     * @return API token or null if none was given
     */
    public ApiKeyAuthenticationToken getUserToken(final HttpServletRequest request) throws ServletException
    {
        String token = getTypedNestedParameter(String.class, "apikey", request);
        return new ApiKeyAuthenticationToken(token);
    }

    /**
     * Generate a JSON response object to indicate an error.
     *
     * @param request HTTP request
     * @param errorCode API/HTTP error code
     * @param errorMessage error description
     * @return generated JSONObject
     */
    public final XContentBuilder generateErrorResponse(HttpServletRequest request, final int errorCode, final String errorMessage)
    {
        try {
            final XContentBuilder jb = getResponseBuilder(request);
            jb.startObject()
                    .startObject("error")
                        .field("code", errorCode)
                        .field("message", errorMessage)
                    .endObject()
            .endObject();

            return jb;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Write an error response for rejecting the used HTTP request method.
     *
     * @param request HTTP request object
     * @param response HTTP response object
     */
    public final void rejectMethod(final HttpServletRequest request, final HttpServletResponse response)
    {
        try {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                    "Unsupported request method");
        } catch (Throwable e) {
            ApiBootstrap.handleException(e, request, response);
        }
    }

    /**
     * Set whether JSON responses should be nicely formatted or not.
     *
     * @param request HTTP request
     * @param prettyPrint whether to pretty-print responses
     */
    public synchronized void setPrettyPrint(HttpServletRequest request, boolean prettyPrint) {
        request.setAttribute(REQUEST_ATTRIBUTE_BASE_NAME + ".prettyPrint", prettyPrint);
    }

    /**
     * Get whether JSON responses should be nicely formatted or not.
     *
     * @return whether to pretty-print responses
     * @param request HTTP request
     */
    public boolean getPrettyPrint(HttpServletRequest request) {
        Boolean prettyPrint = (Boolean) request.getAttribute(REQUEST_ATTRIBUTE_BASE_NAME + ".prettyPrint");
        return prettyPrint != null && prettyPrint;
    }

    /**
     * Write API output to HTTP response with default status code 200.
     *
     * @param response HTTP response object
     * @param responseBuilder response XContent builder
     * @throws IOException
     */
    public void writeResponse(final HttpServletResponse response, final XContentBuilder responseBuilder) throws IOException
    {
        writeResponse(response, responseBuilder, HttpServletResponse.SC_OK);
    }

    /**
     * Write API output to HTTP response with default status code 200.
     *
     * @param response HTTP response object
     * @param responseBuilder response XContent builder
     * @param responseCode HTTP response status code
     * @throws IOException
     */
    public void writeResponse(final HttpServletResponse response, final XContentBuilder responseBuilder, final int responseCode) throws IOException
    {
        response.setStatus(responseCode);
        response.setContentType("application/json");
        response.getWriter().write(responseBuilder.string());
        response.getWriter().flush();
    }

    /**
     * Handle GET request to API endpoint.
     * If GET is not supported for this request, {@link #rejectMethod)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    @Override
    public abstract void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

    /**
     * Handle POST request to API endpoint.
     * If POST is not supported for this request, {@link #rejectMethod)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    @Override
    public abstract void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

    /**
     * Get configured response builder.
     *
     * @param request HTTP request
     * @return response builder
     */
    protected XContentBuilder getResponseBuilder(HttpServletRequest request)
    {
        try {
            if (getPrettyPrint(request)) {
                return XContentFactory.jsonBuilder().prettyPrint();
            }
            return XContentFactory.jsonBuilder();
        } catch (Exception e) {
            // should never happen
            return null;
        }
    }

    /**
     * Return and parse POST data payload.
     *
     * @param request HTTP request
     * @return parsed JSON payload
     * @throws ServletException if failed to parse payload
     */
    protected JSONObject getPayload(HttpServletRequest request) throws ServletException {
        JSONObject parsedPayload = (JSONObject) request.getAttribute(REQUEST_ATTRIBUTE_BASE_NAME + ".parsedPayload");
        if (null != parsedPayload) {
            return parsedPayload;
        }

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            reader.reset();
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            Configured.getInstance().getSysLogger().error("Failed to parse request payload", e);
            return new JSONObject();
        }

        try {
            String jsonStr = sb.toString().trim();
            if (jsonStr.isEmpty()) {
                jsonStr = "{}";
            }
            JSONObject json = new JSONObject(jsonStr);
            request.setAttribute(REQUEST_ATTRIBUTE_BASE_NAME + ".parsedPayload", json);
            return json;
        } catch (JSONException e) {
            throw new UserErrorException("Invalid JSON payload supplied: " + e.getMessage());
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
    protected <T> T getTypedNestedParameter(Class<T> type, String name, final HttpServletRequest request) throws ServletException
    {
        JSONObject body = getPayload(request);
        String[] nameSplit = name.split("\\.");
        for (int i = 0; i < nameSplit.length; ++i) {
            if (!body.has(nameSplit[i])) {
                break;
            }

            if (i == nameSplit.length - 1) {
                if (body.get(nameSplit[i]) == null) {
                    return null;
                }

                if (type.isAssignableFrom(Boolean.class)) {
                    // check boolean parameters first
                    return (T) evaluatesTrue(body.get(nameSplit[i]).toString());
                }
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

        // fall back to URI parameters (we can't parse native JSONObjects or JSONArrays here)
        String value = request.getParameter(name);
        if (null != value) {
            // return as is, if a String is wanted and this is already a String
            if (type.isInstance(value)) {
                return (T) value;
            }
            // if a Boolean is wanted, check if we can convert the value
            if (type.isAssignableFrom(Boolean.class)) {
                return (T) evaluatesTrue(value);
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

    private Boolean evaluatesTrue(String value)
    {
        if (value == null || value.equalsIgnoreCase("false") ||
                value.equalsIgnoreCase("null") || value.equalsIgnoreCase("none")) {
            return false;
        }

        try {
            if (Double.parseDouble(value) == 0.0) {
                return false;
            }
        } catch (NumberFormatException ignored) {}


        // something else which is not 0.0, false, none or null (we count empty as true)
        return true;
    }

    /**
     * Check whether a boolean parameter exists and a value that evaluates to true.
     *
     * @param name parameter name
     * @param request HTTP request
     * @return whether parameter is set and evaluates to true
     */
    public boolean isNestedParameterSet(String name, HttpServletRequest request) throws ServletException
    {
        Boolean param = getTypedNestedParameter(Boolean.class, name, request);
        if (param == null) {
            return getTypedNestedParameter(Object.class, name, request) != null;
        }
        return param;
    }
}