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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MinecraftLauncherMeta {

	public static final Gson GSON = new GsonBuilder().create();

	List<Version> versions;

	private MinecraftLauncherMeta() {
	}

	public static MinecraftLauncherMeta getMeta() throws IOException {
		String url = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
		String json = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
		return GSON.fromJson(json, MinecraftLauncherMeta.class);
	}

	public boolean isStable(String id) {
		return versions.stream().anyMatch(version -> version.id.equals(id) && version.type.equals("release"));
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
