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

import io.javalin.http.Context;
import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.models.LoaderInfoV1;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class EndpointsV1 {

	public static void setup() {

		WebServer.jsonGet("/v1/versions", () -> FabricMeta.database);

		WebServer.jsonGet("/v1/versions/game", () -> FabricMeta.database.game);
		WebServer.jsonGet("/v1/versions/game/:game_version",  context -> filter(context, FabricMeta.database.game));

		WebServer.jsonGet("/v1/versions/mappings", () -> FabricMeta.database.mappings);
		WebServer.jsonGet("/v1/versions/mappings/:game_version", context -> filter(context, FabricMeta.database.mappings));

		WebServer.jsonGet("/v1/versions/loader", () -> FabricMeta.database.getLoader());
		WebServer.jsonGet("/v1/versions/loader/:game_version", EndpointsV1::getLoaderInfoAll);
		WebServer.jsonGet("/v1/versions/loader/:game_version/:loader_version", EndpointsV1::getLoaderInfo);

	}

	private static <T extends Predicate<String>> List filter(Context context, List<T> versionList) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return Collections.emptyList();
		}
		return versionList.stream().filter(t -> t.test(context.pathParam("game_version"))).collect(Collectors.toList());

	}

	private static Object getLoaderInfo(Context context) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}
		if (!context.pathParamMap().containsKey("loader_version")) {
			return null;
		}

		String gameVersion = context.pathParam("game_version");
		String loaderVersion = context.pathParam("loader_version");

		MavenBuildVersion loader = FabricMeta.database.getAllLoader().stream()
			.filter(mavenBuildVersion -> loaderVersion.equals(mavenBuildVersion.getVersion()))
			.findFirst().orElse(null);

		MavenBuildGameVersion mappings = FabricMeta.database.mappings.stream()
			.filter(t -> t.test(gameVersion))
			.findFirst().orElse(null);

		if (loader == null) {
			context.status(400);
			return "no loader version found for " + gameVersion;
		}
		if (mappings == null) {
			context.status(400);
			return "no mappings version found for " + gameVersion;
		}
		return new LoaderInfoV1(loader, mappings).populateMeta();
	}

	private static Object getLoaderInfoAll(Context context) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}
		String gameVersion = context.pathParam("game_version");

		MavenBuildGameVersion mappings = FabricMeta.database.mappings.stream()
			.filter(t -> t.test(gameVersion))
			.findFirst().orElse(null);

		if(mappings == null){
			return Collections.emptyList();
		}

		List<LoaderInfoV1> infoList = new ArrayList<>();

		for(MavenBuildVersion loader : FabricMeta.database.getLoader()){
			infoList.add(new LoaderInfoV1(loader, mappings));
		}
		return infoList;
	}



}
