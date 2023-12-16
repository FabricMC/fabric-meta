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

package net.fabricmc.meta.web.v1;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.Header;

import net.fabricmc.meta.data.DataProvider;
import net.fabricmc.meta.web.Endpoint;
import net.fabricmc.meta.web.WebServer;
import net.fabricmc.meta.web.models.LoaderInfoV1;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;

@SuppressWarnings("Duplicates")
public class EndpointsV1 extends Endpoint {
	public EndpointsV1(DataProvider dataProvider) {
		super(dataProvider);
	}

	@Override
	public EndpointGroup routes() {
		return () -> {
			before(EndpointsV1::beforeHandler);
			path("versions", () -> {
				get(json(() -> dataProvider.getVersionDatabase()));
				get("game", result(ModelsV1::gameVersions));
				get("game/{game_version}", result("game_version", ModelsV1::gameVersions));
				get("mappings", result(ModelsV1::mappingVersions));
				get("mappings/{game_version}", result("game_version", ModelsV1::mappingVersions));
				get("loader", result(ModelsV1::loaderVersions));
				get("loader/{game_version}", json(this::getLoaderInfoAll));
				get("loader/{game_version}/{loader_version}", json(this::getLoaderInfo));
			});
		};
	}

	private static void beforeHandler(Context ctx) {
		ctx.header(Header.CACHE_CONTROL, "public, max-age=60");
	}

	@Deprecated
	private Handler json(Supplier<Object> objectSupplier) {
		return ctx -> ctx.result(WebServer.GSON.toJson(objectSupplier.get()));
	}

	@Deprecated
	private Handler json(Function<Context, Object> objectFunction) {
		return ctx -> ctx.result(WebServer.GSON.toJson(objectFunction.apply(ctx)));
	}

	private LoaderInfoV1 getLoaderInfo(Context context) {
		final String gameVersion = context.pathParamAsClass("game_version", String.class).get();
		final String loaderVersion = context.pathParamAsClass("loader_version", String.class).get();

		MavenBuildVersion loader = dataProvider.getVersionDatabase().getLoader().stream()
				.filter(mavenBuildVersion -> loaderVersion.equals(mavenBuildVersion.getVersion()))
				.findFirst()
				.orElse(null);

		MavenBuildGameVersion mappings = dataProvider.getVersionDatabase().mappings.stream()
				.filter(t -> t.test(gameVersion))
				.findFirst()
				.orElse(null);

		if (loader == null) {
			throw new BadRequestResponse("no loader version found for " + gameVersion);
		}

		if (mappings == null) {
			throw new BadRequestResponse("no mappings version found for " + gameVersion);
		}

		return new LoaderInfoV1(loader, mappings).populateMeta();
	}

	private List<LoaderInfoV1> getLoaderInfoAll(Context context) {
		final String gameVersion = context.pathParamAsClass("game_version", String.class).get();

		MavenBuildGameVersion mappings = dataProvider.getVersionDatabase().mappings.stream()
				.filter(t -> t.test(gameVersion))
				.findFirst()
				.orElse(null);

		if (mappings == null) {
			return Collections.emptyList();
		}

		List<LoaderInfoV1> infoList = new LinkedList<>();

		for (MavenBuildVersion loader : dataProvider.getVersionDatabase().getLoader()) {
			infoList.add(new LoaderInfoV1(loader, mappings));
		}

		return infoList;
	}
}
