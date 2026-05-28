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
import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.entity.SearchRequestParams.SearchRequestType;
import org.codelibs.fess.plugin.webapp.v1_api.UnitWebappTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Unit tests for {@link SuggestRequestParameter}.
 */
public class SuggestRequestParameterTest extends UnitWebappTestCase {

    @Override
    protected void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    }

    // ------------------------------------------------------------------
    // parse() — basic field extraction
    // ------------------------------------------------------------------

    @Test
    public void test_parse_basicQuery() {
        final MockReq req = new MockReq();
        req.setParameter("q", "fess");
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertEquals("fess", params.getQuery());
    }

    @Test
    public void test_parse_nullQuery_returnsNull() {
        final MockReq req = new MockReq();
        // "q" not set
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertNull(params.getQuery());
    }

    @Test
    public void test_parse_numericNum() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        req.setParameter("num", "5");
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertEquals(5, params.getNum());
    }

    @Test
    public void test_parse_numDefault_whenNumAbsent() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        // "num" not set → default 10
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertEquals(10, params.getNum());
    }

    @Test
    public void test_parse_numDefault_whenNumNonNumeric() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        req.setParameter("num", "abc");
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertEquals(10, params.getNum());
    }

    @Test
    public void test_parse_numDefault_whenNumBlank() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        req.setParameter("num", "  ");
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertEquals(10, params.getNum());
    }

    @Test
    public void test_parse_labelTags() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        req.setParameterValues("label", new String[] { "tag1", "tag2" });
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        final String[] tags = params.getTags();
        assertNotNull(tags);
        assertEquals(2, tags.length);
        assertEquals("tag1", tags[0]);
        assertEquals("tag2", tags[1]);
    }

    @Test
    public void test_parse_noLabels_returnsEmptyArray() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        final String[] tags = params.getTags();
        assertNotNull(tags);
        assertEquals(0, tags.length);
    }

    @Test
    public void test_parse_suggestFields() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        req.setParameterValues("field", new String[] { "title", "content" });
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        final String[] fields = params.getSuggestFields();
        assertNotNull(fields);
        assertEquals(2, fields.length);
        assertEquals("title", fields[0]);
        assertEquals("content", fields[1]);
    }

    @Test
    public void test_parse_noFields_returnsEmptyArray() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        final String[] fields = params.getSuggestFields();
        assertNotNull(fields);
        assertEquals(0, fields.length);
    }

    // ------------------------------------------------------------------
    // getType() — always SUGGEST
    // ------------------------------------------------------------------

    @Test
    public void test_getType_returnsSuggest() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertEquals(SearchRequestType.SUGGEST, params.getType());
    }

    // ------------------------------------------------------------------
    // getFields() and getConditions() — return empty maps
    // ------------------------------------------------------------------

    @Test
    public void test_getFields_returnsEmptyMap() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertTrue(params.getFields().isEmpty());
    }

    @Test
    public void test_getConditions_returnsEmptyMap() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        assertTrue(params.getConditions().isEmpty());
    }

    // ------------------------------------------------------------------
    // Unsupported methods throw UnsupportedOperationException
    // ------------------------------------------------------------------

    @Test
    public void test_getGeoInfo_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getGeoInfo();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getFacetInfo_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getFacetInfo();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getSort_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getSort();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getStartPosition_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getStartPosition();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getOffset_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getOffset();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getPageSize_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getPageSize();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getExtraQueries_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getExtraQueries();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getAttribute_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getAttribute("anything");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getLocale_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getLocale();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void test_getSimilarDocHash_throwsUnsupported() {
        final MockReq req = new MockReq();
        final SuggestRequestParameter params = SuggestRequestParameter.parse(req);
        try {
            params.getSimilarDocHash();
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    // ------------------------------------------------------------------
    // Minimal mock request
    // ------------------------------------------------------------------

    private static class MockReq extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final Map<String, String[]> params = new HashMap<>();

        MockReq() {
            super(new StubServletRequest());
        }

        public void setParameter(final String name, final String value) {
            params.put(name, new String[] { value });
        }

        public void setParameterValues(final String name, final String[] values) {
            params.put(name, values);
        }

        @Override
        public String getParameter(final String name) {
            final String[] vals = params.get(name);
            return (vals != null && vals.length > 0) ? vals[0] : null;
        }

        @Override
        public String[] getParameterValues(final String name) {
            return params.get(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(params);
        }
    }

    private static class StubServletRequest implements jakarta.servlet.http.HttpServletRequest {
        @Override
        public Object getAttribute(final String name) {
            return null;
        }

        @Override
        public java.util.Enumeration<String> getAttributeNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public void setCharacterEncoding(final String env) {
        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public long getContentLengthLong() {
            return 0;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            return null;
        }

        @Override
        public String getParameter(final String name) {
            return null;
        }

        @Override
        public java.util.Enumeration<String> getParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String[] getParameterValues(final String name) {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public java.io.BufferedReader getReader() {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public void setAttribute(final String name, final Object o) {
        }

        @Override
        public void removeAttribute(final String name) {
        }

        @Override
        public java.util.Locale getLocale() {
            return java.util.Locale.ROOT;
        }

        @Override
        public java.util.Enumeration<java.util.Locale> getLocales() {
            return Collections.emptyEnumeration();
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public jakarta.servlet.RequestDispatcher getRequestDispatcher(final String path) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public jakarta.servlet.ServletContext getServletContext() {
            return null;
        }

        @Override
        public jakarta.servlet.AsyncContext startAsync() {
            return null;
        }

        @Override
        public jakarta.servlet.AsyncContext startAsync(final jakarta.servlet.ServletRequest sr, final jakarta.servlet.ServletResponse sr2) {
            return null;
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public jakarta.servlet.AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public jakarta.servlet.DispatcherType getDispatcherType() {
            return null;
        }

        @Override
        public String getRequestId() {
            return null;
        }

        @Override
        public String getProtocolRequestId() {
            return null;
        }

        @Override
        public jakarta.servlet.ServletConnection getServletConnection() {
            return null;
        }

        @Override
        public String getAuthType() {
            return null;
        }

        @Override
        public jakarta.servlet.http.Cookie[] getCookies() {
            return null;
        }

        @Override
        public long getDateHeader(final String name) {
            return 0;
        }

        @Override
        public String getHeader(final String name) {
            return null;
        }

        @Override
        public java.util.Enumeration<String> getHeaders(final String name) {
            return Collections.emptyEnumeration();
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public int getIntHeader(final String name) {
            return 0;
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return null;
        }

        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return false;
        }

        @Override
        public java.security.Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public StringBuffer getRequestURL() {
            return null;
        }

        @Override
        public String getServletPath() {
            return null;
        }

        @Override
        public jakarta.servlet.http.HttpSession getSession(final boolean create) {
            return null;
        }

        @Override
        public jakarta.servlet.http.HttpSession getSession() {
            return null;
        }

        @Override
        public String changeSessionId() {
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        @Override
        public boolean authenticate(final jakarta.servlet.http.HttpServletResponse response) {
            return false;
        }

        @Override
        public void login(final String username, final String password) {
        }

        @Override
        public void logout() {
        }

        @Override
        public java.util.Collection<jakarta.servlet.http.Part> getParts() {
            return Collections.emptyList();
        }

        @Override
        public jakarta.servlet.http.Part getPart(final String name) {
            return null;
        }

        @Override
        public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(final Class<T> handlerClass) {
            return null;
        }
    }
}
