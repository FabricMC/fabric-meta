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

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.HttpResponseException;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.fabricmc.meta.utils.Util;

public class WebServer {
	private static Javalin javalin;
	static RoutesConfig routes;

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
			MiscEndpoints.setup();
		});

		return javalin;
	}

	public static void start() {
		assert javalin == null;
		create().start(5555);
	}

	public static void get(String route, String mimeType, Function<Context, String> supplier) {
		routes.get(route, ctx -> {
			String result;

			try {
				result = supplier.apply(ctx);

				if (result == null) {
					throw new NotFoundResponse();
				}
			} catch (Throwable t) {
				result = formatException(t, mimeType);
			}

			ctx.contentType(mimeType).header(Header.CACHE_CONTROL, "public, max-age=60").result(result);
		});
	}

	public static <T> void jsonGet(String route, Supplier<T> supplier) {
		get(route, "application/json", ctx -> handleJson(supplier.get()));
	}

	public static <T> void jsonGet(String route, Function<Context, T> supplier) {
		get(route, "application/json", ctx -> handleJson(supplier.apply(ctx)));
	}

	private static String handleJson(Object object) {
		if (object == null) return null;

		return Util.serializeJson(object);
	}

	private static String formatException(Throwable exc, String mimeType) {
		int status;

		if (exc instanceof HttpResponseException e) {
			status = e.getStatus();
		} else {
			status = 500;
			exc.printStackTrace();
		}

		String message = exc.getMessage();
		if (message == null || message.isEmpty()) message = exc.getClass().getName();

		try {
			return switch (mimeType) {
			case "application/json" -> Util.serializeJson(Map.of("status", status, "message", message));
			case "text/xml" -> createXmlError(status, message);
			default -> message;
			};
		} catch (Exception e) {
			e.printStackTrace();
			return message;
		}
	}

	private static String createXmlError(int status, String message) {
		Document doc = Util.createXmlDocument();

		Element error = doc.createElement("error");
		doc.appendChild(error);

		Element statusElem = doc.createElement("status");
		statusElem.setTextContent(Integer.toString(status));
		error.appendChild(statusElem);

		Element messageElem = doc.createElement("message");
		messageElem.setTextContent(message);
		error.appendChild(messageElem);

		return Util.serializeXml(doc);
	}
}
