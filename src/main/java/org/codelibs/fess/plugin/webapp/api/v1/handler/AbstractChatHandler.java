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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.Constants;
import org.codelibs.fess.entity.ChatMessage.ChatSource;
import org.codelibs.fess.entity.FacetQueryView;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.helper.SystemHelper;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Shared base for chat API handlers, providing Jackson-based JSON serialisation,
 * session helpers, and request parameter parsing.
 */
public abstract class AbstractChatHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(AbstractChatHandler.class);

    /** Jackson mapper shared across all chat handler instances. */
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    /** Default maximum chat message length when the config key is absent or invalid. */
    protected static final int MAX_MESSAGE_LENGTH_DEFAULT = 4000;

    /**
     * Default constructor for subclasses.
     */
    protected AbstractChatHandler() {
        // no-op
    }

    /**
     * Writes a JSON response using Jackson serialisation.
     *
     * @param response the HTTP response
     * @param status the HTTP status code
     * @param data the map to serialise as JSON
     * @throws IOException if an I/O error occurs
     */
    protected void writeJsonResponse(final HttpServletResponse response, final int status, final Map<String, Object> data)
            throws IOException {
        response.setStatus(status);
        response.setContentType(mimeType + "; charset=UTF-8");
        applyJsonResponseHeaders(response);
        response.getWriter().write(objectMapper.writeValueAsString(data));
    }

    /**
     * Creates a success response map.
     *
     * @param sessionId the session ID
     * @param content the response content
     * @param sources the list of chat sources
     * @return a map containing the success response data
     */
    public Map<String, Object> createSuccessResponse(final String sessionId, final String content, final List<ChatSource> sources) {
        final Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("sessionId", sessionId);
        result.put("content", content);
        if (sources != null) {
            result.put("sources", sources);
        }
        return result;
    }

    /**
     * Creates an error response map.
     *
     * @param message the error message
     * @return a map containing the error response data
     */
    public Map<String, Object> createErrorResponse(final String message) {
        final Map<String, Object> result = new HashMap<>();
        result.put("status", "error");
        result.put("message", message);
        return result;
    }

    /**
     * Gets the user ID from the request.
     * Returns a username for authenticated users or a cookie-based code for guests.
     *
     * @param request the HTTP request
     * @return the user ID, or null if the user is a guest with no session
     */
    protected String getUserId(final HttpServletRequest request) {
        final SystemHelper systemHelper = ComponentUtil.getSystemHelper();
        final String username = systemHelper.getUsername();
        if (!Constants.GUEST_USER.equals(username)) {
            return username;
        }
        return ComponentUtil.getUserInfoHelper().getUserCode();
    }

    /**
     * Returns the maximum message length for chat messages.
     *
     * @param fessConfig the Fess configuration
     * @return the maximum message length
     */
    public int getMaxMessageLength(final FessConfig fessConfig) {
        try {
            return Integer.parseInt(fessConfig.getOrDefault("rag.chat.message.max.length", "4000"));
        } catch (final NumberFormatException e) {
            logger.warn("Invalid rag.chat.message.max.length config, using default {}", MAX_MESSAGE_LENGTH_DEFAULT);
            return MAX_MESSAGE_LENGTH_DEFAULT;
        }
    }

    /**
     * Parses and validates field filter parameters from the request.
     * Only configured label values are accepted to prevent query injection.
     *
     * @param request the HTTP request
     * @return a map of field names to their validated filter values
     */
    public Map<String, String[]> parseFieldFilters(final HttpServletRequest request) {
        final Map<String, String[]> fields = new HashMap<>();
        final String[] labels = request.getParameterValues("fields.label");
        if (labels != null && labels.length > 0) {
            // Validate against configured label types (union of request locale and ROOT for robustness)
            final Locale requestLocale = request.getLocale() != null ? request.getLocale() : Locale.ROOT;
            final Set<String> allowedLabels = new java.util.HashSet<>();
            ComponentUtil.getLabelTypeHelper()
                    .getLabelTypeItemList(SearchRequestParams.SearchRequestType.SEARCH, requestLocale)
                    .stream()
                    .map(m -> m.get("value"))
                    .forEach(allowedLabels::add);
            if (!Locale.ROOT.equals(requestLocale)) {
                ComponentUtil.getLabelTypeHelper()
                        .getLabelTypeItemList(SearchRequestParams.SearchRequestType.SEARCH, Locale.ROOT)
                        .stream()
                        .map(m -> m.get("value"))
                        .forEach(allowedLabels::add);
            }
            final List<String> validLabels = new ArrayList<>();
            for (final String label : labels) {
                if (label != null && allowedLabels.contains(label)) {
                    validLabels.add(label);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Rejected unknown label filter value: {}", label);
                }
            }
            if (!validLabels.isEmpty()) {
                fields.put("label", validLabels.toArray(new String[0]));
            }
        }
        return fields;
    }

    /**
     * Parses and validates extra query parameters from the request.
     * Only configured facet query values are accepted to prevent query injection.
     * Values belonging to the same {@link FacetQueryView} are OR-joined into a single clause.
     *
     * @param request the HTTP request
     * @return an array of validated extra query strings
     */
    public String[] parseExtraQueries(final HttpServletRequest request) {
        final String[] extraQueries = request.getParameterValues("ex_q");
        if (extraQueries == null || extraQueries.length == 0) {
            return new String[0];
        }
        // Build allowlist from configured facet queries
        final List<FacetQueryView> facetQueryViewList = ComponentUtil.getViewHelper().getFacetQueryViewList();
        final Set<String> allowedQueries = new java.util.HashSet<>();
        for (final FacetQueryView view : facetQueryViewList) {
            allowedQueries.addAll(view.getQueryMap().values());
        }
        final List<String> validQueries = new ArrayList<>();
        for (final String eq : extraQueries) {
            if (eq != null && allowedQueries.contains(eq)) {
                validQueries.add(eq);
            } else if (logger.isDebugEnabled()) {
                logger.debug("Rejected unknown extra query filter value: {}", eq);
            }
        }
        if (validQueries.isEmpty()) {
            return new String[0];
        }
        // Group validated queries by FacetQueryView and OR-join within the same group
        final Set<String> used = new java.util.HashSet<>();
        final List<String> groupedQueries = new ArrayList<>();
        for (final FacetQueryView view : facetQueryViewList) {
            final Set<String> viewValues = new java.util.HashSet<>(view.getQueryMap().values());
            final List<String> matched = new ArrayList<>();
            for (final String vq : validQueries) {
                if (viewValues.contains(vq)) {
                    matched.add(vq);
                    used.add(vq);
                }
            }
            if (matched.size() == 1) {
                groupedQueries.add(matched.get(0));
            } else if (matched.size() > 1) {
                groupedQueries.add(String.join(" OR ", matched));
            }
        }
        for (final String vq : validQueries) {
            if (!used.contains(vq)) {
                groupedQueries.add(vq);
            }
        }
        return groupedQueries.toArray(new String[0]);
    }
}
