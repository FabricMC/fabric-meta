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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.javalin.http.Context;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.models.LoaderInfoV1;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;

@SuppressWarnings("Duplicates")
public class EndpointsV1 {
	public static void setup() {
		WebServer.jsonGet("/v1/versions", () -> FabricMeta.database.createLegacyDbDump());

		WebServer.jsonGet("/v1/versions/game", () -> FabricMeta.database.getGameModels());
		WebServer.jsonGet("/v1/versions/game/{game_version}", context -> ContextUtil.toList(ContextUtil.getGameModel(context)));

		WebServer.jsonGet("/v1/versions/mappings", () -> FabricMeta.database.getYarnModels());
		WebServer.jsonGet("/v1/versions/mappings/{game_version}", ContextUtil::getYarn);

		WebServer.jsonGet("/v1/versions/loader", () -> FabricMeta.database.getLoader());
		WebServer.jsonGet("/v1/versions/loader/{game_version}", EndpointsV1::getLoaderInfoAll);
		WebServer.jsonGet("/v1/versions/loader/{game_version}/{loader_version}", EndpointsV1::getLoaderInfo);
	}

	private static Object getLoaderInfo(Context context) {
		MavenBuildVersion loader = ContextUtil.getLoader(context);
		MavenBuildGameVersion mappings = ContextUtil.getFirstYarn(context);

		if (loader == null) {
			context.status(400);
			return "no loader version found for " + ContextUtil.getLoaderRaw(context);
		}

		if (mappings == null) {
			context.status(400);
			return "no mappings version found for " + ContextUtil.getGameRaw(context);
		}

		return new LoaderInfoV1(loader, mappings).populateMeta();
	}

	private static Object getLoaderInfoAll(Context context) {
		MavenBuildGameVersion mappings = ContextUtil.getFirstYarn(context);

		if (mappings == null) {
			return Collections.emptyList();
		}

		List<LoaderInfoV1> infoList = new ArrayList<>();

		for (MavenBuildVersion loader : FabricMeta.database.getLoader()) {
			infoList.add(new LoaderInfoV1(loader, mappings));
		}

		return infoList;
	}
}
