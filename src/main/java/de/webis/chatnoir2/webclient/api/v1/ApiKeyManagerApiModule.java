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
import de.webis.chatnoir2.webclient.model.api.ApiKeyModel;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.subject.WebSubject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        ApiKeyModel userModel = ApiTokenRealm.getUserModel(subject);
        assert userModel != null;

        final XContentBuilder builder = getResponseBuilder(request);
        builder.startObject();
        builder.field("apikey", userModel.getId());

        Map<String, Object> modelMap = userModel.getAll();
        for (String key: modelMap.keySet()) {
            // never return the parent API key, as it would allow the owner of a child key to issue new keys
            if (key.equals("parent")) {
                continue;
            }

            if (key.equals("remote_hosts")) {
                builder.startArray("remote_hosts");
                // noinspection unchecked
                for (InetAddress addr: userModel.getRemoteHosts()) {
                    // InetAddress.toString() is too ugly
                    builder.value(addr.getHostAddress());
                }
                builder.endArray();
                continue;
            }

            LocalDateTime expiry = userModel.getExpiryDate();
            if (key.equals("expires") && null != expiry) {
                // return actual expiry information from trust chain
                builder.field("expires", expiry.format(DateTimeFormatter.ISO_DATE_TIME));
                continue;
            }

            if (key.equals("revoked")) {
                // return actual revocation information from trust chain
                builder.field("revoked", userModel.isRevoked());
                continue;
            }

            builder.field(key, modelMap.get(key));
        }
        builder.endObject();

        writeResponse(response, builder);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        Path actionPath = getActionPath(request);
        if (actionPath.getNameCount() < 1) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST, "No action given");
            return;
        }

        String action = actionPath.getName(0).toString();
        switch (action) {
            case "create":
                actionCreate(request, response);
                break;

            default:
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_NOT_FOUND,
                        "Invalid action: " + action);
        }
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        Path actionPath = getActionPath(request);
        if (actionPath.getNameCount() < 1) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST, "No action given");
            return;
        }

        String action = actionPath.getName(0).toString();
        switch (action) {
            case "update":
                actionUpdate(request, response);
                break;

            case "revoke":
                actionRevoke(request, response);
                break;

            default:
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_NOT_FOUND,
                        "Invalid action: " + action);
        }
    }

    /**
     * Ensure that current user has API key creation and modification rights
     * and create appropriate error responses if not.
     *
     * @param userModel user to check permissions for
     * @return false if user is not authorized to modify API keys
     */
    private boolean checkApiKeyCreationPermissions(HttpServletRequest request, HttpServletResponse response, ApiKeyModel userModel) throws IOException, ServletException
    {
        if (null == userModel) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_FORBIDDEN,
                    "User not authenticated");
            return false;
        }

        // validate API key creation permissions
        Set<String> userRoles = userModel.getRoles();
        if (null == userRoles || !(userRoles.contains("dev") || userRoles.contains("keycreate") || userRoles.contains("admin"))) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_FORBIDDEN,
                    "You are not allowed to create or update API keys");
            return false;
        }

        return true;
    }

    /**
     * Check if a model is a child of another model and generate error responses if not.
     *
     * @param parent parent model
     * @param child child to check
     * @return true if <tt>child</tt> is a child of <tt>parent</tt>
     */
    private boolean checkApiKeyIsChild(HttpServletRequest request, HttpServletResponse response,
                                       final ApiKeyModel parent, ApiKeyModel child) throws IOException, ServletException
    {
        ApiKeyModel tmpParent = child;
        while (!tmpParent.getId().equals(parent.getId())) {
            if (null == tmpParent.getParent()) {
                // only allow modification of children
                ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_FORBIDDEN,
                        "Target API key is not a child key");
                return false;
            }

            tmpParent = tmpParent.getParent();
        }

        return true;
    }

    /**
     * Update API key model from request payload.
     * Generates error responses in case of failure.
     *
     * @param request HTTP request with JSON data
     * @param response servlet response object
     * @param model model to update
     * @return true on success
     */
    private boolean updateApiKeyModel(HttpServletRequest request, HttpServletResponse response, ApiKeyModel model) throws IOException, ServletException
    {
        ApiKeyModel userModel = ApiTokenRealm.getUserModel(SecurityUtils.getSubject());
        if (!checkApiKeyCreationPermissions(request, response, userModel)) {
            return false;
        }

        JSONObject requestPayload = getPayload(request);
        model.putAll(requestPayload.toMap());
        model.setParent(userModel);
        if (!model.validate()) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST, model.message());
            return false;
        }

        if (!model.commit()) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_INTERNAL_SERVER_ERROR,
                    "Error updating API key, please try again later");
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private void actionCreate(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        ApiKeyModel userModel = ApiTokenRealm.getUserModel(SecurityUtils.getSubject());
        if (!checkApiKeyCreationPermissions(request, response, userModel)) {
            return;
        }

        ApiKeyModel candiateModel = new ApiKeyModel();
        candiateModel.setId(UUID.randomUUID().toString());

        if (!updateApiKeyModel(request, response, candiateModel)) {
            return;
        }

        // generate API response
        XContentBuilder builder = getResponseBuilder(request)
                .startObject()
                .field("message", "API key created")
                .field("apikey", candiateModel.getId())
                .endObject();
        writeResponse(response, builder, HttpServletResponse.SC_CREATED);
    }

    private void actionUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        ApiKeyModel userModel = ApiTokenRealm.getUserModel(SecurityUtils.getSubject());
        if (!checkApiKeyCreationPermissions(request, response, userModel)) {
            return;
        }

        Path actionPath = getActionPath(request);
        if (actionPath.getNameCount() < 2) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                    "Missing target API key");
            return;
        }

        ApiKeyModel updateModel = new ApiKeyModel();
        String targetApiKey = actionPath.getName(1).toString();
        if (!updateModel.loadById(targetApiKey)) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_NOT_FOUND,
                    "Invalid target API key " + targetApiKey);
            return;
        }

        assert userModel != null;
        if (!userModel.getRoles().contains("admin") && !checkApiKeyIsChild(request, response, userModel, updateModel)) {
            return;
        }

        if (!updateApiKeyModel(request, response, updateModel)) {
            return;
        }

        // generate API response
        XContentBuilder builder = getResponseBuilder(request)
                .startObject()
                .field("message", "API key updated")
                .field("apikey", updateModel.getId())
                .endObject();
        writeResponse(response, builder, HttpServletResponse.SC_OK);
    }

    private void actionRevoke(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        ApiKeyModel userModel = ApiTokenRealm.getUserModel(SecurityUtils.getSubject());
        if (!checkApiKeyCreationPermissions(request, response, userModel)) {
            return;
        }

        Path actionPath = getActionPath(request);
        if (actionPath.getNameCount() < 2) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                    "Missing target API key");
            return;
        }

        ApiKeyModel model = new ApiKeyModel();
        String targetApiKey = actionPath.getName(1).toString();
        if (!model.loadById(targetApiKey)) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_NOT_FOUND,
                    "Invalid target API key " + targetApiKey);
            return;
        }

        assert userModel != null;
        if (!userModel.getRoles().contains("admin") && !checkApiKeyIsChild(request, response, userModel, model)) {
            return;
        }

        model.revoke();
        if (!model.commit()) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_INTERNAL_SERVER_ERROR,
                    "Error updating API key, please try again later");
            return;
        }

        // generate API response
        XContentBuilder builder = getResponseBuilder(request)
                .startObject()
                .field("message", "API key revoked")
                .field("apikey", model.getId())
                .endObject();
        writeResponse(response, builder, HttpServletResponse.SC_OK);
    }
}
