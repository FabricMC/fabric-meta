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

import java.util.Collections;
import java.util.List;

import io.javalin.http.Context;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.data.VersionDatabase.GameVersionData;
import net.fabricmc.meta.web.models.BaseVersion;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;

interface ContextUtil {
	static String getGameRaw(Context context) {
		return context.pathParam("game_version");
	}

	static GameVersionData getGame(Context context) {
		String version = getGameRaw(context);

		return version != null ? FabricMeta.database.getGameData(version) : null;
	}

	static BaseVersion getGameModel(Context context) {
		GameVersionData game = getGame(context);

		return game != null ? game.exposedModel() : null;
	}

	static List<MavenBuildGameVersion> getYarn(Context context) {
		GameVersionData game = getGame(context);

		return game != null ? game.yarn() : Collections.emptyList();
	}

	static MavenBuildGameVersion getFirstYarn(Context context) {
		List<MavenBuildGameVersion> ret = getYarn(context);

		return !ret.isEmpty() ? ret.getFirst() : null;
	}

	static String getLoaderRaw(Context context) {
		return context.pathParam("loader_version");
	}

	static MavenBuildVersion getLoader(Context context) {
		String version = getLoaderRaw(context);

		return version != null ? FabricMeta.database.getLoader(version) : null;
	}

	static <T> List<T> toList(T obj) {
		return obj != null ? Collections.singletonList(obj) : Collections.emptyList();
	}
}
