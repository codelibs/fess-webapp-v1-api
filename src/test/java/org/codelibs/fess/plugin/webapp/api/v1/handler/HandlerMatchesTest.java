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

import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.plugin.webapp.v1_api.UnitWebappTestCase;
import org.codelibs.fess.util.ComponentUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests for the {@code matches()} method of each individual {@link ApiHandler} implementation.
 *
 * <p>Each handler is tested in isolation; only the path and (for chat handlers) the
 * {@code isRagChatEnabled()} flag are relevant to {@code matches()}.
 */
public class HandlerMatchesTest extends UnitWebappTestCase {

    // ------------------------------------------------------------------
    // Handlers under test
    // ------------------------------------------------------------------

    private SearchHandler searchHandler;
    private ScrollSearchHandler scrollSearchHandler;
    private FavoriteHandler favoriteHandler;
    private FavoritesHandler favoritesHandler;
    private LabelHandler labelHandler;
    private PopularWordHandler popularWordHandler;
    private SuggestHandler suggestHandler;
    private PingHandler pingHandler;
    private ChatHandler chatHandler;
    private ChatStreamHandler chatStreamHandler;

    @Override
    protected void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        searchHandler = new SearchHandler();
        scrollSearchHandler = new ScrollSearchHandler();
        favoriteHandler = new FavoriteHandler();
        favoritesHandler = new FavoritesHandler();
        labelHandler = new LabelHandler();
        popularWordHandler = new PopularWordHandler();
        suggestHandler = new SuggestHandler();
        pingHandler = new PingHandler();
        chatHandler = new ChatHandler();
        chatStreamHandler = new ChatStreamHandler();
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private MockHttpRequest req(final String path) {
        final MockHttpRequest r = new MockHttpRequest();
        r.setServletPath(path);
        return r;
    }

