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
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.chat.ChatClient.ChatResult;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code POST /api/v1/chat} non-streaming RAG chat requests.
 */
public class ChatHandler extends AbstractChatHandler {

    private static final Logger logger = LogManager.getLogger(ChatHandler.class);

    private static final String PATH = "/api/v1/chat";

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public ChatHandler() {
        // no-op
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        if (!ComponentUtil.getFessConfig().isRagChatEnabled()) {
            return false;
        }
        final String servletPath = request.getServletPath();
        return PATH.equals(servletPath) || (PATH + "/").equals(servletPath);
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid method for chat request. method={}", request.getMethod());
            }
            writeJsonResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, createErrorResponse("Method not allowed"));
            return;
        }

        try {
            final String message = request.getParameter("message");
            final String sessionId = request.getParameter("sessionId");
            final String clearParam = request.getParameter("clear");

            if (logger.isDebugEnabled()) {
                logger.debug("Processing chat request. sessionId={}, messageLength={}, clear={}", sessionId,
                        message != null ? message.length() : 0, clearParam);
            }

            if (StringUtil.isBlank(message)) {
                if ("true".equals(clearParam) && StringUtil.isNotBlank(sessionId)) {
                    final String clearUserId = getUserId(request);
                    final boolean cleared = ComponentUtil.getChatSessionManager().clearSession(sessionId, clearUserId);
                    if (cleared) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Session cleared. sessionId={}, userId={}", sessionId, clearUserId);
                        }
                        writeJsonResponse(response, HttpServletResponse.SC_OK, createSuccessResponse(sessionId, "Session cleared", null));
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Session not found or not owned. sessionId={}, userId={}", sessionId, clearUserId);
                        }
                        writeJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, createErrorResponse("Session not found"));
                    }
                    return;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Message is required but was empty");
                }
                writeJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, createErrorResponse("Message is required"));
                return;
            }

            final FessConfig fessConfig = ComponentUtil.getFessConfig();
            final int maxMessageLength = getMaxMessageLength(fessConfig);
            if (message.length() > maxMessageLength) {
                logger.warn("Chat message exceeds max length. length={}, max={}", message.length(), maxMessageLength);
                writeJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        createErrorResponse("Message is too long (max " + maxMessageLength + " characters)"));
                return;
            }

            final String userId = getUserId(request);

            // Set LLM type name as Access Type for search log
            request.setAttribute(Constants.SEARCH_LOG_ACCESS_TYPE,
                    ComponentUtil.getFessConfig().getSystemProperty("rag.llm.name", "ollama"));

            final Map<String, String[]> fields = parseFieldFilters(request);
            final String[] extraQueries = parseExtraQueries(request);
            final ChatResult result;
            if (fields.isEmpty() && extraQueries.length == 0) {
                result = ComponentUtil.getChatClient().chat(sessionId, message, userId);
            } else {
                result = ComponentUtil.getChatClient().chat(sessionId, message, userId, fields, extraQueries);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Chat request completed. sessionId={}, responseLength={}", result.getSessionId(),
                        result.getMessage().getContent() != null ? result.getMessage().getContent().length() : 0);
            }

            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    createSuccessResponse(result.getSessionId(), result.getMessage().getContent(), result.getMessage().getSources()));

        } catch (final Exception e) {
            logger.warn("[RAG] Failed to process chat request. message={}", e.getMessage(), e);
            writeJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, createErrorResponse("Internal server error"));
        }
    }
}
