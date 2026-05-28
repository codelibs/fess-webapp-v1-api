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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.chat.ChatClient.ChatResult;
import org.codelibs.fess.chat.ChatPhaseCallback;
import org.codelibs.fess.entity.ChatMessage.ChatSource;
import org.codelibs.fess.helper.SseResponseHelper;
import org.codelibs.fess.llm.LlmException;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code GET /api/v1/chat/stream} or {@code POST /api/v1/chat/stream}
 * streaming RAG chat requests using Server-Sent Events.
 */
public class ChatStreamHandler extends AbstractChatHandler {

    private static final Logger logger = LogManager.getLogger(ChatStreamHandler.class);

    private static final String PATH = "/api/v1/chat/stream";

    /** Payload keys reserved by the SSE protocol; must not be overridden by phase callback metadata. */
    private static final Set<String> RESERVED_PAYLOAD_KEYS = Set.of("phase", "status");

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public ChatStreamHandler() {
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
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"POST".equalsIgnoreCase(request.getMethod())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid method for stream request. method={}, expected GET or POST", request.getMethod());
            }
            writeJsonResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, createErrorResponse("Method not allowed"));
            return;
        }

        final String message = request.getParameter("message");
        final String sessionId = request.getParameter("sessionId");

        if (logger.isDebugEnabled()) {
            logger.debug("Processing stream request. sessionId={}, messageLength={}", sessionId, message != null ? message.length() : 0);
        }

        if (StringUtil.isBlank(message)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Message is required but was empty for stream request");
            }
            writeJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, createErrorResponse("Message is required"));
            return;
        }

        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final int maxMessageLength = getMaxMessageLength(fessConfig);
        if (message.length() > maxMessageLength) {
            logger.warn("Stream message exceeds max length. length={}, max={}", message.length(), maxMessageLength);
            writeJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    createErrorResponse("Message is too long (max " + maxMessageLength + " characters)"));
            return;
        }

        // Set SSE headers (shared with v2 ChatStreamHandler via SseResponseHelper) PLUS the operator-
        // configured api.json.response.headers (CORS, security headers) — SseResponseHelper only
        // emits SSE-specific headers.
        SseResponseHelper.applySseHeaders(response);
        applyJsonResponseHeaders(response);

        // Do NOT wrap the writer in try-with-resources: the servlet container manages the
        // response lifecycle, and closing here would prevent the error path below from
        // reusing the writer after an exception aborts the streaming body.
        final PrintWriter writer = response.getWriter();
        try {
            final String userId = getUserId(request);

            // Set LLM type name as Access Type for search log
            request.setAttribute(Constants.SEARCH_LOG_ACCESS_TYPE,
                    ComponentUtil.getFessConfig().getSystemProperty("rag.llm.name", "ollama"));

            // Create phase callback for SSE events
            final ChatPhaseCallback phaseCallback = new ChatPhaseCallback() {
                @Override
                public void onPhaseStart(final String phase, final String phaseMessage) {
                    onPhaseStart(phase, phaseMessage, null);
                }

                @Override
                public void onPhaseStart(final String phase, final String phaseMessage, final String keywords) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("phase", phase);
                    data.put("status", "start");
                    data.put("message", phaseMessage);
                    putIfNotNull(data, "keywords", keywords);
                    emitSseEventSafely(writer, "phase", data);
                }

                @Override
                public void onPhaseComplete(final String phase) {
                    onPhaseComplete(phase, java.util.Collections.emptyMap());
                }

                @Override
                public void onPhaseComplete(final String phase, final Map<String, Object> payload) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("phase", phase);
                    data.put("status", "complete");
                    if (payload != null) {
                        payload.forEach((k, v) -> {
                            if (v != null && !RESERVED_PAYLOAD_KEYS.contains(k)) {
                                data.put(k, v);
                            }
                        });
                    }
                    emitSseEventSafely(writer, "phase", data);
                }

                @Override
                public void onChunk(final String content, final boolean done) {
                    if (content != null && !content.isEmpty()) {
                        emitSseEventSafely(writer, "chunk", Map.of("content", content));
                    }
                }

                @Override
                public void onError(final String phase, final String errorCode) {
                    emitSseEventSafely(writer, "error", Map.of("phase", phase, "message", errorCode, "errorCode", errorCode));
                }

                @Override
                public void onRetry(final String phase, final String operation, final int attempt, final int maxAttempts,
                        final long sleepMs, final String cause) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("phase", phase);
                    data.put("operation", operation);
                    data.put("attempt", attempt);
                    data.put("maxAttempts", maxAttempts);
                    data.put("sleepMs", sleepMs);
                    putIfNotNull(data, "cause", cause);
                    emitSseEventSafely(writer, "retry", data);
                }

                @Override
                public void onWaiting(final String phase, final String reason, final long elapsedMs, final long timeoutMs) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("phase", phase);
                    data.put("reason", reason);
                    data.put("elapsedMs", elapsedMs);
                    data.put("timeoutMs", timeoutMs);
                    emitSseEventSafely(writer, "waiting", data);
                }

                @Override
                public void onFallback(final String phase, final String reason, final String originalQuery, final String newQuery) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("phase", phase);
                    data.put("reason", reason);
                    putIfNotNull(data, "originalQuery", originalQuery);
                    putIfNotNull(data, "newQuery", newQuery);
                    emitSseEventSafely(writer, "fallback", data);
                }

                @Override
                public void onWarning(final String phase, final String code, final String detail) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("phase", phase);
                    data.put("code", code);
                    putIfNotNull(data, "detail", detail);
                    emitSseEventSafely(writer, "warning", data);
                }
            };

            // Parse filter parameters
            final Map<String, String[]> fields = parseFieldFilters(request);
            final String[] extraQueries = parseExtraQueries(request);

            // Stream the response using enhanced flow (use legacy method when no filters for backward compatibility)
            final ChatResult result;
            if (fields.isEmpty() && extraQueries.length == 0) {
                result = ComponentUtil.getChatClient().streamChatEnhanced(sessionId, message, userId, phaseCallback);
            } else {
                result = ComponentUtil.getChatClient().streamChatEnhanced(sessionId, message, userId, fields, extraQueries, phaseCallback);
            }

            // Send sources
            final List<ChatSource> sources = result.getMessage().getSources();
            if (sources != null && !sources.isEmpty()) {
                sendSseEvent(writer, "sources", Map.of("sources", sources));
                if (logger.isDebugEnabled()) {
                    logger.debug("SSE sources event sent. sourcesCount={}", sources.size());
                }
            }

            // Send completion event with HTML content
            final Map<String, Object> doneData = new HashMap<>();
            doneData.put("sessionId", result.getSessionId());
            final String htmlContent = result.getMessage().getHtmlContent();
            if (htmlContent != null) {
                doneData.put("htmlContent", htmlContent);
            }
            sendSseEvent(writer, "done", doneData);
            if (logger.isDebugEnabled()) {
                logger.debug("SSE stream completed. sessionId={}, hasHtmlContent={}", result.getSessionId(), htmlContent != null);
            }

        } catch (final LlmException e) {
            // LlmException from streamChatEnhanced already sent onError via callback — avoid double-send
            logger.warn("LLM error during stream request. sessionId={}, errorCode={}, message={}", sessionId, e.getErrorCode(),
                    e.getMessage(), e);
        } catch (final Exception e) {
            logger.warn("[RAG] Failed to process stream request. sessionId={}, message={}", sessionId, e.getMessage(), e);
            if (!response.isCommitted()) {
                try {
                    sendSseEvent(writer, "error", Map.of("message", "Internal server error", "errorCode", LlmException.ERROR_UNKNOWN));
                } catch (final Exception ioe) {
                    logger.warn("Failed to send error response. error={}", ioe.getMessage());
                }
            }
        }
    }

    /**
     * Sends an SSE event with broad exception handling and debug logging on success or failure.
     * Use for phase-callback emitters that should never break the streaming flow on a callback fault.
     *
     * @param writer the print writer to write the event to
     * @param event the event name
     * @param data the event data to serialize as JSON
     */
    protected void emitSseEventSafely(final PrintWriter writer, final String event, final Map<String, Object> data) {
        try {
            sendSseEvent(writer, event, data);
            if (logger.isDebugEnabled()) {
                logger.debug("SSE {} event sent. data={}", event, data);
            }
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to send {} event. error={}", event, e.getMessage());
            }
        }
    }

    /**
     * Sends a Server-Sent Event (SSE) to the client.
     *
     * @param writer the print writer to write the event to
     * @param event the event name
     * @param data the event data to serialize as JSON
     */
    public void sendSseEvent(final PrintWriter writer, final String event, final Map<String, Object> data) {
        try {
            writer.write("event: " + event + "\n");
            writer.write("data: " + objectMapper.writeValueAsString(data) + "\n\n");
            writer.flush();
        } catch (final JsonProcessingException e) {
            logger.warn("[RAG] Failed to serialize SSE data. event={}", event, e);
        }
    }

    /**
     * Puts {@code value} into {@code data} under {@code key} only if {@code value} is non-null.
     *
     * @param data the destination map
     * @param key the key to write
     * @param value the value to write (skipped if null)
     */
    protected static void putIfNotNull(final Map<String, Object> data, final String key, final Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }
}
