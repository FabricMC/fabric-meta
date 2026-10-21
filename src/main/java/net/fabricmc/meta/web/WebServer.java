/*
 * Copyright (c) 2019 FabricMC
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.plugin.bundled.CorsPluginConfig;

public class WebServer {
	public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Javalin javalin;
	static RoutesConfig routes;

	private static final int MAX_CACHE_SIZE = Runtime.getRuntime().availableProcessors();
	private static final Deque<StringBuilder> SB_CACHE = new ArrayDeque<>(MAX_CACHE_SIZE);

	public static Javalin create() {
		if (javalin != null) {
			javalin.stop();
		}

		javalin = Javalin.create(config -> {
			config.bundledPlugins.enableRouteOverview("/");
			config.startup.showJavalinBanner = false;
			config.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));
			routes = config.routes;
			EndpointsV1.setup();
			EndpointsV2.setup();
		});

		return javalin;
	}

	public static void start() {
		assert javalin == null;
		create().start(5555);
	}

	public static <T> void jsonGet(String route, Supplier<T> supplier) {
		routes.get(route, ctx -> {
			T object = supplier.get();
			handleJson(ctx, object);
		});
	}

	public static <T> void jsonGet(String route, Function<Context, T> supplier) {
		routes.get(route, ctx -> {
			T object = supplier.apply(ctx);
			handleJson(ctx, object);
		});
	}

	private static void handleJson(Context ctx, Object object) {
		if (object == null) {
			object = new Object();
			ctx.status(400);
		}

		// cache string builder to reduce allocation/resizing pressure
		StringBuilder sb;

		synchronized (SB_CACHE) {
			sb = SB_CACHE.pollLast();

			if (sb == null) {
				sb = new StringBuilder(10_000);
			} else {
				sb.setLength(0);
			}
		}

		try {
			GSON.toJson(object, sb);
			String response = sb.toString();

			ctx.contentType("application/json").header(Header.CACHE_CONTROL, "public, max-age=60").result(response);
		} finally {
			synchronized (SB_CACHE) {
				if (SB_CACHE.size() < MAX_CACHE_SIZE) {
					SB_CACHE.addLast(sb);
				}
			}
		}
	}
}
