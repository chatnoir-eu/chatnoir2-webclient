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
import org.elasticsearch.rest.RestStatus;
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
        ApiKeyModel userModel = ApiTokenRealm.getUserModel(subject);
        Map<String, Object> modelMap = userModel.getAll();
        for (String key: modelMap.keySet()) {
            builder.field(key, modelMap.get(key));
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
        JSONObject requestPayload = getPayload(request);

        ApiKeyModel userModel = ApiTokenRealm.getUserModel(subject);
        ApiKeyModel candiateModel = new ApiKeyModel();

        // validate API key creation permissions
        List<String> userRoles = userModel.getRoles();
        if (null == userRoles || !(userRoles.contains("dev") || userRoles.contains("keycreate") || userRoles.contains("admin"))) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_FORBIDDEN,
                    "You are not allowed to create new API keys");
            return;
        }

        candiateModel.setId(candidateApiKey);
        candiateModel.setParent(userModel);
        candiateModel.putAll(requestPayload.toMap());
        if (!candiateModel.validate()) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST, candiateModel.message());
            return;
        }
        boolean isCreated = candiateModel.commit();

        // generate API response
        if (isCreated) {
            XContentBuilder builder = getResponseBuilder(request)
                    .startObject()
                        .field("status", RestStatus.CREATED.getStatus())
                        .field("message", "API key created")
                        .field("apikey", candiateModel.getId())
                    .endObject();
            writeResponse(response, builder, RestStatus.CREATED.getStatus());
        } else {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_INTERNAL_SERVER_ERROR,
                    "Error creating a new API key, please try again later");
        }
    }
}
