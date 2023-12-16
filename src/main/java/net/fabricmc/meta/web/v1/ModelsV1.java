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

package net.fabricmc.meta.web.v1;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.fabricmc.meta.data.DataProvider;
import net.fabricmc.meta.web.JsonModel;
import net.fabricmc.meta.web.models.BaseVersion;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;

/**
 * Strongly defined records of the public API. Take extra care to record to method return types here.
 */
public sealed interface ModelsV1 extends JsonModel permits ModelsV1.GameVersion, ModelsV1.MappingVersion, ModelsV1.LoaderVersion {
	/**
	 * /v2/versions/game
	 */
	static List<GameVersion> gameVersions(DataProvider dataProvider) {
		LinkedList<GameVersion> versions = new LinkedList<>();

		for (BaseVersion version : dataProvider.getVersionDatabase().game) {
			versions.add(GameVersion.from(version));
		}

		return versions;
	}

	/**
	 * /v2/game/{game_version}
	 */
	static List<GameVersion> gameVersions(DataProvider dataProvider, String gameVersion) {
		for (BaseVersion version : dataProvider.getVersionDatabase().game) {
			if (version.getVersion().equals(gameVersion)) {
				return List.of(GameVersion.from(version));
			}
		}

		return Collections.emptyList();
	}

	/**
	 * /v2/versions/mappings
	 */
	static List<MappingVersion> mappingVersions(DataProvider dataProvider) {
		LinkedList<MappingVersion> versions = new LinkedList<>();

		for (MavenBuildGameVersion version : dataProvider.getVersionDatabase().mappings) {
			versions.add(MappingVersion.from(version));
		}

		return versions;
	}

	/**
	 * /v2/mappings/{game_version}
	 */
	static List<MappingVersion> mappingVersions(DataProvider dataProvider, String gameVersion) {
		LinkedList<MappingVersion> versions = new LinkedList<>();

		for (MavenBuildGameVersion version : dataProvider.getVersionDatabase().mappings) {
			if (version.getGameVersion().equals(gameVersion)) {
				versions.add(MappingVersion.from(version));
			}
		}

		return versions;
	}

	/**
	 * /v2/versions/loader
	 */
	static List<LoaderVersion> loaderVersions(DataProvider dataProvider) {
		LinkedList<LoaderVersion> versions = new LinkedList<>();

		for (MavenBuildVersion version : dataProvider.getVersionDatabase().getLoader()) {
			versions.add(LoaderVersion.from(version));
		}

		return versions;
	}

	record GameVersion(String version, boolean stable) implements ModelsV1 {
		private static GameVersion from(BaseVersion version) {
			return new GameVersion(version.getVersion(), version.isStable());
		}
	}

	record MappingVersion(String gameVersion, String separator, int build, String maven, String version, boolean stable) implements ModelsV1 {
		private static MappingVersion from(MavenBuildGameVersion version) {
			return new MappingVersion(version.getGameVersion(), version.getSeparator(), version.getBuild(), version.getMaven(), version.getVersion(), version.isStable());
		}
	}

	record LoaderVersion(String separator, int build, String maven, String version, boolean stable) implements ModelsV1 {
		private static LoaderVersion from(MavenBuildVersion version) {
			return new LoaderVersion(version.getSeparator(), version.getBuild(), version.getMaven(), version.getVersion(), version.isStable());
		}
	}
}
