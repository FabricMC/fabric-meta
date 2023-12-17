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

import java.time.Duration;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import net.fabricmc.meta.data.DataProvider;

public abstract class Endpoint {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	protected final DataProvider dataProvider;

	protected Endpoint(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	public abstract EndpointGroup routes();

	protected Handler cache(Duration duration) {
		return ctx -> {
			ctx.header("Cache-Control", "public, max-age=" + duration.getSeconds());
		};
	}

	// Return a json list with no params
	protected Handler result(JsonListHandler handler) {
		return ctx -> {
			List<? extends JsonModel> result = handler.apply(dataProvider);
			jsonResult(ctx, result);
		};
	}

	// Return a json list with one string param
	protected Handler result(String key, JsonListHandler1 handler) {
		return ctx -> {
			final String value = ctx.pathParamAsClass(key, String.class).get();
			List<? extends JsonModel> result = handler.apply(dataProvider, value);
			jsonResult(ctx, result);
		};
	}

	// Return a json list with two string params
	protected Handler result(String key1, String key2, JsonListHandler2 handler) {
		return ctx -> {
			final String value1 = ctx.pathParamAsClass(key1, String.class).get();
			final String value2 = ctx.pathParamAsClass(key2, String.class).get();
			List<? extends JsonModel> result = handler.apply(dataProvider, value1, value2);
			jsonResult(ctx, result);
		};
	}

	// Return a json list with two string params
	protected Handler result(String key1, String key2, JsonHandler2 handler) {
		return ctx -> {
			final String value1 = ctx.pathParamAsClass(key1, String.class).get();
			final String value2 = ctx.pathParamAsClass(key2, String.class).get();
			JsonModel result = handler.apply(dataProvider, value1, value2);
			jsonResult(ctx, result);
		};
	}

	protected void jsonResult(Context ctx, Object result) {
		ctx.contentType(ContentType.APPLICATION_JSON);
		ctx.result(GSON.toJson(result));
	}

	protected interface JsonListHandler {
		List<? extends JsonModel> apply(DataProvider dataProvider);
	}

	protected interface JsonListHandler1 {
		List<? extends JsonModel> apply(DataProvider dataProvider, String key1);
	}

	protected interface JsonListHandler2 {
		List<? extends JsonModel> apply(DataProvider dataProvider, String key1, String key2);
	}

	protected interface JsonHandler2 {
		JsonModel apply(DataProvider dataProvider, String key1, String key2);
	}
}
