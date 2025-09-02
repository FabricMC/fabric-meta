/*
 * Copyright (c) 2021-2024 Legacy Fabric
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

package net.legacyfabric.meta.web;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.javalin.http.Context;
import org.apache.commons.io.IOUtils;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.utils.MinecraftLauncherMeta;
import net.fabricmc.meta.web.EndpointsV2;
import net.fabricmc.meta.web.WebServer;

import net.legacyfabric.meta.BuildConstants;

public class LegacyEndpointsV2 extends EndpointsV2 {
	public static void setup() {
		WebServer.jsonGet("/v2/meta", () -> BuildConstants.VERSION);

		LegacyWebServer.stringGet("/v2/manifest/{game_version}", LegacyEndpointsV2::getVersionManifest);
		WebServer.jsonGet("/v2/versions/manifest", context -> FabricMeta.database.launcherMeta);
		MMCInstance.setup();
		YarnDiff.setup();
	}

	private static String getVersionManifest(Context context) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}

		String gameVersion = context.pathParam("game_version");

		MinecraftLauncherMeta.Version version = FabricMeta.database.launcherMeta.getVersions().stream()
				.filter(v -> Objects.equals(v.getId(), gameVersion))
				.findFirst().orElse(null);

		String json = null;

		if (version != null) {
			try {
				json = IOUtils.toString(new URL(version.getUrl()), StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return json;
	}
}
