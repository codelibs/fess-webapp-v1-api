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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.entity.SearchRenderData;
import org.codelibs.fess.exception.InvalidQueryException;
import org.codelibs.fess.exception.ResultOffsetExceededException;
import org.codelibs.fess.helper.RelatedContentHelper;
import org.codelibs.fess.helper.RelatedQueryHelper;
import org.codelibs.fess.helper.SearchHelper;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.plugin.webapp.api.v1.params.JsonRequestParams;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.FacetResponse;
import org.codelibs.fess.util.FacetResponse.Field;
import org.dbflute.optional.OptionalThing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles {@code GET /api/v1/documents} search requests.
 * This is the fallback handler for the {@code /api/v1/documents} endpoint.
 */
public class SearchHandler extends AbstractApiHandler {

    private static final Logger logger = LogManager.getLogger(SearchHandler.class);

    private static final String TYPE = "documents";

    /**
     * Default constructor. The handler is stateless and intended to be
     * instantiated once by the API manager and shared across concurrent requests.
     */
    public SearchHandler() {
        // no-op
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        // Mirrors the original detectFormatType: an absent type segment (e.g. /api/v1) used to
        // return FormatType.SEARCH; an explicit "documents" type also returns SEARCH.
        final String[] segments = splitPath(request);
        if (segments.length <= 3) {
            return true;
        }
        return isTypeSegment(request, TYPE);
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!acceptHttpMethod(request, GET)) {
            return;
        }

        final SearchHelper searchHelper = ComponentUtil.getSearchHelper();
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final RelatedQueryHelper relatedQueryHelper = ComponentUtil.getRelatedQueryHelper();
        final RelatedContentHelper relatedContentHelper = ComponentUtil.getRelatedContentHelper();

