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
import java.util.Locale;
import java.util.Map;

import org.codelibs.fess.entity.SearchRequestParams.SearchRequestType;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.plugin.webapp.v1_api.UnitWebappTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Unit tests for {@link JsonRequestParams}.
 *
 * <p>Only fields that do not require the full Fess search stack are tested here
 * (query parsing, start/offset/pageSize clamping, fields/conditions extraction).
 */
public class JsonRequestParamsTest extends UnitWebappTestCase {

    /** Minimal FessConfig stub used across all tests. */
    private static final FessConfig STUB_CONFIG = new FessConfig.SimpleImpl() {
        private static final long serialVersionUID = 1L;

        @Override
        public Integer getPagingSearchPageStartAsInteger() {
            return 0;
        }

        @Override
        public Integer getPagingSearchPageSizeAsInteger() {
            return 20;
        }

        @Override
        public Integer getPagingSearchPageMaxSizeAsInteger() {
            return 100;
        }
    };

    @Override
    protected void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    }

    // ------------------------------------------------------------------
    // getQuery()
    // ------------------------------------------------------------------

    @Test
    public void test_getQuery_returnsParam() {
        final MockReq req = new MockReq();
        req.setParameter("q", "fess search");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals("fess search", params.getQuery());
    }

    @Test
    public void test_getQuery_null_whenAbsent() {
        final MockReq req = new MockReq();
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertNull(params.getQuery());
    }

    // ------------------------------------------------------------------
    // getStartPosition()
    // ------------------------------------------------------------------

    @Test
    public void test_getStartPosition_validInteger() {
        final MockReq req = new MockReq();
        req.setParameter("start", "10");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(10, params.getStartPosition());
    }

    @Test
    public void test_getStartPosition_fallsBackToDefault_whenAbsent() {
        final MockReq req = new MockReq();
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(0, params.getStartPosition()); // default from config
    }

    @Test
    public void test_getStartPosition_fallsBackToDefault_whenNonNumeric() {
        final MockReq req = new MockReq();
        req.setParameter("start", "abc");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(0, params.getStartPosition());
    }

    @Test
    public void test_getStartPosition_cached_secondCallReturnsSameValue() {
        final MockReq req = new MockReq();
        req.setParameter("start", "5");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(5, params.getStartPosition());
        // mutate param — cached value should still be returned
        req.setParameter("start", "99");
        assertEquals(5, params.getStartPosition());
    }

    // ------------------------------------------------------------------
    // getOffset()
    // ------------------------------------------------------------------

    @Test
    public void test_getOffset_validInteger() {
        final MockReq req = new MockReq();
        req.setParameter("offset", "3");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(3, params.getOffset());
    }

    @Test
    public void test_getOffset_fallsBackToZero_whenAbsent() {
        final MockReq req = new MockReq();
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(0, params.getOffset());
    }

    @Test
    public void test_getOffset_fallsBackToZero_whenNonNumeric() {
        final MockReq req = new MockReq();
        req.setParameter("offset", "bad");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(0, params.getOffset());
    }

    @Test
    public void test_getOffset_cached() {
        final MockReq req = new MockReq();
        req.setParameter("offset", "7");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(7, params.getOffset());
        req.setParameter("offset", "99");
        assertEquals(7, params.getOffset());
    }

    // ------------------------------------------------------------------
    // getPageSize()
    // ------------------------------------------------------------------

    @Test
    public void test_getPageSize_validInteger() {
        final MockReq req = new MockReq();
        req.setParameter("num", "10");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(10, params.getPageSize());
    }

    @Test
    public void test_getPageSize_fallsBackToDefault_whenAbsent() {
        final MockReq req = new MockReq();
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(20, params.getPageSize()); // config default
    }

    @Test
    public void test_getPageSize_fallsBackToDefault_whenNonNumeric() {
        final MockReq req = new MockReq();
        req.setParameter("num", "nope");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(20, params.getPageSize());
    }

    @Test
    public void test_getPageSize_clampedToMax_whenExceedsMax() {
        final MockReq req = new MockReq();
        req.setParameter("num", "999");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(100, params.getPageSize()); // clamped to max
    }

    @Test
    public void test_getPageSize_clampedToMax_whenZero() {
        final MockReq req = new MockReq();
        req.setParameter("num", "0");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(100, params.getPageSize()); // 0 is invalid → max
    }

    @Test
    public void test_getPageSize_exactlyMax_accepted() {
        final MockReq req = new MockReq();
        req.setParameter("num", "100");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(100, params.getPageSize());
    }

    @Test
    public void test_getPageSize_cached() {
        final MockReq req = new MockReq();
        req.setParameter("num", "15");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(15, params.getPageSize());
        req.setParameter("num", "99");
        assertEquals(15, params.getPageSize());
    }

    // ------------------------------------------------------------------
    // getFields()
    // ------------------------------------------------------------------

    @Test
    public void test_getFields_extractsFieldsDotPrefix() {
        final MockReq req = new MockReq();
        req.setParameterValues("fields.label", new String[] { "tag1", "tag2" });
        req.setParameterValues("fields.category", new String[] { "catA" });
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        final Map<String, String[]> fields = params.getFields();
        assertNotNull(fields);
        assertTrue(fields.containsKey("label"));
        assertTrue(fields.containsKey("category"));
    }

    @Test
    public void test_getFields_emptyWhenNoFieldsParams() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertTrue(params.getFields().isEmpty());
    }

    @Test
    public void test_getFields_doesNotIncludeNonFieldsParams() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        req.setParameterValues("fields.label", new String[] { "tagA" });
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        final Map<String, String[]> fields = params.getFields();
        assertFalse(fields.containsKey("q"));
        assertTrue(fields.containsKey("label"));
    }

    // ------------------------------------------------------------------
    // getConditions()
    // ------------------------------------------------------------------

    @Test
    public void test_getConditions_extractsAsDotPrefix() {
        final MockReq req = new MockReq();
        req.setParameterValues("as.title", new String[] { "fess" });
        req.setParameterValues("as.content", new String[] { "search" });
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        final Map<String, String[]> conds = params.getConditions();
        assertNotNull(conds);
        assertTrue(conds.containsKey("title"));
        assertTrue(conds.containsKey("content"));
    }

    @Test
    public void test_getConditions_emptyWhenNoAsParams() {
        final MockReq req = new MockReq();
        req.setParameter("q", "test");
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertTrue(params.getConditions().isEmpty());
    }

    // ------------------------------------------------------------------
    // getType()
    // ------------------------------------------------------------------

    @Test
    public void test_getType_isJson() {
        final MockReq req = new MockReq();
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(SearchRequestType.JSON, params.getType());
    }

    // ------------------------------------------------------------------
    // getLocale()
    // ------------------------------------------------------------------

    @Test
    public void test_getLocale_isRoot() {
        final MockReq req = new MockReq();
        final JsonRequestParams params = new JsonRequestParams(req, STUB_CONFIG);
        assertEquals(Locale.ROOT, params.getLocale());
    }

    // ------------------------------------------------------------------
    // Minimal mock request
    // ------------------------------------------------------------------

    private static class MockReq extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final Map<String, String[]> params = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();

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

        @Override
        public Object getAttribute(final String name) {
            return attributes.get(name);
        }

        @Override
        public void setAttribute(final String name, final Object o) {
            attributes.put(name, o);
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
            return Locale.ROOT;
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
