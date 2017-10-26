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

package de.webis.chatnoir2.webclient.api.v1;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiErrorModule;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import de.webis.chatnoir2.webclient.auth.api.ApiTokenRealm;
import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.subject.WebSubject;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * ChatNoir API module managing API keys.
 */
@ApiModuleV1("_manage_keys")
public class ApiKeyManagerApiModule extends ApiModuleBase
{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        // return user info
        WebSubject subject = (WebSubject) SecurityUtils.getSubject();

        final XContentBuilder builder = getResponseBuilder(request);
        builder.startObject();
        Map<String, Object> principalData = ApiTokenRealm.getPrincipalFields(subject);
        for (String key: principalData.keySet()) {
            builder.field(key, principalData.get(key));
        }
        builder.endObject();

        writeResponse(response, builder);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        String actionPath = getActionPath(request);
        switch (actionPath) {
            case "/create":
                actionCreate(request, response);
                break;

            case "/":
            case "":
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                        "No action given");
                return;

            default:
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                        "Invalid action: " + actionPath);

        }
    }

    @SuppressWarnings("unchecked")
    private void actionCreate(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSubject subject = (WebSubject) SecurityUtils.getSubject();
        String candidateApiKey = UUID.randomUUID().toString();
        HashMap<String, Object> newPrincipals = new HashMap<>();
        JSONObject requestPayload = getPayload(request);

        try {
            // validate API key creation permissions
            Set<String> userRoles = ApiTokenRealm.getTypedPrincipalField(subject, "roles");
            // TODO: change to dedicated role
            if (null == userRoles || !userRoles.contains("dev")) {
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_FORBIDDEN,
                        "You are not allowed to create new API keys");
                return;
            }

            // validate user data
            JSONObject newUserDataRaw = requestPayload.has("userdata") ? requestPayload.getJSONObject("userdata") : null;
            if (null == newUserDataRaw ||
                    anyNull(
                            newUserDataRaw.has("first_name") ? newUserDataRaw.get("first_name") : null,
                            newUserDataRaw.has("last_name") ? newUserDataRaw.get("last_name") : null,
                            newUserDataRaw.has("email") ? newUserDataRaw.get("email") : null
                    )) {
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                        "Incomplete userdata: first_name, last_name and email are required");
                return;
            }

            // validate limits
            JSONObject newLimitsRaw = requestPayload.has("limits") ? requestPayload.getJSONObject("limits") : null;
            if (null == newLimitsRaw ||
                    !allOfType(
                            Integer.class,
                            true,
                            newLimitsRaw.has("day") ? newLimitsRaw.get("day") : null,
                            newLimitsRaw.has("week") ? newLimitsRaw.get("week") : null,
                            newLimitsRaw.has("month") ? newLimitsRaw.get("month") : null
                    )) {
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                        "Invalid or missing API limits");
                return;
            }

            ApiTokenRealm.ApiLimits userLimits = ApiTokenRealm.getTypedPrincipalField(subject, "limits");
            assert userLimits != null;
            ApiTokenRealm.ApiLimits newLimits = new ApiTokenRealm.ApiLimits(
                    candidateApiKey,
                    newLimitsRaw.has("day") ? (Integer) newLimitsRaw.get("day") : userLimits.getDailyLimit(),
                    newLimitsRaw.has("week") ? (Integer) newLimitsRaw.get("week") : userLimits.getDailyLimit(),
                    newLimitsRaw.has("month") ? (Integer) newLimitsRaw.get("month") : userLimits.getDailyLimit());
            if ((userLimits.getDailyLimit() > 0 && newLimits.getDailyLimit() > userLimits.getDailyLimit()) ||
                    (userLimits.getWeeklyLimit() > 0 && newLimits.getWeeklyLimit() > userLimits.getWeeklyLimit()) ||
                    (userLimits.getMonthlyLimit() > 0 && newLimits.getMonthlyLimit() > userLimits.getMonthlyLimit()) ||
                    (userLimits.getDailyLimit() > 0 && newLimits.getDailyLimit() <= 0) ||
                    (userLimits.getWeeklyLimit() > 0 && newLimits.getWeeklyLimit() <= 0) ||
                    (userLimits.getMonthlyLimit() > 0 && newLimits.getMonthlyLimit() <= 0)
                    ) {
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_FORBIDDEN,
                        "API limits cannot exceed user limits");
                return;
            }

            // validate additional roles
            JSONArray newRoles = requestPayload.has("roles") ? requestPayload.getJSONArray("roles") : null;
            if (null != newRoles && !userRoles.contains("admin")) {
                for (Object role : newRoles) {
                    if (!userRoles.contains(role.toString())) {
                        ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_FORBIDDEN,
                                String.format("No permission to assign role '%s'", role));
                        return;
                    }
                }
            }

            // everything fine if we got here, so go ahead and assemble new principal data
            HashMap<String, Object> newUserData = new HashMap<>();
            Map<String, Object> newUserDataRawMap = newUserDataRaw.toMap();
            newUserData.put("first_name", newUserDataRawMap.get("first_name"));
            newUserData.put("last_name", newUserDataRawMap.get("last_name"));
            newUserData.put("email", newUserDataRawMap.get("email"));
            newUserData.put("address", newUserDataRawMap.get("address"));
            newUserData.put("zip_code", newUserDataRawMap.get("zip_code"));
            newUserData.put("country", newUserDataRawMap.get("country"));
            newPrincipals.put("user", newUserData);

            newPrincipals.put("limits", newLimits);
            newPrincipals.put("roles", null != newRoles ? newRoles : new ArrayList<String>());
            newPrincipals.put("parent", ApiTokenRealm.getTypedPrincipalField(subject, "apikey"));

            // TODO: add remote_hosts and expires

        } catch (Exception ignored) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                    "Invalid request");
            return;
        }

        // create the API key
        TransportClient client = Configured.getInstance().getClient();
        ConfigLoader.Config config = Configured.getInstance().getConf();
        IndexResponse indexResponse = client.prepareIndex(config.getString(
                "auth.api.key_index"),
                "apikey",
                candidateApiKey).setSource(newPrincipals).get();

        if (indexResponse.status() == RestStatus.CREATED) {
            XContentBuilder builder = getResponseBuilder(request)
                    .startObject()
                        .field("status", RestStatus.CREATED.getStatus())
                        .field("message", "API key created")
                        .field("apikey", candidateApiKey)
                    .endObject();
            writeResponse(response, builder, RestStatus.CREATED.getStatus());
        } else {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_INTERNAL_SERVER_ERROR,
                    "Error creating a new API key, please try again later");
        }
    }
}