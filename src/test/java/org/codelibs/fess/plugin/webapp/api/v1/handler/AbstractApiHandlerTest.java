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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.plugin.webapp.v1_api.UnitWebappTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Unit tests for the pure-Java utility methods on {@link AbstractApiHandler}
 * that are not already covered by SearchApiManagerTest.
 */
public class AbstractApiHandlerTest extends UnitWebappTestCase {

    @Override
    protected void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    }

    // ------------------------------------------------------------------
    // escapeJson — null
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJson_null_returnsLiteralNull() {
        assertEquals("null", AbstractApiHandler.escapeJson(null));
    }

    // ------------------------------------------------------------------
    // escapeJson — Boolean
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJson_booleanTrue_returnsTrue() {
        assertEquals("true", AbstractApiHandler.escapeJson(Boolean.TRUE));
    }

    @Test
    public void test_escapeJson_booleanFalse_returnsFalse() {
        assertEquals("false", AbstractApiHandler.escapeJson(Boolean.FALSE));
    }

    // ------------------------------------------------------------------
    // escapeJson — numeric types
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJson_integer_noQuotes() {
        final String result = AbstractApiHandler.escapeJson(42);
        assertEquals("42", result);
        assertFalse(result.startsWith("\""), "Integer should not be wrapped in quotes");
    }

    @Test
    public void test_escapeJson_long_noQuotes() {
        final String result = AbstractApiHandler.escapeJson(1_234_567_890_123L);
        assertEquals("1234567890123", result);
        assertFalse(result.startsWith("\""), "Long should not be wrapped in quotes");
    }

    @Test
    public void test_escapeJson_float_noQuotes() {
        final String result = AbstractApiHandler.escapeJson(3.14f);
        assertFalse(result.startsWith("\""), "Float should not be wrapped in quotes");
        assertFalse(result.isEmpty());
    }

    @Test
    public void test_escapeJson_double_noQuotes() {
        final String result = AbstractApiHandler.escapeJson(2.718281828);
        assertFalse(result.startsWith("\""), "Double should not be wrapped in quotes");
        assertFalse(result.isEmpty());
    }

    // ------------------------------------------------------------------
    // escapeJson — String[]
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJson_emptyStringArray_returnsEmptyJsonArray() {
        assertEquals("[]", AbstractApiHandler.escapeJson(new String[0]));
    }

    @Test
    public void test_escapeJson_stringArrayWithValues() {
        final String result = AbstractApiHandler.escapeJson(new String[] { "a", "b", "c" });
        assertEquals("[\"a\",\"b\",\"c\"]", result);
    }

    @Test
    public void test_escapeJson_stringArrayWithNullElement() {
        // null elements should render as the JSON null literal
        final String result = AbstractApiHandler.escapeJson(new String[] { "a", null, "c" });
        assertEquals("[\"a\",null,\"c\"]", result);
    }

    @Test
    public void test_escapeJson_stringArrayWithSpecialChars() {
        final String result = AbstractApiHandler.escapeJson(new String[] { "hello \"world\"" });
        assertTrue(result.contains("\\\""), "Special chars must be escaped: " + result);
    }

    // ------------------------------------------------------------------
    // escapeJson — List<?>
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJson_emptyList_returnsEmptyJsonArray() {
        assertEquals("[]", AbstractApiHandler.escapeJson(List.of()));
    }

    @Test
    public void test_escapeJson_listOfStrings() {
        final String result = AbstractApiHandler.escapeJson(Arrays.asList("x", "y"));
        assertEquals("[\"x\",\"y\"]", result);
    }

    @Test
    public void test_escapeJson_listWithNull() {
        final List<String> list = Arrays.asList("a", null, "b");
        final String result = AbstractApiHandler.escapeJson(list);
        assertEquals("[\"a\",null,\"b\"]", result);
    }

    // ------------------------------------------------------------------
    // escapeJson — Map<?,?>
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJson_emptyMap_returnsEmptyJsonObject() {
        assertEquals("{}", AbstractApiHandler.escapeJson(Map.of()));
    }

    @Test
    public void test_escapeJson_mapWithStringValues() {
        // Use LinkedHashMap for deterministic insertion order
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("k1", "v1");
        m.put("k2", "v2");
        final String result = AbstractApiHandler.escapeJson(m);
        assertEquals("{\"k1\":\"v1\",\"k2\":\"v2\"}", result);
    }

    @Test
    public void test_escapeJson_mapWithMixedValues() {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "fess");
        m.put("count", 99);
        m.put("active", Boolean.TRUE);
        final String result = AbstractApiHandler.escapeJson(m);
        assertTrue(result.contains("\"name\":\"fess\""), result);
        assertTrue(result.contains("\"count\":99"), result);
        assertTrue(result.contains("\"active\":true"), result);
    }

    @Test
    public void test_escapeJson_nestedMap() {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", 1);
        final Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("inner", inner);
        final String result = AbstractApiHandler.escapeJson(outer);
        assertEquals("{\"inner\":{\"x\":1}}", result);
    }

    // ------------------------------------------------------------------
    // escapeJson — plain String
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJson_simpleString_wrappedInQuotes() {
        assertEquals("\"hello\"", AbstractApiHandler.escapeJson("hello"));
    }

    @Test
    public void test_escapeJson_stringWithNewline_escaped() {
        final String result = AbstractApiHandler.escapeJson("line1\nline2");
        assertTrue(result.contains("\\n"), "Newline should be escaped, got: " + result);
    }

    @Test
    public void test_escapeJson_stringWithBackslash_escaped() {
        final String result = AbstractApiHandler.escapeJson("a\\b");
        assertTrue(result.contains("\\\\"), "Backslash should be escaped, got: " + result);
    }

    // ------------------------------------------------------------------
    // escapeCallbackName
    // ------------------------------------------------------------------

    /** Expose escapeCallbackName via a minimal concrete subclass. */
    private static final class ConcreteHandler extends AbstractApiHandler {
        @Override
        public boolean matches(final jakarta.servlet.http.HttpServletRequest request) {
            return false;
        }

        @Override
        public void handle(final jakarta.servlet.http.HttpServletRequest request, final jakarta.servlet.http.HttpServletResponse response,
                final jakarta.servlet.FilterChain chain) {
        }

        // Make escapeCallbackName accessible in tests
        public String callEscapeCallbackName(final String name) {
            return escapeCallbackName(name);
        }
    }

    @Test
    public void test_escapeCallbackName_prependsComment() {
        final ConcreteHandler h = new ConcreteHandler();
        final String result = h.callEscapeCallbackName("myCallback");
        assertTrue(result.startsWith("/**/"), "Should start with /**/, got: " + result);
    }

    @Test
    public void test_escapeCallbackName_removesInvalidChars() {
        final ConcreteHandler h = new ConcreteHandler();
        // Spaces and hyphens are invalid in a callback name
        final String result = h.callEscapeCallbackName("my-callback name");
        assertEquals("/**/mycallbackname", result);
    }

    @Test
    public void test_escapeCallbackName_allowsDollarAndUnderscore() {
        final ConcreteHandler h = new ConcreteHandler();
        final String result = h.callEscapeCallbackName("$_cb_123");
        assertEquals("/**/$_cb_123", result);
    }

    @Test
    public void test_escapeCallbackName_allowsDots() {
        final ConcreteHandler h = new ConcreteHandler();
        final String result = h.callEscapeCallbackName("jQuery.callback");
        assertEquals("/**/jQuery.callback", result);
    }

    @Test
    public void test_escapeCallbackName_stripsAngleBrackets() {
        final ConcreteHandler h = new ConcreteHandler();
        final String result = h.callEscapeCallbackName("<script>alert(1)</script>");
        // < > ( ) / are all stripped; only alphanumeric + _ $ . survive
        assertFalse(result.contains("<"), "< should be stripped: " + result);
        assertFalse(result.contains(">"), "> should be stripped: " + result);
        assertFalse(result.contains("("), "( should be stripped: " + result);
    }

    // ------------------------------------------------------------------
    // splitPath
    // ------------------------------------------------------------------

    /** Minimal subclass to expose splitPath. */
    private static final class SplitPathHandler extends AbstractApiHandler {
        @Override
        public boolean matches(final jakarta.servlet.http.HttpServletRequest request) {
            return false;
        }

        @Override
        public void handle(final jakarta.servlet.http.HttpServletRequest request, final jakarta.servlet.http.HttpServletResponse response,
                final jakarta.servlet.FilterChain chain) {
        }

        public String[] callSplitPath(final jakarta.servlet.http.HttpServletRequest request) {
            return splitPath(request);
        }
    }

    @Test
    public void test_splitPath_standard() {
        final SplitPathHandler h = new SplitPathHandler();
        final MockHttpRequest req = new MockHttpRequest();
        req.setServletPath("/api/v1/documents");
        final String[] segments = h.callSplitPath(req);
        // ["", "api", "v1", "documents"]
        assertEquals(4, segments.length);
        assertEquals("", segments[0]);
        assertEquals("api", segments[1]);
        assertEquals("v1", segments[2]);
        assertEquals("documents", segments[3]);
    }

    @Test
    public void test_splitPath_multipleSlashesCollapsed() {
        final SplitPathHandler h = new SplitPathHandler();
        final MockHttpRequest req = new MockHttpRequest();
        req.setServletPath("/api//v1///documents");
        final String[] segments = h.callSplitPath(req);
        assertEquals(4, segments.length);
        assertEquals("documents", segments[3]);
    }

    @Test
    public void test_splitPath_trailingSlash() {
        final SplitPathHandler h = new SplitPathHandler();
        final MockHttpRequest req = new MockHttpRequest();
        req.setServletPath("/api/v1/health/");
        final String[] segments = h.callSplitPath(req);
        // trailing slash produces an empty string at the end when split naively,
        // but Pattern.split drops trailing empty strings by default
        assertTrue(segments.length >= 4, "Expected at least 4 segments, got " + segments.length);
        assertEquals("health", segments[3]);
    }

    // ------------------------------------------------------------------
    // escapeJsonKeyValue — the key has no trailing colon (regression for m1)
    // ------------------------------------------------------------------

    @Test
    public void test_escapeJsonKeyValue_keyHasNoTrailingColon() {
        // Guards against the historical bug where the error-response generator passed
        // "error_code:" as the key, producing JSON like "error_code:":"<uuid>".
        final String formatted = AbstractApiHandler.escapeJsonKeyValue("error_code", "abc-123");
        assertEquals("\"error_code\":\"abc-123\"", formatted);
    }

    @Test
    public void test_escapeJsonKeyValue_basicShape() {
        // Bare safety net for the format contract.
        assertEquals("\"k\":\"v\"", AbstractApiHandler.escapeJsonKeyValue("k", "v"));
    }

    // ------------------------------------------------------------------
    // applyJsonResponseHeaders — regression guard for C1
    // ------------------------------------------------------------------

    /** Mock HttpServletResponse capturing setHeader / setContentType calls. */
    private static class CapturingResponse implements jakarta.servlet.http.HttpServletResponse {

        final java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        String capturedContentType;

        @Override
        public void setHeader(final String name, final String value) {
            headers.put(name, value);
        }

        @Override
        public void setContentType(final String type) {
            capturedContentType = type;
        }

        // Unused methods (minimal stub) — return null/no-op
        @Override
        public void addCookie(final jakarta.servlet.http.Cookie cookie) {
        }

        @Override
        public boolean containsHeader(final String name) {
            return headers.containsKey(name);
        }

        @Override
        public String encodeURL(final String url) {
            return url;
        }

        @Override
        public String encodeRedirectURL(final String url) {
            return url;
        }

        @Override
        public void sendError(final int sc, final String msg) {
        }

        @Override
        public void sendError(final int sc) {
        }

        @Override
        public void sendRedirect(final String location) {
        }

        @Override
        public void sendRedirect(final String location, final int sc, final boolean clearBuffer) {
        }

        @Override
        public void sendRedirect(final String location, final int sc) {
        }

        @Override
        public void sendRedirect(final String location, final boolean clearBuffer) {
        }

        @Override
        public void setDateHeader(final String name, final long date) {
        }

        @Override
        public void addDateHeader(final String name, final long date) {
        }

        @Override
        public void addHeader(final String name, final String value) {
            headers.put(name, value);
        }

        @Override
        public void setIntHeader(final String name, final int value) {
        }

        @Override
        public void addIntHeader(final String name, final int value) {
        }

        @Override
        public void setStatus(final int sc) {
        }

        @Override
        public int getStatus() {
            return 0;
        }

        @Override
        public String getHeader(final String name) {
            return headers.get(name);
        }

        @Override
        public java.util.Collection<String> getHeaders(final String name) {
            return java.util.Collections.singletonList(headers.get(name));
        }

        @Override
        public java.util.Collection<String> getHeaderNames() {
            return headers.keySet();
        }

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public String getContentType() {
            return capturedContentType;
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() {
            return null;
        }

        @Override
        public java.io.PrintWriter getWriter() {
            return null;
        }

        @Override
        public void setCharacterEncoding(final String charset) {
        }

        @Override
        public void setContentLength(final int len) {
        }

        @Override
        public void setContentLengthLong(final long len) {
        }

        @Override
        public void setBufferSize(final int size) {
        }

        @Override
        public int getBufferSize() {
            return 0;
        }

        @Override
        public void flushBuffer() {
        }

        @Override
        public void resetBuffer() {
        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public void setLocale(final java.util.Locale loc) {
        }

        @Override
        public java.util.Locale getLocale() {
            return java.util.Locale.ROOT;
        }
    }

    /** Expose protected static applyJsonResponseHeaders in tests. */
    private static final class HeaderProbeHandler extends AbstractApiHandler {
        @Override
        public boolean matches(final jakarta.servlet.http.HttpServletRequest request) {
            return false;
        }

        @Override
        public void handle(final jakarta.servlet.http.HttpServletRequest request, final jakarta.servlet.http.HttpServletResponse response,
                final jakarta.servlet.FilterChain chain) {
        }

        public void invokeApply(final jakarta.servlet.http.HttpServletResponse response) {
            applyJsonResponseHeaders(response);
        }
    }

    @Test
    public void test_applyJsonResponseHeaders_setsConfiguredHeaders() {
        org.codelibs.fess.util.ComponentUtil.setFessConfig(new org.codelibs.fess.mylasta.direction.FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.util.List<org.codelibs.core.misc.Pair<String, String>> getApiJsonResponseHeaderList() {
                return java.util.List.of(new org.codelibs.core.misc.Pair<>("Access-Control-Allow-Origin", "*"),
                        new org.codelibs.core.misc.Pair<>("X-Content-Type-Options", "nosniff"));
            }
        });

        final CapturingResponse res = new CapturingResponse();
        new HeaderProbeHandler().invokeApply(res);
        assertEquals("*", res.headers.get("Access-Control-Allow-Origin"));
        assertEquals("nosniff", res.headers.get("X-Content-Type-Options"));
    }

    @Test
    public void test_applyJsonResponseHeaders_emptyList_noHeaders() {
        org.codelibs.fess.util.ComponentUtil.setFessConfig(new org.codelibs.fess.mylasta.direction.FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.util.List<org.codelibs.core.misc.Pair<String, String>> getApiJsonResponseHeaderList() {
                return java.util.Collections.emptyList();
            }
        });

        final CapturingResponse res = new CapturingResponse();
        new HeaderProbeHandler().invokeApply(res);
        assertTrue("no headers expected, got: " + res.headers, res.headers.isEmpty());
    }

    // ------------------------------------------------------------------
    // AbstractChatHandler.writeJsonResponse — uses propagated mimeType (GAP-3)
    // ------------------------------------------------------------------

    @Test
    public void test_chatHandler_writeJsonResponse_usesPropagatedMimeType() throws Exception {
        org.codelibs.fess.util.ComponentUtil.setFessConfig(new org.codelibs.fess.mylasta.direction.FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.util.List<org.codelibs.core.misc.Pair<String, String>> getApiJsonResponseHeaderList() {
                return java.util.Collections.emptyList();
            }
        });

        final ChatHandler ch = new ChatHandler();
        ch.setMimeType("application/vnd.fess+json");

        final CapturingResponse res = new CapturingResponse() {
            @Override
            public java.io.PrintWriter getWriter() {
                return new java.io.PrintWriter(new java.io.StringWriter());
            }
        };
        final java.lang.reflect.Method m = AbstractChatHandler.class.getDeclaredMethod("writeJsonResponse",
                jakarta.servlet.http.HttpServletResponse.class, int.class, java.util.Map.class);
        m.setAccessible(true);
        m.invoke(ch, res, 200, java.util.Map.of("x", "y"));
        assertTrue("Content-Type must include propagated mimeType, got: " + res.capturedContentType,
                res.capturedContentType.startsWith("application/vnd.fess+json"));
    }
}
