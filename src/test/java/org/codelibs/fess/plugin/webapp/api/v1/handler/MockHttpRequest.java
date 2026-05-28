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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared minimal mock for {@link jakarta.servlet.http.HttpServletRequest} used across handler tests.
 */
public class MockHttpRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

    private String servletPath = "/";
    private String method = "GET";
    private Locale locale = Locale.ROOT;
    private final Map<String, String[]> parameterValuesMap = new HashMap<>();
    private final Map<String, Object> attributeMap = new HashMap<>();
    private final Map<String, String> headerMap = new HashMap<>();

    public MockHttpRequest() {
        super(new StubHttpServletRequest());
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(final String servletPath) {
        this.servletPath = servletPath;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    @Override
    public String getParameter(final String name) {
        final String[] vals = parameterValuesMap.get(name);
        return (vals != null && vals.length > 0) ? vals[0] : null;
    }

    @Override
    public String[] getParameterValues(final String name) {
        return parameterValuesMap.get(name);
    }

    public void setParameter(final String name, final String value) {
        parameterValuesMap.put(name, new String[] { value });
    }

    public void setParameterValues(final String name, final String[] values) {
        parameterValuesMap.put(name, values);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameterValuesMap);
    }

    @Override
    public Object getAttribute(final String name) {
        return attributeMap.get(name);
    }

    @Override
    public void setAttribute(final String name, final Object o) {
        attributeMap.put(name, o);
    }

    @Override
    public String getHeader(final String name) {
        return headerMap.get(name);
    }

    public void setHeader(final String name, final String value) {
        headerMap.put(name, value);
    }

    // -------------------------------------------------------------------------
    // Stub inner class — provides the required non-null delegate for the wrapper
    // -------------------------------------------------------------------------

    static class StubHttpServletRequest implements jakarta.servlet.http.HttpServletRequest {
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
        public Locale getLocale() {
            return Locale.ROOT;
        }

        @Override
        public java.util.Enumeration<Locale> getLocales() {
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
