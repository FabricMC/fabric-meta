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

import com.google.gson.JsonObject;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.InternalServerErrorResponse;

import net.fabricmc.meta.data.DataProvider;
import net.fabricmc.meta.models.BaseVersion;
import net.fabricmc.meta.models.MavenBuildGameVersion;
import net.fabricmc.meta.models.MavenBuildVersion;
import net.fabricmc.meta.web.JsonModel;

/**
 * Strongly defined records of the public API. Take extra care to record to method return types here.
 */
public sealed interface ModelsV1 extends JsonModel permits ModelsV1.GameVersion, ModelsV1.MappingVersion, ModelsV1.LoaderVersion, ModelsV1.LoaderInfo, ModelsV1.LoaderLauncherInfo {
	/**
	 * /v2/versions/game
	 */
	static List<GameVersion> gameVersions(DataProvider dataProvider) {
		LinkedList<GameVersion> versions = new LinkedList<>();

		for (BaseVersion version : dataProvider.getGameVersions()) {
			versions.add(GameVersion.from(version));
		}

		return versions;
	}

	/**
	 * /v2/game/{game_version}
	 */
	static List<GameVersion> gameVersions(DataProvider dataProvider, String gameVersion) {
		for (BaseVersion version : dataProvider.getGameVersions()) {
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

		for (MavenBuildGameVersion version : dataProvider.getMappingVersions()) {
			versions.add(MappingVersion.from(version));
		}

		return versions;
	}

	/**
	 * /v2/mappings/{game_version}
	 */
	static List<MappingVersion> mappingVersions(DataProvider dataProvider, String gameVersion) {
		LinkedList<MappingVersion> versions = new LinkedList<>();

		for (MavenBuildGameVersion version : dataProvider.getMappingVersions()) {
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

		for (MavenBuildVersion version : dataProvider.getLoaderVersions()) {
			versions.add(LoaderVersion.from(version));
		}

		return versions;
	}

	/**
	 * /v2/versions/loader/{game_version}/{loader_version}
	 */
	static List<LoaderInfo> loaderInfo(DataProvider dataProvider, String gameVersion) {
		MavenBuildGameVersion mappings = null;

		for (MavenBuildGameVersion version : dataProvider.getMappingVersions()) {
			if (version.test(gameVersion)) {
				mappings = version;
				break;
			}
		}

		if (mappings == null) {
			return Collections.emptyList();
		}

		List<LoaderInfo> infoList = new LinkedList<>();

		for (MavenBuildVersion loader : dataProvider.getLoaderVersions()) {
			infoList.add(new LoaderInfo(LoaderVersion.from(loader), MappingVersion.from(mappings)));
		}

		return infoList;
	}

	/**
	 * /v2/versions/loader/{game_version}/{loader_version}
	 */
	static LoaderLauncherInfo loaderLauncherInfo(DataProvider dataProvider, String gameVersion, String loaderVersion) {
		MavenBuildVersion loader = null;
		MavenBuildGameVersion mappings = null;

		for (MavenBuildVersion version : dataProvider.getLoaderVersions()) {
			if (loaderVersion.equals(version.getVersion())) {
				loader = version;
				break;
			}
		}

		for (MavenBuildGameVersion version : dataProvider.getMappingVersions()) {
			if (version.test(gameVersion)) {
				mappings = version;
				break;
			}
		}

		if (loader == null) {
			throw new BadRequestResponse("no loader version found for " + gameVersion);
		}

		if (mappings == null) {
			throw new BadRequestResponse("no mappings version found for " + gameVersion);
		}

		final JsonObject installerJson = dataProvider.getLoaderInstallerJson(loader.getMaven());

		if (installerJson == null) {
			throw new InternalServerErrorResponse("Failed to load installer json, report to Fabric");
		}

		return new LoaderLauncherInfo(
				LoaderVersion.from(loader),
				MappingVersion.from(mappings),
				installerJson
		);
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

	record LoaderInfo(LoaderVersion loader, MappingVersion mappings) implements ModelsV1 { }

	record LoaderLauncherInfo(LoaderVersion loader, MappingVersion mappings, Object launcherMeta) implements ModelsV1 { }
}
