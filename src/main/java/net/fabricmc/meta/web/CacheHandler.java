/*
 * Copyright (c) 2023 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.meta.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.javalin.http.Handler;

public class CacheHandler {
	private final Map<String, Response> cache = new ConcurrentHashMap<>();

	public CacheHandler() {
	}

	public Handler before() {
		return ctx -> {
			Response response = cache.get(ctx.path());

			if (response == null) {
				return;
			}

			// Replay the response
			ctx.status(response.status());
			response.headers().forEach(ctx::header);
			ctx.contentType(response.contentType());
			ctx.result(response.body());

			ctx.skipRemainingHandlers();
		};
	}

	public Handler after() {
		return ctx -> {
			if (ctx.statusCode() != 200) {
				return;
			}

			if (!ctx.queryParamMap().isEmpty()) {
				// Don't cache any requests with query params to prevent the cache from growing too big.
				// Maybe look into something better here
				return;
			}

			cache.put(ctx.path(), new Response(
					ctx.statusCode(),
					readAllBytes(ctx.resultInputStream()),
					ctx.headerMap(),
					ctx.res().getContentType()));
		};
	}

	private static byte[] readAllBytes(InputStream is) throws IOException {
		is.reset();
		byte[] bytes = is.readAllBytes();
		is.reset();
		return bytes;
	}

	public void invalidateCache() {
		cache.clear();
	}

	private record Response(
			int status,
			byte[] body,
			Map<String, String> headers,
			String contentType) {
	}
}