        String query = null;
        final StringBuilder buf = new StringBuilder(1000);
        request.setAttribute(Constants.SEARCH_LOG_ACCESS_TYPE, Constants.SEARCH_LOG_ACCESS_TYPE_JSON);
        try {
            final SearchRenderData data = new SearchRenderData();
            final JsonRequestParams params = new JsonRequestParams(request, fessConfig);
            query = params.getQuery();
            searchHelper.search(params, data, OptionalThing.empty());
            final String execTime = data.getExecTime();
            final String queryTime = Long.toString(data.getQueryTime());
            final String pageSize = Integer.toString(data.getPageSize());
            final String currentPageNumber = Integer.toString(data.getCurrentPageNumber());
            final String allRecordCount = Long.toString(data.getAllRecordCount());
            final String allRecordCountRelation = data.getAllRecordCountRelation();
            final String allPageCount = Integer.toString(data.getAllPageCount());
            final List<Map<String, Object>> documentItems = data.getDocumentItems();
            final FacetResponse facetResponse = data.getFacetResponse();
            final String queryId = data.getQueryId();
            final String highlightParams = data.getAppendHighlightParams();
            final boolean nextPage = data.isExistNextPage();
            final boolean prevPage = data.isExistPrevPage();
            final long startRecordNumber = data.getCurrentStartRecordNumber();
            final long endRecordNumber = data.getCurrentEndRecordNumber();
            final List<String> pageNumbers = data.getPageNumberList();
            final boolean partial = data.isPartialResults();
            final String searchQuery = data.getSearchQuery();
            final long requestedTime = data.getRequestedTime();

            buf.append("\"q\":");
            buf.append(escapeJson(query));
            buf.append(",\"query_id\":");
            buf.append(escapeJson(queryId));
            buf.append(",\"exec_time\":");
            buf.append(execTime);
            buf.append(",\"query_time\":");
            buf.append(queryTime);
            buf.append(',');
            buf.append("\"page_size\":");
            buf.append(pageSize);
            buf.append(',');
            buf.append("\"page_number\":");
            buf.append(currentPageNumber);
            buf.append(',');
            buf.append("\"record_count\":");
            buf.append(allRecordCount);
            buf.append(',');
            buf.append("\"record_count_relation\":");
            buf.append(escapeJson(allRecordCountRelation));
            buf.append(',');
            buf.append("\"page_count\":");
            buf.append(allPageCount);
            buf.append(",\"highlight_params\":");
            buf.append(escapeJson(highlightParams));
            buf.append(",\"next_page\":");
            buf.append(escapeJson(nextPage));
            buf.append(",\"prev_page\":");
            buf.append(escapeJson(prevPage));
            buf.append(",\"start_record_number\":");
            buf.append(startRecordNumber);
            buf.append(",\"end_record_number\":");
            buf.append(escapeJson(endRecordNumber));
            buf.append(",\"page_numbers\":");
            buf.append(escapeJson(pageNumbers));
            buf.append(",\"partial\":");
            buf.append(escapeJson(partial));
            buf.append(",\"search_query\":");
            buf.append(escapeJson(searchQuery));
            buf.append(",\"requested_time\":");
            buf.append(requestedTime);
            final String[] relatedQueries = relatedQueryHelper.getRelatedQueries(query);
            buf.append(",\"related_query\":");
            buf.append(escapeJson(relatedQueries));
            final String[] relatedContents = relatedContentHelper.getRelatedContents(query);
            buf.append(",\"related_contents\":");
            buf.append(escapeJson(relatedContents));
            buf.append(',');
            buf.append("\"data\":[");
            if (!documentItems.isEmpty()) {
                boolean first1 = true;
                for (final Map<String, Object> document : documentItems) {
                    if (!first1) {
                        buf.append(',');
                    } else {
                        first1 = false;
                    }
                    buf.append('{');
                    boolean first2 = true;
                    for (final Map.Entry<String, Object> entry : document.entrySet()) {
                        final String name = entry.getKey();
                        if (StringUtil.isNotBlank(name) && entry.getValue() != null
                                && ComponentUtil.getQueryFieldConfig().isApiResponseField(name)) {
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
                }
            }
            buf.append(']');
            if (facetResponse != null && facetResponse.hasFacetResponse()) {
                // facet field
                buf.append(',');
                buf.append("\"facet_field\":[");
                if (facetResponse.getFieldList() != null) {
                    boolean first1 = true;
                    for (final Field field : facetResponse.getFieldList()) {
                        if (!first1) {
                            buf.append(',');
                        } else {
                            first1 = false;
                        }
                        buf.append("{\"name\":");
                        buf.append(escapeJson(field.getName()));
                        buf.append(",\"result\":[");
                        boolean first2 = true;
                        for (final Map.Entry<String, Long> entry : field.getValueCountMap().entrySet()) {
                            if (!first2) {
                                buf.append(',');
                            } else {
                                first2 = false;
                            }
                            buf.append("{\"value\":");
                            buf.append(escapeJson(entry.getKey()));
                            buf.append(",\"count\":");
                            buf.append(entry.getValue());
                            buf.append('}');
                        }
                        buf.append(']');
                        buf.append('}');
                    }
                }
                buf.append(']');
                // facet q
                buf.append(',');
                buf.append("\"facet_query\":[");
                if (facetResponse.getQueryCountMap() != null) {
                    boolean first1 = true;
                    for (final Map.Entry<String, Long> entry : facetResponse.getQueryCountMap().entrySet()) {
                        if (!first1) {
                            buf.append(',');
                        } else {
                            first1 = false;
                        }
                        buf.append("{\"value\":");
                        buf.append(escapeJson(entry.getKey()));
                        buf.append(",\"count\":");
                        buf.append(entry.getValue());
                        buf.append('}');
                    }
                }
                buf.append(']');
            }
            writeJsonResponse(HttpServletResponse.SC_OK, buf.toString());
        } catch (final InvalidQueryException | ResultOffsetExceededException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a search request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_BAD_REQUEST, e);
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process a search request.", e);
            }
            writeJsonResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
}
