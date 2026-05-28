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
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.exception.InvalidAccessTokenException;
import org.codelibs.fess.util.ComponentUtil;
import org.lastaflute.web.util.LaRequestUtil;
import org.lastaflute.web.util.LaResponseUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Base class for API handlers, providing shared response-writing and JSON-escaping utilities.
 */
public abstract class AbstractApiHandler implements ApiHandler {

    private static final Logger logger = LogManager.getLogger(AbstractApiHandler.class);

    /**
     * The message field name used in JSON error responses.
     */
    protected static final String MESSAGE_FIELD = "message";

    /**
     * The result field name used in JSON success responses.
     */
    protected static final String RESULT_FIELD = "result";

    /**
     * The HTTP GET method string.
     */
    protected static final String GET = "GET";

    /**
     * The HTTP POST method string.
     */
    protected static final String POST = "POST";

    /**
     * ISO-8601 extended formatter matching the legacy {@code SimpleDateFormat} pattern
     * {@code "yyyy-MM-dd'T'HH:mm:ss.SSSZ"}.
     */
    protected static final DateTimeFormatter ISO_8601_EXTEND_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT);

    /**
     * Pattern used to strip invalid characters from JSONP callback names.
     */
    protected static final Pattern CALLBACK_NAME_INVALID_CHARS = Pattern.compile("[^0-9a-zA-Z_$.]");

    /**
     * Pattern for collapsing multiple consecutive slashes in servlet paths.
     */
    protected static final Pattern MULTIPLE_SLASHES = Pattern.compile("/+");

    /**
     * Default MIME type for JSON responses.
     */
    protected String mimeType = "application/json";

    /**
     * Default constructor for subclasses.
     */
    protected AbstractApiHandler() {
        // no-op
    }

    /**
     * Sets the MIME type used by JSON responses written via {@link #writeJsonResponse(int, String)}.
     *
     * @param mimeType the MIME type string (without charset suffix)
     */
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Applies the configured API JSON response headers (CORS, security headers, etc.) from
     * {@link org.codelibs.fess.mylasta.direction.FessConfig#getApiJsonResponseHeaderList()}.
     *
     * @param response the HTTP response
     */
    protected static void applyJsonResponseHeaders(final HttpServletResponse response) {
        ComponentUtil.getFessConfig().getApiJsonResponseHeaderList().forEach(e -> response.setHeader(e.getFirst(), e.getSecond()));
    }

    /**
     * Splits the servlet path on one or more consecutive slashes.
     *
     * @param request the HTTP request
     * @return path segments; index 0 is always an empty string because paths start with "/"
     */
    protected String[] splitPath(final HttpServletRequest request) {
        return MULTIPLE_SLASHES.split(request.getServletPath());
    }

    /**
     * Checks whether the request path's type segment (the 4th path segment, i.e. {@code segments[3]})
     * matches the given type identifier, case-insensitively. Mirrors the lowercasing done by the
     * original {@code SearchApiManager.detectFormatType} switch.
     *
     * @param request the HTTP request
     * @param type the expected type identifier (e.g. {@code "labels"}, {@code "documents"})
     * @return true when the type segment matches {@code type} (case-insensitive)
     */
    protected boolean isTypeSegment(final HttpServletRequest request, final String type) {
        final String[] segments = splitPath(request);
        return segments.length > 3 && type.equalsIgnoreCase(segments[3]);
    }

    /**
     * Validates that the request method is one of the accepted methods.
     * Writes a 405 response and returns false when the method is not allowed.
     *
     * @param request the HTTP request
     * @param methods the accepted HTTP methods
     * @return true if the method is accepted, false otherwise
     */
    protected boolean acceptHttpMethod(final HttpServletRequest request, final String... methods) {
        final String method = request.getMethod();
        for (final String m : methods) {
            if (m.equals(method)) {
                return true;
            }
        }
        writeJsonResponse(HttpServletResponse.SC_METHOD_NOT_ALLOWED, escapeJsonKeyValue(MESSAGE_FIELD, method + " is not allowed."));
        return false;
    }

    /**
     * Writes a JSON response whose body is derived from the given throwable.
     * When the {@code api.json.response.exception.included} config is {@code true}, the full
     * stack trace is embedded; otherwise an opaque error code is logged and returned.
     *
     * @param status the HTTP status code
     * @param t the throwable whose message / stack trace describes the error
     */
    protected void writeJsonResponse(final int status, final Throwable t) {
        final Supplier<String> stacktraceString = () -> {
            final StringBuilder buf = new StringBuilder(100);
            if (StringUtil.isBlank(t.getMessage())) {
                buf.append(t.getClass().getName());
            } else {
                buf.append(t.getMessage());
            }
            try (final StringWriter sw = new StringWriter(); final PrintWriter pw = new PrintWriter(sw)) {
                t.printStackTrace(pw);
                pw.flush();
                buf.append(" [ ").append(sw.toString()).append(" ]");
            } catch (final IOException e) {
                // StringWriter.close() should not throw IOException, but log just in case
                if (logger.isDebugEnabled()) {
                    logger.debug("Unexpected IOException while closing StringWriter", e);
                }
            }
            return buf.toString();
        };
        final String message;
        if (Constants.TRUE.equalsIgnoreCase(ComponentUtil.getFessConfig().getApiJsonResponseExceptionIncluded())) {
            message = escapeJsonKeyValue(MESSAGE_FIELD, stacktraceString.get());
        } else {
            final String errorCode = UUID.randomUUID().toString();
            message = escapeJsonKeyValue("error_code", errorCode);
            if (logger.isDebugEnabled()) {
                logger.debug("[{}] {}", errorCode, stacktraceString.get().replace("\n", "\\n"));
            } else if (status >= 500) {
                logger.warn("[{}] {}", errorCode, t.getMessage());
            }
        }
        final HttpServletResponse response = LaResponseUtil.getResponse();
        if (t instanceof final InvalidAccessTokenException e) {
            response.setHeader("WWW-Authenticate", "Bearer error=\"" + e.getType() + "\"");
            writeJsonResponse(HttpServletResponse.SC_UNAUTHORIZED, message);
        } else {
            writeJsonResponse(status, message);
        }
    }

    /**
     * Writes a JSON response with the given status code and pre-serialised body fragment.
     * Supports JSONP when the {@code callback} request parameter is present and JSONP is enabled.
     *
     * @param status the HTTP status code
     * @param body a JSON fragment (without the outer braces) to embed in the response object
     */
    protected void writeJsonResponse(final int status, final String body) {
        writeJsonResponse(status, body, mimeType);
    }

    /**
     * Static variant of {@link #writeJsonResponse(int, String)} used by the dispatcher when no
     * handler instance is available. Applies {@code getApiJsonResponseHeaderList()} and supports
     * JSONP wrapping like the instance method.
     *
     * @param status the HTTP status code
     * @param body a JSON fragment (without the outer braces) to embed in the response object
     * @param mimeType the response MIME type (without charset suffix)
     */
    public static void writeJsonResponse(final int status, final String body, final String mimeType) {
        final String callback = LaRequestUtil.getOptionalRequest().map(req -> req.getParameter("callback")).orElse(null);
        final boolean isJsonp = ComponentUtil.getFessConfig().isApiJsonpEnabled() && StringUtil.isNotBlank(callback);

        final HttpServletResponse response = LaResponseUtil.getResponse();
        response.setStatus(status);

        final StringBuilder buf = new StringBuilder(1000);
        if (isJsonp) {
            buf.append(escapeCallbackName(callback));
            buf.append('(');
        }
        buf.append('{');
        if (StringUtil.isNotBlank(body)) {
            buf.append(body);
        }
        buf.append('}');
        if (isJsonp) {
            buf.append(')');
        }
        try {
            response.setContentType(mimeType + "; charset=" + Constants.UTF_8);
            applyJsonResponseHeaders(response);
            response.getWriter().write(buf.toString());
        } catch (final IOException e) {
            logger.warn("Failed to write JSON response", e);
        }
    }

    /**
     * Formats a key-value pair as a JSON property string (e.g. {@code "key":"value"}).
     *
     * @param key the JSON property name
     * @param value the property value, JSON-escaped via {@link #escapeJson(Object)}
     * @return the formatted JSON property string
     */
    public static String escapeJsonKeyValue(final String key, final String value) {
        return "\"" + key + "\":" + escapeJson(value);
    }

    /**
     * Sanitises a JSONP callback name by stripping all characters that are not
     * alphanumeric, {@code _}, {@code $}, or {@code .}.
     *
     * @param callbackName the raw callback name from the request
     * @return a safe callback name prefixed with a comment opener sequence
     */
    public static String escapeCallbackName(final String callbackName) {
        return "/**/" + CALLBACK_NAME_INVALID_CHARS.matcher(callbackName).replaceAll(StringUtil.EMPTY);
    }

    /**
     * Serialises {@code obj} to a JSON value string.
     *
     * <p>Supported types: {@code null}, {@code String[]}, {@code List<?>}, {@code Map<?,?>},
     * {@code Integer}, {@code Long}, {@code Float}, {@code Double}, {@code Boolean}, {@code Date},
     * and any other object whose {@code toString()} is used as a JSON string.
     *
     * <p>The {@link Date} branch formats using {@link #ISO_8601_EXTEND_FORMATTER} with the JVM
     * default time zone, which matches the legacy {@code SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")}
     * output tested by {@code SearchApiManagerTest.test_escapeJson_dateMatchesLegacySimpleDateFormat}.
     *
     * @param obj the object to serialise
     * @return JSON representation of {@code obj}
     */
    public static String escapeJson(final Object obj) {
        if (obj == null) {
            return "null";
        }

        final StringBuilder buf = new StringBuilder(255);
        if (obj instanceof String[]) {
            buf.append('[');
            boolean first = true;
            for (final Object child : (String[]) obj) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(escapeJson(child));
            }
            buf.append(']');
        } else if (obj instanceof List<?>) {
            buf.append('[');
            boolean first = true;
            for (final Object child : (List<?>) obj) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(escapeJson(child));
            }
            buf.append(']');
        } else if (obj instanceof Map<?, ?>) {
            buf.append('{');
            boolean first = true;
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(escapeJson(entry.getKey())).append(':').append(escapeJson(entry.getValue()));
            }
            buf.append('}');
        } else if (obj instanceof Integer || obj instanceof Long || obj instanceof Float || obj instanceof Double) {
            buf.append(obj);
        } else if (obj instanceof Boolean) {
            buf.append(obj.toString());
        } else if (obj instanceof Date) {
            buf.append('\"')
                    .append(StringEscapeUtils
                            .escapeJson(ISO_8601_EXTEND_FORMATTER.withZone(ZoneId.systemDefault()).format(((Date) obj).toInstant())))
                    .append('\"');
        } else {
            buf.append('\"').append(StringEscapeUtils.escapeJson(obj.toString())).append('\"');
        }
        return buf.toString();
    }

    /**
     * Returns a human-readable chain of exception messages, suitable for debug logging.
     *
     * @param t the throwable (may be null)
     * @return a summary string
     */
    protected String detailedMessage(final Throwable t) {
        if (t == null) {
            return "Unknown";
        }
        Throwable target = t;
        if (target.getCause() == null) {
            return target.getClass().getSimpleName() + "[" + target.getMessage() + "]";
        }
        final StringBuilder sb = new StringBuilder();
        while (target != null) {
            sb.append(target.getClass().getSimpleName());
            if (target.getMessage() != null) {
                sb.append("[");
                sb.append(target.getMessage());
                sb.append("]");
            }
            sb.append("; ");
            target = target.getCause();
            if (target != null) {
                sb.append("nested: ");
            }
        }
        return sb.toString();
    }
}
