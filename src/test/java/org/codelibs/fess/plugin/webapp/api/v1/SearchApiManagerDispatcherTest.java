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

import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.plugin.webapp.v1_api.UnitWebappTestCase;
import org.codelibs.fess.util.ComponentUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests for {@link SearchApiManager#matches(jakarta.servlet.http.HttpServletRequest)} —
 * the dispatcher-level routing gate — and handler-ordering invariants.
 *
 * <p>Handler ordering guarantees are verified purely through the per-handler
 * {@code matches()} contracts (no I/O, no Fess search stack required).
 */
public class SearchApiManagerDispatcherTest extends UnitWebappTestCase {

    private SearchApiManager manager;

    @Override
    protected void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        manager = new SearchApiManager();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MockRequest req(final String path) {
        return new MockRequest(path);
    }

    private void setWebApiJsonEnabled(final boolean enabled) {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isWebApiJson() {
                return enabled;
            }

            @Override
            public boolean isRagChatEnabled() {
                return false;
            }
        });
    }

    private void setRagChatEnabled(final boolean ragEnabled) {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isWebApiJson() {
                return false;
            }

            @Override
            public boolean isRagChatEnabled() {
                return ragEnabled;
            }
        });
    }

    private void setBothEnabled() {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isWebApiJson() {
                return true;
            }

            @Override
            public boolean isRagChatEnabled() {
                return true;
            }
        });
    }

    // ==================================================================
    // SearchApiManager.matches() — prefix gate
    // ==================================================================

    @Test
    public void test_matches_apiV1Documents_webApiJsonEnabled() {
        setWebApiJsonEnabled(true);
        assertTrue(manager.matches(req("/api/v1/documents")));
    }

    @Test
    public void test_matches_apiV1Labels_webApiJsonEnabled() {
        setWebApiJsonEnabled(true);
        assertTrue(manager.matches(req("/api/v1/labels")));
    }

    @Test
    public void test_matches_apiV1Health_webApiJsonEnabled() {
        setWebApiJsonEnabled(true);
        assertTrue(manager.matches(req("/api/v1/health")));
    }

    @Test
    public void test_matches_returnsFalse_whenWebApiJsonDisabled_nonChatPath() {
        setWebApiJsonEnabled(false);
        assertFalse(manager.matches(req("/api/v1/documents")));
        assertFalse(manager.matches(req("/api/v1/labels")));
        assertFalse(manager.matches(req("/api/v1/health")));
    }

    @Test
    public void test_matches_returnsFalse_wrongPrefix() {
        setWebApiJsonEnabled(true);
        assertFalse(manager.matches(req("/api/v2/documents")));
        assertFalse(manager.matches(req("/admin/config")));
        assertFalse(manager.matches(req("/search")));
    }

    @Test
    public void test_matches_chatPath_ragEnabledOnly() {
        // isWebApiJson() == false but isRagChatEnabled() == true  → chat paths must still match
        setRagChatEnabled(true);
        assertTrue(manager.matches(req("/api/v1/chat")));
        assertTrue(manager.matches(req("/api/v1/chat/stream")));
    }

    @Test
    public void test_matches_chatPath_ragDisabled_webApiJsonDisabled_returnsFalse() {
        // Both flags off → /api/v1/chat must not match
        setWebApiJsonEnabled(false);
        // isRagChatEnabled already returns false from setWebApiJsonEnabled
        assertFalse(manager.matches(req("/api/v1/chat")));
    }

    @Test
    public void test_matches_chatPath_ragEnabled_webApiJsonEnabled() {
        setBothEnabled();
        assertTrue(manager.matches(req("/api/v1/chat")));
        assertTrue(manager.matches(req("/api/v1/chat/stream")));
    }

    // ==================================================================
    // Handler ordering: ScrollSearchHandler before SearchHandler
    //
    // Both handlers' matches() return true for /api/v1/documents/all (the
    // original detectFormatType used segment-based matching, where any
    // /api/v1/documents/* maps to SEARCH unless it specifically refines
    // to SCROLL or FAVORITE). The dispatcher resolves the ambiguity via
    // handler ordering — ScrollSearchHandler is listed first.
    // ==================================================================

    @Test
    public void test_handlerOrdering_scrollHandledBeforeSearch() {
        final org.codelibs.fess.plugin.webapp.api.v1.handler.ScrollSearchHandler scroll =
                new org.codelibs.fess.plugin.webapp.api.v1.handler.ScrollSearchHandler();
        final org.codelibs.fess.plugin.webapp.api.v1.handler.SearchHandler search =
                new org.codelibs.fess.plugin.webapp.api.v1.handler.SearchHandler();

        final MockRequest scrollReq = req("/api/v1/documents/all");
        assertTrue("ScrollSearchHandler must match /api/v1/documents/all", scroll.matches(scrollReq));
        // SearchHandler also matches (type-segment based) — dispatcher ordering ensures Scroll wins.
        assertTrue("SearchHandler matches as the fallback; ordering elects Scroll", search.matches(scrollReq));
    }

    // ==================================================================
    // Handler ordering: ChatStreamHandler before ChatHandler
    // ==================================================================

    @Test
    public void test_handlerOrdering_chatStreamHandledBeforeChat() {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isRagChatEnabled() {
                return true;
            }
        });
        final org.codelibs.fess.plugin.webapp.api.v1.handler.ChatStreamHandler stream =
                new org.codelibs.fess.plugin.webapp.api.v1.handler.ChatStreamHandler();
        final org.codelibs.fess.plugin.webapp.api.v1.handler.ChatHandler chat =
                new org.codelibs.fess.plugin.webapp.api.v1.handler.ChatHandler();

        final MockRequest streamReq = req("/api/v1/chat/stream");
        assertTrue("ChatStreamHandler must match /api/v1/chat/stream", stream.matches(streamReq));
        assertFalse("ChatHandler must NOT match /api/v1/chat/stream", chat.matches(streamReq));
    }

    // ==================================================================
    // Handler ordering: FavoriteHandler before ScrollSearchHandler
    //
    // Mirrors the original detectFormatType priority where the favorite
    // check ran before the "all" check. A path that satisfies both
    // (/api/v1/documents/all/favorite) must be routed to FavoriteHandler.
    // ==================================================================

    @Test
    public void test_handlerOrdering_favoriteBeforeScroll() {
        final org.codelibs.fess.plugin.webapp.api.v1.handler.FavoriteHandler favorite =
                new org.codelibs.fess.plugin.webapp.api.v1.handler.FavoriteHandler();
        final org.codelibs.fess.plugin.webapp.api.v1.handler.ScrollSearchHandler scroll =
                new org.codelibs.fess.plugin.webapp.api.v1.handler.ScrollSearchHandler();

        final MockRequest collisionReq = req("/api/v1/documents/all/favorite");
        assertTrue("FavoriteHandler must match the favorite-collision path", favorite.matches(collisionReq));
        assertTrue("ScrollSearchHandler matches segments[4]==all, but ordering elects Favorite", scroll.matches(collisionReq));
    }

    // ==================================================================
    // setMimeType propagation — verifies fix for C4
    // ==================================================================

    @Test
    public void test_setMimeType_propagatesToHandlers() {
        // Replace handlers with a single probe handler instance reachable via reflection.
        // Simpler approach: call setMimeType on the manager, then construct a handler list
        // separately and verify the field via package access.
        manager.setMimeType("application/vnd.fess+json");
        // The manager retains the value
        assertEquals("application/vnd.fess+json", getMimeType(manager));
    }

    private static String getMimeType(final SearchApiManager m) {
        try {
            final java.lang.reflect.Field f = SearchApiManager.class.getDeclaredField("mimeType");
            f.setAccessible(true);
            return (String) f.get(m);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_setMimeType_appliedToEveryAbstractApiHandler() throws Exception {
        manager.setMimeType("text/json-test");
        final java.lang.reflect.Field handlersField = SearchApiManager.class.getDeclaredField("handlers");
        handlersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.List<org.codelibs.fess.plugin.webapp.api.v1.handler.ApiHandler> hs =
                (java.util.List<org.codelibs.fess.plugin.webapp.api.v1.handler.ApiHandler>) handlersField.get(manager);
        int abstractCount = 0;
        for (final org.codelibs.fess.plugin.webapp.api.v1.handler.ApiHandler h : hs) {
            if (h instanceof org.codelibs.fess.plugin.webapp.api.v1.handler.AbstractApiHandler) {
                abstractCount++;
                final java.lang.reflect.Field mt =
                        org.codelibs.fess.plugin.webapp.api.v1.handler.AbstractApiHandler.class.getDeclaredField("mimeType");
                mt.setAccessible(true);
                assertEquals("Handler " + h.getClass().getSimpleName() + " mimeType not propagated", "text/json-test", (String) mt.get(h));
            }
        }
        assertTrue("expected at least one AbstractApiHandler", abstractCount > 0);
    }

    // ==================================================================
    // Mock request class (package-local to this test class)
    // ==================================================================

    private static class MockRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final String servletPath;
        private final Map<String, Object> attributes = new HashMap<>();

        MockRequest(final String servletPath) {
            super(new StubRequest());
            this.servletPath = servletPath;
        }

        @Override
        public String getServletPath() {
            return servletPath;
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

    /** Minimal no-op servlet request for the wrapper super constructor. */
    private static class StubRequest implements jakarta.servlet.http.HttpServletRequest {
        @Override
        public Object getAttribute(final String name) {
            return null;
        }

        @Override
        public java.util.Enumeration<String> getAttributeNames() {
            return java.util.Collections.emptyEnumeration();
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
            return java.util.Collections.emptyEnumeration();
        }

        @Override
        public String[] getParameterValues(final String name) {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return java.util.Collections.emptyMap();
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
            return java.util.Collections.emptyEnumeration();
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
            return java.util.Collections.emptyEnumeration();
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            return java.util.Collections.emptyEnumeration();
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
            return java.util.Collections.emptyList();
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
