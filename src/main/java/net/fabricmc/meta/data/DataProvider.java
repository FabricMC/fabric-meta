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

package net.fabricmc.meta.data;

import java.util.List;

import com.google.gson.JsonObject;

import net.fabricmc.meta.models.BaseVersion;
import net.fabricmc.meta.models.MavenBuildGameVersion;
import net.fabricmc.meta.models.MavenBuildVersion;
import net.fabricmc.meta.models.MavenVersion;
import net.fabricmc.meta.utils.LoaderMeta;

public interface DataProvider {
	@Deprecated // TODO work to remove
	VersionDatabase getVersionDatabase();

	default List<BaseVersion> getGameVersions() {
		return getVersionDatabase().game;
	}

	default List<MavenBuildGameVersion> getMappingVersions() {
		return getVersionDatabase().mappings;
	}

	default List<MavenVersion> getIntermediaryVersions() {
		return getVersionDatabase().intermediary;
	}

	default List<MavenBuildVersion> getLoaderVersions() {
		return getVersionDatabase().getLoader();
	}

	default JsonObject getLoaderInstallerJson(String mavenNotation) {
		return LoaderMeta.getMeta(mavenNotation);
	}
}
