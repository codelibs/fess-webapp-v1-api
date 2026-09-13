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

import java.util.List;
import java.util.function.Consumer;

import org.codelibs.fess.entity.SearchRenderData;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.helper.RelatedContentHelper;
import org.codelibs.fess.helper.RelatedQueryHelper;
import org.codelibs.fess.helper.SearchHelper;
import org.codelibs.fess.mylasta.action.FessUserBean;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.plugin.webapp.v1_api.UnitWebappTestCase;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.optional.OptionalThing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests what {@link SearchHandler} reports about a result that is not complete, against a
 * search helper that fills the render data directly.
 */
public class SearchHandlerTest extends UnitWebappTestCase {

    @Override
    protected void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        ComponentUtil.setFessConfig(new StubFessConfig());
        ComponentUtil.register(new RelatedQueryHelper() {
            @Override
            public String[] getRelatedQueries(final String query) {
                return new String[0];
            }
        }, "relatedQueryHelper");
        ComponentUtil.register(new RelatedContentHelper() {
            @Override
            public String[] getRelatedContents(final String query) {
                return new String[0];
            }
        }, "relatedContentHelper");
    }

    @Test
    public void test_handle_reportsAShardFailure() throws Exception {
        final CapturingSearchHandler handler = search(data -> {
            data.setPartialResults(true);
            data.setShardFailed(true);
        });
        assertEquals(HttpServletResponse.SC_OK, handler.status);
        assertTrue("a failed shard must not be reported as a timeout: " + handler.body,
                handler.body.contains("\"partial\":true,\"timed_out\":false,\"shard_failed\":true"));
    }

    @Test
    public void test_handle_reportsATimeout() throws Exception {
        final CapturingSearchHandler handler = search(data -> {
            data.setPartialResults(true);
            data.setTimedOut(true);
        });
        assertEquals(HttpServletResponse.SC_OK, handler.status);
        assertTrue("a timeout must be reported as a timeout: " + handler.body,
                handler.body.contains("\"partial\":true,\"timed_out\":true,\"shard_failed\":false"));
    }

    @Test
    public void test_handle_reportsNeitherCauseForACompleteResult() throws Exception {
        final CapturingSearchHandler handler = search(data -> {});
        assertEquals(HttpServletResponse.SC_OK, handler.status);
        assertTrue("a complete result names no cause: " + handler.body,
                handler.body.contains("\"partial\":false,\"timed_out\":false,\"shard_failed\":false"));
    }

    private CapturingSearchHandler search(final Consumer<SearchRenderData> result) throws Exception {
        ComponentUtil.register(new SearchHelper() {
            @Override
            public void search(final SearchRequestParams params, final SearchRenderData data, final OptionalThing<FessUserBean> userBean) {
                data.setDocumentItems(List.of());
                result.accept(data);
            }
        }, "searchHelper");
        final MockHttpRequest request = new MockHttpRequest();
        request.setServletPath("/api/v1/documents");
        request.setParameter("q", "fess");
        final CapturingSearchHandler handler = new CapturingSearchHandler();
        handler.handle(request, null, null);
        return handler;
    }

    /**
     * Intercepts both response-writing seams so the assertions can read the body without binding
     * a LastaFlute response to the thread.
     */
    private static class CapturingSearchHandler extends SearchHandler {
        private int status = -1;
        private String body;

        @Override
        protected void writeJsonResponse(final int status, final String body) {
            this.status = status;
            this.body = body;
        }

        @Override
        protected void writeJsonResponse(final int status, final Throwable t) {
            this.status = status;
            body = String.valueOf(t);
        }
    }

    /**
     * Only the keys this path reads; {@code FessConfig.SimpleImpl} throws from any other getter
     * without a loaded property object.
     */
    private static class StubFessConfig extends FessConfig.SimpleImpl {
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
    }
}
