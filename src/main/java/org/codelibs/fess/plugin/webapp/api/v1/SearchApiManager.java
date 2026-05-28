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
package org.codelibs.fess.plugin.webapp.api.v1;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.api.BaseApiManager;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.plugin.webapp.api.v1.handler.AbstractApiHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.ApiHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.ChatHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.ChatStreamHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.FavoriteHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.FavoritesHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.LabelHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.PingHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.PopularWordHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.ScrollSearchHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.SearchHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.SuggestHandler;
import org.codelibs.fess.util.ComponentUtil;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Dispatcher API manager for the {@code /api/v1} endpoint family.
 * Delegates each request to the first matching {@link ApiHandler}.
 */
public class SearchApiManager extends BaseApiManager {

    private static final Logger logger = LogManager.getLogger(SearchApiManager.class);

    private static final String CHAT_PATH_PREFIX = "/api/v1/chat";

    /**
     * Default MIME type used by the dispatcher's 404 fallback and propagated to handlers via
     * {@link #setMimeType(String)}.
     */
    protected String mimeType = "application/json";

    /**
     * Ordered list of handlers. FavoriteHandler precedes ScrollSearchHandler precedes SearchHandler
     * so that {@code /api/v1/documents/{id}/favorite} and {@code /api/v1/documents/all} win over the
     * generic search fallback. ChatStreamHandler precedes ChatHandler for the same reason on the
     * chat side.
     */
    private final List<ApiHandler> handlers;

    /**
     * Constructor. Initialises the handler chain in dispatch priority order.
     */
    public SearchApiManager() {
        setPathPrefix("/api/v1");
        handlers = List.of(new ChatStreamHandler(), new ChatHandler(), new FavoriteHandler(), new ScrollSearchHandler(),
                new FavoritesHandler(), new LabelHandler(), new PopularWordHandler(), new SuggestHandler(), new PingHandler(),
                new SearchHandler());
    }

    /**
     * Registers this API manager with the {@link org.codelibs.fess.api.WebApiManagerFactory}.
     */
    @PostConstruct
    public void register() {
        if (logger.isInfoEnabled()) {
            logger.info("Loaded {}", this.getClass().getSimpleName());
        }
        ComponentUtil.getWebApiManagerFactory().add(this);
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        final String servletPath = request.getServletPath();
        if (!servletPath.startsWith(pathPrefix)) {
            return false;
        }
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        // Always match chat paths when RAG is enabled, regardless of isWebApiJson
        if (servletPath.startsWith(CHAT_PATH_PREFIX) && fessConfig.isRagChatEnabled()) {
            return true;
        }
        return fessConfig.isWebApiJson();
    }

    @Override
    public void process(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        for (final ApiHandler handler : handlers) {
            if (handler.matches(request)) {
                handler.handle(request, response, chain);
                return;
            }
        }
        AbstractApiHandler.writeJsonResponse(HttpServletResponse.SC_NOT_FOUND,
                AbstractApiHandler.escapeJsonKeyValue("message", "Not found."), mimeType);
    }

    @Override
    protected void writeHeaders(final HttpServletResponse response) {
        ComponentUtil.getFessConfig().getApiJsonResponseHeaderList().forEach(e -> response.setHeader(e.getFirst(), e.getSecond()));
    }

    /**
     * Sets the response MIME type. Propagated to every handler so that operator overrides via DI
     * actually take effect.
     *
     * @param mimeType the MIME type string (without charset suffix)
     */
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
        for (final ApiHandler h : handlers) {
            if (h instanceof final AbstractApiHandler a) {
                a.setMimeType(mimeType);
            }
        }
    }
}
