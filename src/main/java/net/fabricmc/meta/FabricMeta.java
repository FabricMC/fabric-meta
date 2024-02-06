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

package net.fabricmc.meta;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.stream.JsonReader;
import net.legacyfabric.meta.data.LegacyVersionDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.meta.data.VersionDatabase;
import net.fabricmc.meta.utils.Reference;
import net.fabricmc.meta.web.WebServer;

public class FabricMeta {
	public static volatile VersionDatabase database;

	private static final Logger LOGGER = LoggerFactory.getLogger(VersionDatabase.class);
	private static final Map<String, String> config = new HashMap<>();
	private static boolean configInitialized;
	private static URL heartbeatUrl; // URL pinged with every successful update()

	public static void main(String[] args) {
		Path configFile = Paths.get("config.json");

		if (Files.exists(configFile)) {
			try (JsonReader reader = new JsonReader(Files.newBufferedReader(configFile))) {
				reader.beginObject();

				while (reader.hasNext()) {
					config.put(reader.nextName(), reader.nextString());
				}

				reader.endObject();

				String heartbeatUrlString = config.get("heartbeatUrl");

				if (heartbeatUrlString != null) {
					heartbeatUrl = new URL(heartbeatUrlString);
				}
			} catch (IOException | IllegalStateException e) {
				throw new RuntimeException("malformed config in "+configFile, e);
			}
		}

		configInitialized = true;

		LOGGER.info("Starting with local maven {}", Reference.LOCAL_FABRIC_MAVEN_URL);

		update();

		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(FabricMeta::update, 1, 1, TimeUnit.MINUTES);

		WebServer.start();
	}

	private static void update(){
		try {
			database = VersionDatabase.generate(new LegacyVersionDatabase());
			updateHeartbeat();
		} catch (Throwable t) {
			if (database == null){
				throw new RuntimeException(t);
			} else {
				LOGGER.warn("update failed", t);
			}
		}
	}

	private static void updateHeartbeat() {
		if (heartbeatUrl == null) return;

		try {
			HttpURLConnection conn = (HttpURLConnection) heartbeatUrl.openConnection();
			conn.setRequestMethod("HEAD");
			conn.setConnectTimeout(500);
			conn.setReadTimeout(500);

			int status = conn.getResponseCode();

			if (status != HttpURLConnection.HTTP_OK) {
				LOGGER.warn("heartbeat request failed with status {}", status);
			}
		} catch (IOException e) {
			LOGGER.warn("heartbeat request failed: {}", e.toString());
		}
	}

	public static Map<String, String> getConfig() {
		if (!configInitialized) throw new IllegalStateException("accessing config before initialization"); // to catch any accidental early access through <clinit> etc

		return config;
	}
}
