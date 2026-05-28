v1 REST API Plugin for Fess
[![Java CI with Maven](https://github.com/codelibs/fess-webapp-v1-api/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-webapp-v1-api/actions/workflows/maven.yml)
==========================

## Overview

This plugin provides the v1 REST API surface (`/api/v1/*`) for the Fess web
application. It registers `SearchApiManager` and `ChatApiManager` with the Fess
`WebApiManagerFactory`, exposing the following endpoints:

- `/api/v1/documents` (search)
- `/api/v1/documents/{id}/favorite`
- `/api/v1/documents/all` (scroll)
- `/api/v1/labels`
- `/api/v1/popular-words`
- `/api/v1/favorites`
- `/api/v1/health`
- `/api/v1/suggest-words`
- `/api/v1/chat`
- `/api/v1/chat/stream`

## Download

See [Maven Repository](https://repo1.maven.org/maven2/org/codelibs/fess/fess-webapp-v1-api/).

## Installation

See [Plugin](https://fess.codelibs.org/15.7/admin/plugin-guide.html) of Administration guide.

