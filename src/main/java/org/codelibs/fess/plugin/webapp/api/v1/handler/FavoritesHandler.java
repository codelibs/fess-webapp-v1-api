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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.app.service.FavoriteLogService;
import org.codelibs.fess.entity.SearchRequestParams.SearchRequestType;
import org.codelibs.fess.exception.WebApiException;
import org.codelibs.fess.helper.SearchHelper;
import org.codelibs.fess.helper.UserInfoHelper;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.DocumentUtil;
import org.dbflute.optional.OptionalThing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code GET /api/v1/favorites} requests.
 */
public class FavoritesHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(FavoritesHandler.class);

    private static final String TYPE = "favorites";

    private static final String DOC_ID_FIELD = "doc_id";

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public FavoritesHandler() {
        // no-op
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        return isTypeSegment(request, TYPE);
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!acceptHttpMethod(request, GET)) {
            return;
        }

        if (!ComponentUtil.getFessConfig().isUserFavorite()) {
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, escapeJsonKeyValue(MESSAGE_FIELD, "Unsupported operation."));
            return;
        }

        final UserInfoHelper userInfoHelper = ComponentUtil.getUserInfoHelper();
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final SearchHelper searchHelper = ComponentUtil.getSearchHelper();
        final FavoriteLogService favoriteLogService = ComponentUtil.getComponent(FavoriteLogService.class);

        try {
            final String queryId = request.getParameter("queryId");
            final String userCode = userInfoHelper.getUserCode();

            if (StringUtil.isBlank(userCode)) {
                throw new WebApiException(HttpServletResponse.SC_BAD_REQUEST, "No user session.");
            }
            if (StringUtil.isBlank(queryId)) {
                throw new WebApiException(HttpServletResponse.SC_BAD_REQUEST, "Query ID is null.");
            }

            final String[] docIds = userInfoHelper.getResultDocIds(queryId);
            final List<Map<String, Object>> docList = searchHelper.getDocumentListByDocIds(docIds, new String[] {
                    fessConfig.getIndexFieldUrl(), fessConfig.getIndexFieldDocId(), fessConfig.getIndexFieldFavoriteCount() },
                    OptionalThing.empty(), SearchRequestType.JSON);
            List<String> urlList = new ArrayList<>(docList.size());
            for (final Map<String, Object> doc : docList) {
                final String urlObj = DocumentUtil.getValue(doc, fessConfig.getIndexFieldUrl(), String.class);
                if (urlObj != null) {
                    urlList.add(urlObj);
                }
            }
            final Set<String> favoriteUrls = new HashSet<>(favoriteLogService.getUrlList(userCode, urlList));
            final List<String> docIdList = new ArrayList<>(favoriteUrls.size());
            for (final Map<String, Object> doc : docList) {
                final String urlObj = DocumentUtil.getValue(doc, fessConfig.getIndexFieldUrl(), String.class);
                if (urlObj != null && favoriteUrls.contains(urlObj)) {
                    final String docIdObj = DocumentUtil.getValue(doc, fessConfig.getIndexFieldDocId(), String.class);
                    if (docIdObj != null) {
                        docIdList.add(docIdObj);
                    }
                }
            }

            final StringBuilder buf = new StringBuilder(255);
            buf.append("\"record_count\":").append(docIdList.size());
            buf.append(", \"data\":[");
            if (!docIdList.isEmpty()) {
                for (int i = 0; i < docIdList.size(); i++) {
                    if (i > 0) {
                        buf.append(',');
                    }
                    buf.append('{').append(escapeJsonKeyValue(DOC_ID_FIELD, docIdList.get(i))).append('}');
                }
            }
            buf.append(']');
            writeJsonResponse(HttpServletResponse.SC_OK, buf.toString());
        } catch (final WebApiException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a favorites request.", e);
            }
            writeJsonResponse(e.getStatusCode(), e);
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a favorites request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
}
