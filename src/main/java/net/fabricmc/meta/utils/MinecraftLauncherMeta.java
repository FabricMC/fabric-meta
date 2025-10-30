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

package net.fabricmc.meta.utils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.meta.data.VersionDatabase;

public class MinecraftLauncherMeta {
	// cache to allow meta to start up even while mc's servers are flaky
	private static final Path MC_VERSIONS_CACHE = Paths.get("cache", "version_manifest.json");
	private static final Gson GSON = new GsonBuilder().create();
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionDatabase.class);

	List<Version> versions;

	private MinecraftLauncherMeta() {
	}

	private MinecraftLauncherMeta(List<Version> versions) {
		this.versions = versions;
	}

	public static MinecraftLauncherMeta getMeta(boolean allowCache) throws IOException {
		String cachedJson = Files.exists(MC_VERSIONS_CACHE) ? Files.readString(MC_VERSIONS_CACHE) : null;
		String url = Reference.MC_METADATA_URL;

		try {
			String json = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
			MinecraftLauncherMeta ret = GSON.fromJson(json, MinecraftLauncherMeta.class);
			if (ret.versions.isEmpty()) throw new IOException("received empty version list");

			// success, update cache if changed
			if (!json.equals(cachedJson)) {
				Files.createDirectories(MC_VERSIONS_CACHE.toAbsolutePath().getParent());
				Files.writeString(MC_VERSIONS_CACHE, json);
			}

			return ret;
		} catch (IOException | JsonParseException e) {
			if (allowCache && cachedJson != null) {
				LOGGER.warn("Error retrieving MC metadata, using local cache: {}", e.toString());

				return GSON.fromJson(cachedJson, MinecraftLauncherMeta.class);
			}

			throw e;
		}
	}

	public static MinecraftLauncherMeta getExperimentalMeta() throws IOException {
		String url = Reference.LOCAL_FABRIC_MAVEN_URL+"net/minecraft/experimental_versions.json";
		String json = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
		return GSON.fromJson(json, MinecraftLauncherMeta.class);
	}

	public static MinecraftLauncherMeta getAllMeta(boolean allowCache) throws IOException {
		List<Version> versions = new ArrayList<>();
		versions.addAll(getMeta(allowCache).versions);
		versions.addAll(getExperimentalMeta().versions);

		// Order by release time
		versions.sort(Comparator.comparing(Version::getReleaseTime).reversed());

		return new MinecraftLauncherMeta(versions);
	}

	public boolean isStable(String id) {
		return versions.stream().anyMatch(version -> version.id.equals(id) && version.type.equals("release"));
	}

	public int getIndex(String version) {
		for (int i = 0; i < versions.size(); i++) {
			if (versions.get(i).id.equals(version)) {
				return i;
			}
		}

		return 0;
	}

	public List<Version> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public static class Version {
		String id;
		String type;
		String url;
		String time;
		String releaseTime;

		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getUrl() {
			return url;
		}

		public String getTime() {
			return time;
		}

		public String getReleaseTime() {
			return releaseTime;
		}
	}
}
