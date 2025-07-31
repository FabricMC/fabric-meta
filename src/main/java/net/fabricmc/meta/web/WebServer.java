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

import static io.javalin.apibuilder.ApiBuilder.after;
import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.path;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.plugin.bundled.CorsPlugin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.plugin.bundled.RouteOverviewPlugin;

import net.fabricmc.meta.data.DataProvider;
import net.fabricmc.meta.web.v1.EndpointsV1;
import net.fabricmc.meta.web.v2.EndpointsV2;

public class WebServer {
	@Deprecated(forRemoval = true)
	public static Javalin javalin;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final DataProvider dataProvider;
	private final CacheHandler cacheHandler;

	private final EndpointsV1 endpointsV1;

	public WebServer(DataProvider dataProvider, CacheHandler cacheHandler) {
		this.dataProvider = dataProvider;
		this.cacheHandler = cacheHandler;

		endpointsV1 = new EndpointsV1(dataProvider);
	}

	public Javalin createServer() {
		Javalin javalin = Javalin.create(config -> {
			config.useVirtualThreads = true;
			config.showJavalinBanner = false;
			config.registerPlugin(new RouteOverviewPlugin(routeOverview -> routeOverview.path = "/"));
			config.registerPlugin(new CorsPlugin(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost)));

			config.router.apiBuilder(() -> {
				before(cacheHandler.before());
				after(cacheHandler.after());
				path("v1", endpointsV1.routes());
			});
		});

		// TODO remove this
		WebServer.javalin = javalin;
		EndpointsV2.setup();

		return javalin;
	}

	public static <T> void jsonGet(String route, Supplier<T> supplier) {
		javalin.get(route, ctx -> {
			T object = supplier.get();
			handleJson(ctx, object);
		});
	}

	public static <T> void jsonGet(String route, Function<Context, T> supplier) {
		javalin.get(route, ctx -> {
			T object = supplier.apply(ctx);
			handleJson(ctx, object);
		});
	}

	private static void handleJson(Context ctx, Object object) {
		if (object == null) {
			object = new Object();
			ctx.status(400);
		}

		String response = GSON.toJson(object);
		ctx.contentType("application/json").header(Header.CACHE_CONTROL, "public, max-age=60").result(response);
	}
}
