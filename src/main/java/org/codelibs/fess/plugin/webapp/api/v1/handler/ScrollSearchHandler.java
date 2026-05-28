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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.exception.IORuntimeException;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.exception.InvalidQueryException;
import org.codelibs.fess.exception.ResultOffsetExceededException;
import org.codelibs.fess.helper.SearchHelper;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.plugin.webapp.api.v1.params.JsonRequestParams;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.optional.OptionalThing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code GET /api/v1/documents/all} scroll search requests.
 */
public class ScrollSearchHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(ScrollSearchHandler.class);

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public ScrollSearchHandler() {
        // no-op
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        final String[] segments = splitPath(request);
        return segments.length > 4 && isTypeSegment(request, "documents") && "all".equals(segments[4]);
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!acceptHttpMethod(request, GET)) {
            return;
        }
        final SearchHelper searchHelper = ComponentUtil.getSearchHelper();
        final FessConfig fessConfig = ComponentUtil.getFessConfig();

        if (!fessConfig.isAcceptedSearchReferer(request.getHeader("referer"))) {
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, escapeJsonKeyValue(MESSAGE_FIELD, "Referer is invalid."));
            return;
        }

        if (!fessConfig.isApiSearchScroll()) {
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, escapeJsonKeyValue(MESSAGE_FIELD, "Scroll Search is not available."));
            return;
        }

        final StringBuilder buf = new StringBuilder(1000);
        request.setAttribute(Constants.SEARCH_LOG_ACCESS_TYPE, Constants.SEARCH_LOG_ACCESS_TYPE_JSON);
        final JsonRequestParams params = new JsonRequestParams(request, fessConfig);
        try {
            response.setContentType("application/x-ndjson; charset=UTF-8");
            applyJsonResponseHeaders(response);
            final long count = searchHelper.scrollSearch(params, doc -> {
                buf.setLength(0);
                buf.append('{');
                boolean first2 = true;
                for (final Map.Entry<String, Object> entry : doc.entrySet()) {
                    final String name = entry.getKey();
                    if (StringUtil.isNotBlank(name) && entry.getValue() != null) {
                        if (!first2) {
                            buf.append(',');
                        } else {
                            first2 = false;
                        }
                        buf.append(escapeJson(name));
                        buf.append(':');
                        buf.append(escapeJson(entry.getValue()));
                    }
                }
                buf.append('}');
                buf.append('\n');
                try {
                    response.getWriter().print(buf.toString());
                } catch (final IOException e) {
                    throw new IORuntimeException(e);
                }
                return true;
            }, OptionalThing.empty());
            response.flushBuffer();
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded {} documents", count);
            }
        } catch (final InvalidQueryException | ResultOffsetExceededException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a scroll request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, e);
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a scroll request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
}
