/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.plugin.webapp.api.v1.handler;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.FavoriteLogService;
import org.codelibs.fess.exception.WebApiException;
import org.codelibs.fess.helper.SearchHelper;
import org.codelibs.fess.helper.SystemHelper;
import org.codelibs.fess.helper.UserInfoHelper;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.DocumentUtil;
import org.dbflute.optional.OptionalThing;
import org.opensearch.script.Script;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code POST /api/v1/documents/{id}/favorite} requests.
 */
public class FavoriteHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(FavoriteHandler.class);

    private static final String DOC_ID_FIELD = "doc_id";

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public FavoriteHandler() {
        // no-op
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        final String[] segments = splitPath(request);
        return segments.length > 5 && isTypeSegment(request, "documents") && "favorite".equals(segments[5]);
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        // Extract and store doc ID from path — segments[4] is the document ID
        final String[] segments = splitPath(request);
        request.setAttribute(DOC_ID_FIELD, segments[4]);

        if (!acceptHttpMethod(request, POST)) {
            return;
        }

        if (!ComponentUtil.getFessConfig().isUserFavorite()) {
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, escapeJsonKeyValue(MESSAGE_FIELD, "Unsupported operation."));
            return;
        }

        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final UserInfoHelper userInfoHelper = ComponentUtil.getUserInfoHelper();
        final SearchHelper searchHelper = ComponentUtil.getSearchHelper();
        final FavoriteLogService favoriteLogService = ComponentUtil.getComponent(FavoriteLogService.class);
        final SystemHelper systemHelper = ComponentUtil.getSystemHelper();

        try {
            final Object docIdObj = request.getAttribute(DOC_ID_FIELD);
            if (docIdObj == null) {
                throw new WebApiException(HttpServletResponse.SC_BAD_REQUEST, "docId is empty.");
            }
            final String docId = docIdObj.toString();
            final String queryId = request.getParameter("queryId");

            final String[] docIds = userInfoHelper.getResultDocIds(URLDecoder.decode(queryId, Constants.UTF_8));
            if (docIds == null) {
                throw new WebApiException(HttpServletResponse.SC_BAD_REQUEST, "No searched urls.");
            }

            searchHelper
                    .getDocumentByDocId(docId, new String[] { fessConfig.getIndexFieldUrl(), fessConfig.getIndexFieldLang() },
                            OptionalThing.empty())
                    .ifPresent(doc -> {
                        final String favoriteUrl = DocumentUtil.getValue(doc, fessConfig.getIndexFieldUrl(), String.class);
                        final String userCode = userInfoHelper.getUserCode();

                        if (StringUtil.isBlank(userCode)) {
                            throw new WebApiException(HttpServletResponse.SC_BAD_REQUEST, "No user session.");
                        }
                        if (StringUtil.isBlank(favoriteUrl)) {
                            throw new WebApiException(HttpServletResponse.SC_BAD_REQUEST, "URL is null.");
                        }

                        if (!ArrayUtils.contains(docIds, docId)) {
                            throw new WebApiException(HttpServletResponse.SC_NOT_FOUND, "Not found: " + favoriteUrl);
                        }

                        if (!favoriteLogService.addUrl(userCode, (userInfo, favoriteLog) -> {
                            favoriteLog.setUserInfoId(userInfo.getId());
                            favoriteLog.setUrl(favoriteUrl);
                            favoriteLog.setDocId(docId);
                            favoriteLog.setQueryId(queryId);
                            favoriteLog.setCreatedAt(systemHelper.getCurrentTimeAsLocalDateTime());
                        })) {
                            throw new WebApiException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to add url: " + favoriteUrl);
                        }

                        final String id = DocumentUtil.getValue(doc, fessConfig.getIndexFieldId(), String.class);
                        searchHelper.update(id, builder -> {
                            final Script script = ComponentUtil.getLanguageHelper()
                                    .createScript(doc, "ctx._source." + fessConfig.getIndexFieldFavoriteCount() + "+=1");
                            builder.setScript(script);
                            final Map<String, Object> upsertMap = new HashMap<>();
                            upsertMap.put(fessConfig.getIndexFieldFavoriteCount(), 1);
                            builder.setUpsert(upsertMap);
                            builder.setRefreshPolicy(Constants.TRUE);
                        });

                        writeJsonResponse(HttpServletResponse.SC_CREATED, escapeJsonKeyValue(RESULT_FIELD, "created"));

                    })
                    .orElse(() -> {
                        throw new WebApiException(HttpServletResponse.SC_NOT_FOUND, "Not found: " + docId);
                    });

        } catch (final WebApiException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a favorite request.", e);
            }
            writeJsonResponse(e.getStatusCode(), e);
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a favorite request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
}
