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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.ContentType;
import io.javalin.http.Handler;

import net.fabricmc.meta.data.DataProvider;

public abstract class Endpoint {
	protected final DataProvider dataProvider;

	protected Endpoint(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	public abstract EndpointGroup routes();

	// Return a json list with no params
	protected Handler result(Function<DataProvider, List<? extends JsonModel>> objectSupplier) {
		return ctx -> {
			List<? extends JsonModel> result = objectSupplier.apply(dataProvider);

			ctx.contentType(ContentType.APPLICATION_JSON);
			ctx.result(WebServer.GSON.toJson(result));
		};
	}

	// Return a json list with one string param
	protected Handler result(String key, BiFunction<DataProvider, String, List<? extends JsonModel>> objectSupplier) {
		return ctx -> {
			final String value = ctx.pathParamAsClass(key, String.class).get();

			List<? extends JsonModel> result = objectSupplier.apply(dataProvider, value);
			ctx.contentType(ContentType.APPLICATION_JSON);
			ctx.result(WebServer.GSON.toJson(result));
		};
	}
}