    private void enableRagChat() {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isRagChatEnabled() {
                return true;
            }
        });
    }

    private void disableRagChat() {
        ComponentUtil.setFessConfig(new FessConfig.SimpleImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isRagChatEnabled() {
                return false;
            }
        });
    }

    // ==================================================================
    // SearchHandler
    // ==================================================================

    @Test
    public void test_searchHandler_matches_exactPath() {
        assertTrue(searchHandler.matches(req("/api/v1/documents")));
    }

    @Test
    public void test_searchHandler_matches_trailingSlash() {
        assertTrue(searchHandler.matches(req("/api/v1/documents/")));
    }

    @Test
    public void test_searchHandler_doesNotMatch_wrongPath() {
        assertFalse(searchHandler.matches(req("/api/v1/labels")));
    }

    @Test
    public void test_searchHandler_matches_apiV1Root() {
        // Regression guard for NEW-1: original detectFormatType returned SEARCH for paths without
        // a type segment (e.g. /api/v1). SearchHandler must preserve this fallback behaviour.
        assertTrue("/api/v1 should match (fallback SEARCH)", searchHandler.matches(req("/api/v1")));
        assertTrue("/api/v1/ should match (fallback SEARCH)", searchHandler.matches(req("/api/v1/")));
    }

    @Test
    public void test_searchHandler_matches_subPath() {
        // Mirrors old detectFormatType behaviour: any path with type segment "documents"
        // matches SearchHandler. The dispatcher's handler-list ordering guarantees that
        // ScrollSearchHandler and FavoriteHandler win for their sub-paths.
        assertTrue(searchHandler.matches(req("/api/v1/documents/all")));
        assertTrue(searchHandler.matches(req("/api/v1/documents/abc/favorite")));
        assertTrue(searchHandler.matches(req("/api/v1/documents/any/sub/path")));
    }

    @Test
    public void test_searchHandler_matches_v2Path() {
        // Handler-level matching is type-segment based; the /api/v1 path-prefix gate is the
        // dispatcher's responsibility (SearchApiManager.matches).
        assertTrue(searchHandler.matches(req("/api/v2/documents")));
    }

    // ==================================================================
    // ScrollSearchHandler
    // ==================================================================

    @Test
    public void test_scrollSearchHandler_matches_exactPath() {
        assertTrue(scrollSearchHandler.matches(req("/api/v1/documents/all")));
    }

    @Test
    public void test_scrollSearchHandler_matches_trailingSlash() {
        assertTrue(scrollSearchHandler.matches(req("/api/v1/documents/all/")));
    }

    @Test
    public void test_scrollSearchHandler_doesNotMatch_documentsRoot() {
        assertFalse(scrollSearchHandler.matches(req("/api/v1/documents")));
    }

    @Test
    public void test_scrollSearchHandler_doesNotMatch_wrongPath() {
        assertFalse(scrollSearchHandler.matches(req("/api/v1/labels")));
    }

    // ==================================================================
    // FavoriteHandler — path /api/v1/documents/{id}/favorite
    // ==================================================================

    @Test
    public void test_favoriteHandler_matches_withDocId() {
        assertTrue(favoriteHandler.matches(req("/api/v1/documents/abc123/favorite")));
    }

    @Test
    public void test_favoriteHandler_matches_withComplexDocId() {
        assertTrue(favoriteHandler.matches(req("/api/v1/documents/some-doc-id-42/favorite")));
    }

    @Test
    public void test_favoriteHandler_doesNotMatch_noDocId() {
        // Only 5 segments → does not match the 6-segment pattern
        assertFalse(favoriteHandler.matches(req("/api/v1/documents/favorite")));
    }

    @Test
    public void test_favoriteHandler_doesNotMatch_wrongLastSegment() {
        assertFalse(favoriteHandler.matches(req("/api/v1/documents/abc123/other")));
    }

    @Test
    public void test_favoriteHandler_doesNotMatch_favoritesPath() {
        assertFalse(favoriteHandler.matches(req("/api/v1/favorites")));
    }

    @Test
    public void test_favoriteHandler_setsDocIdAsAttribute() {
        final MockHttpRequest request = req("/api/v1/documents/myDocId/favorite");
        // matches() itself should NOT mutate the request — attribute is set in handle()
        favoriteHandler.matches(request);
        // The doc_id attribute is set in handle(), not matches(), so it should be null here.
        assertNull(request.getAttribute("doc_id"));
    }

    // ==================================================================
    // FavoritesHandler
    // ==================================================================

    @Test
    public void test_favoritesHandler_matches_exactPath() {
        assertTrue(favoritesHandler.matches(req("/api/v1/favorites")));
    }

    @Test
    public void test_favoritesHandler_matches_trailingSlash() {
        assertTrue(favoritesHandler.matches(req("/api/v1/favorites/")));
    }

    @Test
    public void test_favoritesHandler_doesNotMatch_wrongPath() {
        assertFalse(favoritesHandler.matches(req("/api/v1/documents")));
    }

    @Test
    public void test_favoritesHandler_matches_subPath() {
        // Type-segment match: old detectFormatType returned FAVORITES regardless of trailing segments.
        assertTrue(favoritesHandler.matches(req("/api/v1/favorites/sub")));
    }

    // ==================================================================
    // LabelHandler
    // ==================================================================

    @Test
    public void test_labelHandler_matches_exactPath() {
        assertTrue(labelHandler.matches(req("/api/v1/labels")));
    }

    @Test
    public void test_labelHandler_matches_trailingSlash() {
        assertTrue(labelHandler.matches(req("/api/v1/labels/")));
    }

    @Test
    public void test_labelHandler_doesNotMatch_wrongPath() {
        assertFalse(labelHandler.matches(req("/api/v1/documents")));
    }

    @Test
    public void test_labelHandler_matches_subPath() {
        assertTrue(labelHandler.matches(req("/api/v1/labels/sub")));
    }

    @Test
    public void test_labelHandler_matches_v2Path() {
        // /api/v1 path-prefix gate is the dispatcher's responsibility.
        assertTrue(labelHandler.matches(req("/api/v2/labels")));
    }

    // ==================================================================
    // PopularWordHandler
    // ==================================================================

    @Test
    public void test_popularWordHandler_matches_exactPath() {
        assertTrue(popularWordHandler.matches(req("/api/v1/popular-words")));
    }

    @Test
    public void test_popularWordHandler_matches_trailingSlash() {
        assertTrue(popularWordHandler.matches(req("/api/v1/popular-words/")));
    }

    @Test
    public void test_popularWordHandler_doesNotMatch_wrongPath() {
        assertFalse(popularWordHandler.matches(req("/api/v1/labels")));
    }

    @Test
    public void test_popularWordHandler_matches_subPath() {
        assertTrue(popularWordHandler.matches(req("/api/v1/popular-words/sub")));
    }

    // ==================================================================
    // SuggestHandler
    // ==================================================================

    @Test
    public void test_suggestHandler_matches_exactPath() {
        assertTrue(suggestHandler.matches(req("/api/v1/suggest-words")));
    }

    @Test
    public void test_suggestHandler_matches_trailingSlash() {
        assertTrue(suggestHandler.matches(req("/api/v1/suggest-words/")));
    }

    @Test
    public void test_suggestHandler_doesNotMatch_wrongPath() {
        assertFalse(suggestHandler.matches(req("/api/v1/documents")));
    }

    @Test
    public void test_suggestHandler_matches_subPath() {
        assertTrue(suggestHandler.matches(req("/api/v1/suggest-words/extra")));
    }

    // ==================================================================
    // PingHandler
    // ==================================================================

    @Test
    public void test_pingHandler_matches_exactPath() {
        assertTrue(pingHandler.matches(req("/api/v1/health")));
    }

    @Test
    public void test_pingHandler_matches_trailingSlash() {
        assertTrue(pingHandler.matches(req("/api/v1/health/")));
    }

    @Test
    public void test_pingHandler_doesNotMatch_wrongPath() {
        assertFalse(pingHandler.matches(req("/api/v1/documents")));
    }

    @Test
    public void test_pingHandler_matches_subPath() {
        assertTrue(pingHandler.matches(req("/api/v1/health/status")));
    }

    // ==================================================================
    // ChatHandler — requires isRagChatEnabled() == true
    // ==================================================================

    @Test
    public void test_chatHandler_matches_exactPath_ragEnabled() {
        enableRagChat();
        assertTrue(chatHandler.matches(req("/api/v1/chat")));
    }

    @Test
    public void test_chatHandler_matches_trailingSlash_ragEnabled() {
        enableRagChat();
        assertTrue(chatHandler.matches(req("/api/v1/chat/")));
    }

    @Test
    public void test_chatHandler_doesNotMatch_ragDisabled() {
        disableRagChat();
        assertFalse(chatHandler.matches(req("/api/v1/chat")));
    }

    @Test
    public void test_chatHandler_doesNotMatch_streamPath() {
        enableRagChat();
        // /api/v1/chat/stream must be handled by ChatStreamHandler, not ChatHandler
        assertFalse(chatHandler.matches(req("/api/v1/chat/stream")));
    }

    @Test
    public void test_chatHandler_doesNotMatch_wrongPath() {
        enableRagChat();
        assertFalse(chatHandler.matches(req("/api/v1/documents")));
    }

    // ==================================================================
    // ChatStreamHandler — requires isRagChatEnabled() == true
    // ==================================================================

    @Test
    public void test_chatStreamHandler_matches_exactPath_ragEnabled() {
        enableRagChat();
        assertTrue(chatStreamHandler.matches(req("/api/v1/chat/stream")));
    }

    @Test
    public void test_chatStreamHandler_matches_trailingSlash_ragEnabled() {
        enableRagChat();
        assertTrue(chatStreamHandler.matches(req("/api/v1/chat/stream/")));
    }

    @Test
    public void test_chatStreamHandler_doesNotMatch_ragDisabled() {
        disableRagChat();
        assertFalse(chatStreamHandler.matches(req("/api/v1/chat/stream")));
    }

    @Test
    public void test_chatStreamHandler_doesNotMatch_chatBasePath() {
        enableRagChat();
        // /api/v1/chat (without /stream) must NOT match ChatStreamHandler
        assertFalse(chatStreamHandler.matches(req("/api/v1/chat")));
    }

    @Test
    public void test_chatStreamHandler_doesNotMatch_wrongPath() {
        enableRagChat();
        assertFalse(chatStreamHandler.matches(req("/api/v1/documents")));
    }

    // ==================================================================
    // Chat handler differentiation — ordering guarantee
    // ==================================================================

    @Test
    public void test_chatHandlerDifferentiation_streamPath_onlyChatStreamHandlerMatches() {
        enableRagChat();
        final MockHttpRequest streamReq = req("/api/v1/chat/stream");
        assertFalse("ChatHandler must NOT match /api/v1/chat/stream", chatHandler.matches(streamReq));
        assertTrue("ChatStreamHandler must match /api/v1/chat/stream", chatStreamHandler.matches(streamReq));
    }

    @Test
    public void test_chatHandlerDifferentiation_chatPath_onlyChatHandlerMatches() {
        enableRagChat();
        final MockHttpRequest chatReq = req("/api/v1/chat");
        assertTrue("ChatHandler must match /api/v1/chat", chatHandler.matches(chatReq));
        assertFalse("ChatStreamHandler must NOT match /api/v1/chat", chatStreamHandler.matches(chatReq));
    }

    // ==================================================================
    // Case-sensitivity — paths are compared literally (lowercase only)
    // ==================================================================

    @Test
    public void test_labelHandler_matches_uppercaseTypeSegment() {
        // Old detectFormatType lowercased the type segment, so uppercase still matches.
        assertTrue(labelHandler.matches(req("/api/v1/LABELS")));
    }

    @Test
    public void test_searchHandler_matches_mixedCasePathPrefix() {
        // The /api/v1 path-prefix gate is the dispatcher's job; the handler only inspects segments[3].
        assertTrue(searchHandler.matches(req("/API/V1/documents")));
    }

    // ==================================================================
    // FavoriteHandler — doc_id is extracted in handle(), not matches()
    // ==================================================================

    @Test
    public void test_favoriteHandler_matches_extraSegments() {
        // Old detectFormatType used `values.length > 5 && "favorite".equals(values[5])`, so
        // paths with extra trailing segments still match.
        assertTrue(favoriteHandler.matches(req("/api/v1/documents/id/favorite/extra")));
    }
}
