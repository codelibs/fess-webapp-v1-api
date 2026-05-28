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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Strategy interface for per-endpoint API request handlers.
 */
public interface ApiHandler {

    /**
     * Returns true if this handler should process the given request.
     *
     * @param request the incoming HTTP request
     * @return true if this handler matches the request
     */
    boolean matches(HttpServletRequest request);

    /**
     * Processes the request and writes the response.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param chain the filter chain
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;
}
