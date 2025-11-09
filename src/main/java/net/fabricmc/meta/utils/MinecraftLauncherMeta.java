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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.data.VersionDatabase;

public class MinecraftLauncherMeta {
	private static final boolean EMULATE_OLD = false; // temporary to make comparison tests happy

	// version manifest "type" variants to sort above the other ones (typically from fabric's exp release manifest)
	private static final Set<String> HIGH_PRIORITY_TYPES = EMULATE_OLD ? Collections.emptySet() : Set.of("release", "snapshot");

	private static final String EXTRA_META_URL = System.getProperty("extraMcMetaUrl");
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionDatabase.class);

	private final List<Version> versions;
	private final Map<String, Integer> index;

	private MinecraftLauncherMeta(List<Version> versions) {
		this.versions = versions;

		index = new HashMap<>(versions.size());

		for (int i = 0; i < versions.size(); i++) {
			index.put(versions.get(i).id(), i);
		}
	}

	private static List<Version> getMeta(String url, boolean readFromCache, boolean writeToCache) throws IOException {
		Path cacheFile = readFromCache || writeToCache ? FabricMeta.CACHE_DIR.resolve(url.substring(url.lastIndexOf('/') + 1)) : null;
		String cachedJson = cacheFile != null && Files.exists(cacheFile) ? Files.readString(cacheFile) : null;

		try {
			String json = IOUtils.toString(URI.create(url), StandardCharsets.UTF_8);
			List<Version> ret = parse(json);

			if (writeToCache) {
				if (ret.isEmpty()) throw new IOException("received empty version list"); // protect against overwriting cache with nothing

				// success, update cache if changed
				if (!json.equals(cachedJson)) { // cachedJson may be null
					Files.createDirectories(cacheFile.toAbsolutePath().getParent());
					Files.writeString(cacheFile, json);
				}
			}

			return ret;
		} catch (Exception e) {
			if (readFromCache && cachedJson != null) {
				LOGGER.warn("Error retrieving MC metadata, using local cache: {}", e.toString());

				return parse(cachedJson);
			}

			throw e;
		}
	}

	private static List<Version> parse(String json) throws IOException {
		MclMetaVersionManifest parsed = FabricMeta.GSON.fromJson(json, MclMetaVersionManifest.class);
		List<Version> ret = new ArrayList<>(parsed.versions.size());

		for (MclMetaVersionManifest.Version version : parsed.versions) {
			byte[] hash = version.sha1() != null ? HexFormat.of().parseHex(version.sha1()) : null;
			OffsetDateTime time = OffsetDateTime.parse(version.releaseTime());
			boolean obfuscated = FabricMeta.MC_OBFUSCATION_CHECKER.isObfuscated(version.id(), version.url(), hash, time);
			if (EMULATE_OLD && !obfuscated) continue;

			ret.add(new Version(version.id(),
					version.type(),
					version.url(),
					hash,
					time,
					obfuscated));
		}

		return ret;
	}

	static final class MclMetaVersionManifest {
		public List<Version> versions;

		public record Version(String id, String type, String url, String releaseTime, String sha1) { }
	}

	public static MinecraftLauncherMeta getAllMeta(boolean allowCacheRead) throws IOException {
		List<Version> versions = new ArrayList<>();
		// use cache to allow meta to start up even while mc's servers are flaky
		versions.addAll(getMeta(Reference.MC_METADATA_URL, allowCacheRead, true));
		versions.addAll(getMeta(Reference.LOCAL_FABRIC_MAVEN_URL+"net/minecraft/experimental_versions.json", false, false));

		if (EXTRA_META_URL != null) {
			versions.addAll(getMeta(EXTRA_META_URL, false, false));
		}

		// Order by type priority, then release time
		versions.sort(Comparator
				.<Version>comparingInt(v -> HIGH_PRIORITY_TYPES.contains(v.type()) ? 0 : 1)
				.thenComparing(Comparator.comparing(Version::releaseTime).reversed()));

		return new MinecraftLauncherMeta(versions);
	}

	public List<Version> getVersions() {
		return versions;
	}

	public Version getVersion(String id) {
		Integer idx = index.get(id);

		return idx != null ? versions.get(idx) : null;
	}

	public int getIndex(String version) {
		return index.getOrDefault(version, -1);
	}

	public record Version(String id, String type, String url, byte[] sha1, OffsetDateTime releaseTime, boolean obfuscated) {
		public boolean isStable() {
			return type.equals("release");
		}
	}
}
