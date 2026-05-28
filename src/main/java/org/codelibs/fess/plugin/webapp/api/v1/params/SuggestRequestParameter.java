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
package org.codelibs.fess.plugin.webapp.api.v1.params;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.entity.FacetInfo;
import org.codelibs.fess.entity.GeoInfo;
import org.codelibs.fess.entity.HighlightInfo;
import org.codelibs.fess.entity.SearchRequestParams;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Search request parameters for the suggest API endpoint.
 */
public class SuggestRequestParameter extends SearchRequestParams {

    private final String query;

    private final String[] fields;

    private final int num;

    private final HttpServletRequest request;

    private final String[] tags;

    /**
     * Constructor for SuggestRequestParameter.
     *
     * @param request the HTTP servlet request
     * @param query the search query string
     * @param tags array of tags to filter suggestions
     * @param fields array of fields to search in for suggestions
     * @param num the maximum number of suggestions to return
     */
    protected SuggestRequestParameter(final HttpServletRequest request, final String query, final String[] tags, final String[] fields,
            final int num) {
        this.query = query;
        this.tags = tags;
        this.fields = fields;
        this.num = num;
        this.request = request;
    }

    /**
     * Parses the HTTP request to create a SuggestRequestParameter object.
     *
     * @param request the HTTP servlet request containing the parameters
     * @return a new SuggestRequestParameter object with parsed values
     */
    public static SuggestRequestParameter parse(final HttpServletRequest request) {
        final String query = request.getParameter("q");
        final String[] tags = getParamValueArray(request, "label");
        final String[] fields = getParamValueArray(request, "field");

        final String numStr = request.getParameter("num");
        final int num;
        if (StringUtil.isNotBlank(numStr) && StringUtils.isNumeric(numStr)) {
            num = Integer.parseInt(numStr);
        } else {
            num = 10;
        }

        return new SuggestRequestParameter(request, query, tags, fields, num);
    }

    @Override
    public String getQuery() {
        return query;
    }

    /**
     * Gets the suggest fields for the request.
     *
     * @return array of field names to search in for suggestions
     */
    public String[] getSuggestFields() {
        return fields;
    }

    /**
     * Gets the maximum number of suggestions to return.
     *
     * @return the maximum number of suggestions
     */
    public int getNum() {
        return num;
    }

    @Override
    public Map<String, String[]> getFields() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String[]> getConditions() {
        return Collections.emptyMap();
    }

    /**
     * Gets the tags for filtering suggestions.
     *
     * @return array of tags used to filter suggestions
     */
    public String[] getTags() {
        return tags;
    }

    @Override
    public String[] getLanguages() {
        return getParamValueArray(request, "lang");
    }

    @Override
    public GeoInfo getGeoInfo() {
        throw new UnsupportedOperationException("getGeoInfo() is not supported in this implementation");
    }

    @Override
    public FacetInfo getFacetInfo() {
        throw new UnsupportedOperationException("getFacetInfo() is not supported in this implementation");
    }

    @Override
    public String getSort() {
        throw new UnsupportedOperationException("getSort() is not supported in this implementation");
    }

    @Override
    public int getStartPosition() {
        throw new UnsupportedOperationException("getStartPosition() is not supported in this implementation");
    }

    @Override
    public int getOffset() {
        throw new UnsupportedOperationException("getOffset() is not supported in this implementation");
    }

    @Override
    public int getPageSize() {
        throw new UnsupportedOperationException("getPageSize() is not supported in this implementation");
    }

    @Override
    public String[] getExtraQueries() {
        throw new UnsupportedOperationException("getExtraQueries() is not supported in this implementation");
    }

    @Override
    public Object getAttribute(final String name) {
        throw new UnsupportedOperationException("getAttribute() is not supported in this implementation");
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException("getLocale() is not supported in this implementation");
    }

    @Override
    public SearchRequestType getType() {
        return SearchRequestType.SUGGEST;
    }

    @Override
    public String getSimilarDocHash() {
        throw new UnsupportedOperationException("getSimilarDocHash() is not supported in this implementation");
    }

    @Override
    public HighlightInfo getHighlightInfo() {
        return new HighlightInfo();
    }
}
