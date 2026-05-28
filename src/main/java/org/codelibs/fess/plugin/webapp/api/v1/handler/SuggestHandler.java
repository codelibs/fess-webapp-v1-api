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

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.entity.SearchRequestParams.SearchRequestType;
import org.codelibs.fess.helper.RoleQueryHelper;
import org.codelibs.fess.helper.SearchHelper;
import org.codelibs.fess.helper.SuggestHelper;
import org.codelibs.fess.plugin.webapp.api.v1.params.SuggestRequestParameter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.codelibs.fess.util.ComponentUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.codelibs.core.stream.StreamUtil.stream;

/**
 * Handles {@code GET /api/v1/suggest-words} requests.
 */
public class SuggestHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(SuggestHandler.class);

    private static final String TYPE = "suggest-words";

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public SuggestHandler() {
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

        if (!ComponentUtil.getFessConfig().isAcceptedSearchReferer(request.getHeader("referer"))) {
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, escapeJsonKeyValue(MESSAGE_FIELD, "Referer is invalid."));
            return;
        }

        final RoleQueryHelper roleQueryHelper = ComponentUtil.getRoleQueryHelper();
        final SearchHelper searchHelper = ComponentUtil.getSearchHelper();

        final StringBuilder buf = new StringBuilder(255);
        try {
            final SuggestRequestParameter parameter = SuggestRequestParameter.parse(request);
            final String[] langs = searchHelper.getLanguages(request, parameter);

            final SuggestHelper suggestHelper = ComponentUtil.getSuggestHelper();
            final SuggestRequestBuilder builder = suggestHelper.suggester().suggest();
            builder.setQuery(parameter.getQuery());
            stream(parameter.getSuggestFields()).of(stream -> stream.forEach(builder::addField));
            roleQueryHelper.build(SearchRequestType.SUGGEST).stream().forEach(builder::addRole);
            builder.setSize(parameter.getNum());
            stream(langs).of(stream -> stream.forEach(builder::addLang));

            stream(parameter.getTags()).of(stream -> stream.forEach(builder::addTag));
            final String key = ComponentUtil.getVirtualHostHelper().getVirtualHostKey();
            if (StringUtil.isNotBlank(key)) {
                builder.addTag(key);
            }

            builder.addKind(SuggestItem.Kind.USER.toString());
            if (ComponentUtil.getFessConfig().isSuggestSearchLog()) {
                builder.addKind(SuggestItem.Kind.QUERY.toString());
            }
            if (ComponentUtil.getFessConfig().isSuggestDocuments()) {
                builder.addKind(SuggestItem.Kind.DOCUMENT.toString());
            }

            final SuggestResponse suggestResponse = builder.execute().getResponse();

            buf.append("\"query_time\":").append(suggestResponse.getTookMs());
            buf.append(",\"record_count\":").append(suggestResponse.getTotal());
            buf.append(",\"page_size\":").append(suggestResponse.getNum());

            if (!suggestResponse.getItems().isEmpty()) {
                buf.append(",\"data\":[");

                boolean first = true;
                for (final SuggestItem item : suggestResponse.getItems()) {
                    if (!first) {
                        buf.append(',');
                    }
                    first = false;

                    buf.append("{\"text\":\"").append(StringEscapeUtils.escapeJson(item.getText())).append('\"');
                    buf.append(",\"labels\":[");
                    for (int i = 0; i < item.getTags().length; i++) {
                        if (i > 0) {
                            buf.append(',');
                        }
                        buf.append('\"').append(StringEscapeUtils.escapeJson(item.getTags()[i])).append('\"');
                    }
                    buf.append(']');
                    buf.append('}');
                }
                buf.append(']');
            }

            writeJsonResponse(HttpServletResponse.SC_OK, buf.toString());
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a suggest request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
}
