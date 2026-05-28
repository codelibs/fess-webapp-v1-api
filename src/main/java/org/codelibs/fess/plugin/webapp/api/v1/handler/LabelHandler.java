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
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.Constants;
import org.codelibs.fess.entity.SearchRequestParams.SearchRequestType;
import org.codelibs.fess.helper.LabelTypeHelper;
import org.codelibs.fess.util.ComponentUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code GET /api/v1/labels} requests.
 */
public class LabelHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(LabelHandler.class);

    private static final String TYPE = "labels";

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public LabelHandler() {
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

        final LabelTypeHelper labelTypeHelper = ComponentUtil.getLabelTypeHelper();

        final StringBuilder buf = new StringBuilder(255);
        try {
            final List<Map<String, String>> labelTypeItems = labelTypeHelper.getLabelTypeItemList(SearchRequestType.JSON,
                    request.getLocale() == null ? Locale.ROOT : request.getLocale());
            buf.append("\"record_count\":");
            buf.append(labelTypeItems.size());
            if (!labelTypeItems.isEmpty()) {
                buf.append(',');
                buf.append("\"data\":[");
                boolean first1 = true;
                for (final Map<String, String> labelMap : labelTypeItems) {
                    if (!first1) {
                        buf.append(',');
                    } else {
                        first1 = false;
                    }
                    buf.append("{\"label\":");
                    buf.append(escapeJson(labelMap.get(Constants.ITEM_LABEL)));
                    buf.append(", \"value\":");
                    buf.append(escapeJson(labelMap.get(Constants.ITEM_VALUE)));
                    buf.append('}');
                }
                buf.append(']');
            }

            writeJsonResponse(HttpServletResponse.SC_OK, buf.toString());
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a label request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
}
