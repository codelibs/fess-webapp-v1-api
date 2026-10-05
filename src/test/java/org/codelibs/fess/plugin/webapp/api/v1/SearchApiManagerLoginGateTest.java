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

import org.codelibs.fess.plugin.webapp.api.v1.handler.ApiHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.ChatHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.ChatStreamHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.FavoriteHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.FavoritesHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.LabelHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.PingHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.PopularWordHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.ScrollSearchHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.SearchHandler;
import org.codelibs.fess.plugin.webapp.api.v1.handler.SuggestHandler;
import org.codelibs.fess.plugin.webapp.v1_api.UnitWebappTestCase;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tests for the refusal decision {@link SearchApiManager#getRefusalStatus} makes before a handler runs:
 * {@code login.required} and {@code rag.chat.permissions}, which the v2 API already honours.
 */
public class SearchApiManagerLoginGateTest extends UnitWebappTestCase {

    private static final int OK = 0;
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;

    /** A manager whose caller is described by the fields, without a login session or a search engine. */
    private static class Gate extends SearchApiManager {
        boolean loginRequired;
        boolean loggedIn;
        boolean chatPermitted = true;
        boolean validToken;

        @Override
        protected boolean isLoginRequired() {
            return loginRequired;
        }

        @Override
        protected boolean isLoggedIn() {
            return loggedIn;
        }

        @Override
        protected boolean isChatPermitted() {
            return chatPermitted;
        }

        @Override
        protected boolean hasValidAccessToken(final HttpServletRequest request) {
            return validToken;
        }

        int status(final ApiHandler handler) {
            return getRefusalStatus(handler, null);
        }
    }

    private static void check(final int expected, final Gate gate, final ApiHandler handler) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, gate.status(handler), handler.getClass().getSimpleName());
    }

    private static final ApiHandler[] TOKEN_ENDPOINTS =
            { new SearchHandler(), new ScrollSearchHandler(), new SuggestHandler(), new LabelHandler(), new PopularWordHandler() };
    private static final ApiHandler[] USER_ENDPOINTS =
            { new FavoriteHandler(), new FavoritesHandler(), new ChatHandler(), new ChatStreamHandler() };

    @Test
    public void test_everythingIsServed_whenLoginIsNotRequired() {
        final Gate gate = new Gate();
        for (final ApiHandler handler : TOKEN_ENDPOINTS) {
            check(OK, gate, handler);
        }
        for (final ApiHandler handler : USER_ENDPOINTS) {
            check(OK, gate, handler);
        }
    }

    @Test
    public void test_anonymousCallerIsRefusedEverywhere_whenLoginIsRequired() {
        final Gate gate = new Gate();
        gate.loginRequired = true;
        for (final ApiHandler handler : TOKEN_ENDPOINTS) {
            check(UNAUTHORIZED, gate, handler);
        }
        for (final ApiHandler handler : USER_ENDPOINTS) {
            check(UNAUTHORIZED, gate, handler);
        }
    }

    @Test
    public void test_healthCheckIsServedWithoutALogin() {
        final Gate gate = new Gate();
        gate.loginRequired = true;
        assertEquals(OK, gate.status(new PingHandler()));
    }

    @Test
    public void test_loggedInUserIsServed_whenLoginIsRequired() {
        final Gate gate = new Gate();
        gate.loginRequired = true;
        gate.loggedIn = true;
        for (final ApiHandler handler : TOKEN_ENDPOINTS) {
            check(OK, gate, handler);
        }
        for (final ApiHandler handler : USER_ENDPOINTS) {
            check(OK, gate, handler);
        }
    }

    @Test
    public void test_accessTokenStandsInForTheLoginOnlyWhereTheTokenDecidesTheResult() {
        final Gate gate = new Gate();
        gate.loginRequired = true;
        gate.validToken = true;
        for (final ApiHandler handler : TOKEN_ENDPOINTS) {
            check(OK, gate, handler);
        }
        for (final ApiHandler handler : USER_ENDPOINTS) {
            check(UNAUTHORIZED, gate, handler);
        }
    }

    @Test
    public void test_chatIsRefusedToAUserWithoutThePermission() {
        final Gate gate = new Gate();
        gate.chatPermitted = false;
        gate.loggedIn = true;
        assertEquals(FORBIDDEN, gate.status(new ChatHandler()));
        assertEquals(FORBIDDEN, gate.status(new ChatStreamHandler()));
        // the permission is about the chat only
        assertEquals(OK, gate.status(new SearchHandler()));
    }

    @Test
    public void test_chatIsRefusedToAnAnonymousCallerWithoutThePermission_evenWhenLoginIsNotRequired() {
        final Gate gate = new Gate();
        gate.chatPermitted = false;
        assertEquals(UNAUTHORIZED, gate.status(new ChatHandler()));
        assertEquals(UNAUTHORIZED, gate.status(new ChatStreamHandler()));
    }

    @Test
    public void test_chatIsServedToAUserWithThePermission() {
        final Gate gate = new Gate();
        gate.loggedIn = true;
        assertEquals(OK, gate.status(new ChatHandler()));
    }
}
