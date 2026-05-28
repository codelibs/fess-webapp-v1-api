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
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.exception.WebApiException;
import org.codelibs.fess.helper.PopularWordHelper;
import org.codelibs.fess.util.ComponentUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code GET /api/v1/popular-words} requests.
 */
public class PopularWordHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(PopularWordHandler.class);

    private static final String TYPE = "popular-words";

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public PopularWordHandler() {
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

        if (!ComponentUtil.getFessConfig().isWebApiPopularWord()) {
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, escapeJsonKeyValue(MESSAGE_FIELD, "Unsupported operation."));
            return;
        }

        final String seed = request.getParameter("seed");
        String[] tags = SearchRequestParams.getParamValueArray(request, "label");
        final String key = ComponentUtil.getVirtualHostHelper().getVirtualHostKey();
        if (StringUtil.isNotBlank(key)) {
            tags = ArrayUtils.addAll(tags, key);
        }
        final String[] fields = request.getParameterValues("field");

        final PopularWordHelper popularWordHelper = ComponentUtil.getPopularWordHelper();

        final StringBuilder buf = new StringBuilder(255);
        try {
            final List<String> popularWordList = popularWordHelper.getWordList(SearchRequestParams.SearchRequestType.JSON, seed, tags, null,
                    fields, StringUtil.EMPTY_STRINGS);
            buf.append("\"record_count\":").append(popularWordList.size()).append(',');
            buf.append("\"data\":[");
            boolean first1 = true;
            for (final String word : popularWordList) {
                if (!first1) {
                    buf.append(',');
                } else {
                    first1 = false;
                }
                buf.append(escapeJson(word));
            }
            buf.append(']');
            writeJsonResponse(HttpServletResponse.SC_OK, buf.toString());
        } catch (final WebApiException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a popularWord request.", e);
            }
            writeJsonResponse(e.getStatusCode(), e);
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a popularWord request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
}
