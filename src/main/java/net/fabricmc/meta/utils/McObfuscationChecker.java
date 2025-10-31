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
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.IOUtils;

import net.fabricmc.meta.FabricMeta;

public class McObfuscationChecker {
	private static final Path FILE = FabricMeta.CACHE_DIR.resolve("obfuscated_mc_versions.json");
	private static final OffsetDateTime FIRST_MODERN_UNOBF_RELEASE = OffsetDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);

	private final Map<String, Entry> entries = new HashMap<>();
	private boolean dirty;

	public McObfuscationChecker() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				Collection<Entry> entries = FabricMeta.GSON.fromJson(reader, new TypeToken<Collection<Entry>>() { });

				for (Entry entry : entries) {
					this.entries.put(entry.version(), entry);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	public void save() throws IOException {
		if (!dirty) return;

		Files.createDirectories(FILE.getParent());

		try (Writer writer = Files.newBufferedWriter(FILE)) {
			FabricMeta.GSON.toJson(entries.values(), writer);
		}

		dirty = false;
	}

	public boolean isObfuscated(String version, String url, byte[] hash, OffsetDateTime releaseTime) throws IOException {
		if (releaseTime.isBefore(FIRST_MODERN_UNOBF_RELEASE)) return true;

		Entry entry = entries.get(version);

		if (entry != null
				&& (hash == null || entry.hash() == null || Arrays.equals(hash, entry.hash()))) {
			return entry.obfuscated();
		}

		boolean obfuscated = hasMojmapFiles(version, url);

		entries.put(version, new Entry(version, hash, obfuscated));
		dirty = true;

		return obfuscated;
	}

	private static boolean hasMojmapFiles(String id, String url) throws IOException {
		String json = IOUtils.toString(URI.create(url), StandardCharsets.UTF_8);

		try (JsonReader reader = new JsonReader(new StringReader(json))) {
			reader.beginObject();

			while (reader.hasNext()) {
				String key = reader.nextName();

				if (!key.equals("downloads")) {
					reader.skipValue();
					continue;
				}

				reader.beginObject();

				while (reader.hasNext()) {
					switch (reader.nextName()) {
					case "client_mappings":
					case "server_mappings":
						return true;
					default:
						reader.skipValue();
					}
				}

				reader.endObject();

				return false; // no downloads.client_mappings/server_mappings
			}

			reader.endObject();

			throw new IOException("missing downloads section in "+id+" version json");
		} catch (DateTimeParseException e) {
			throw new IOException("error parsing releaseTime: "+e);
		} catch (IllegalStateException e) {
			throw new IOException("error parsing json: "+e);
		}
	}

	private record Entry(String version, byte[] hash, boolean obfuscated) { }
}
