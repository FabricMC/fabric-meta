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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import net.fabricmc.meta.utils.LoaderMeta;
import net.fabricmc.meta.utils.Reference;
import net.fabricmc.meta.web.EndpointsV2.ProfileEnvironment;

public class ProfileHandler {
	private static final Executor EXECUTOR = Executors.newFixedThreadPool(2);
	private static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public static void setup() {
		EndpointsV2.fileDownload("client", "json", ProfileHandler::getFileName, ProfileHandler::profileJson);
		EndpointsV2.fileDownload("client", "zip", ProfileHandler::getFileName, ProfileHandler::profileZip);

		EndpointsV2.fileDownload("server", "json", ProfileHandler::getFileName, ProfileHandler::profileJson);
	}

	private static String getFileName(ProfileEnvironment env) {
		return String.format("%s.%s", getName(env), env.ext());
	}

	private static String getName(ProfileEnvironment env) {
		String loaderVersion = env.loader().getVersion();

		if (env.game().version().obfuscated()) {
			return String.format("fabric-loader-%s-%s", loaderVersion, env.game().intermediary().getVersion());
		} else {
			return String.format("fabric-loader-%s", loaderVersion);
		}
	}

	private static CompletableFuture<InputStream> profileJson(ProfileEnvironment env) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(env), EXECUTOR);
	}

	private static CompletableFuture<InputStream> profileZip(ProfileEnvironment env) {
		return profileJson(env)
				.thenApply(inputStream -> packageZip(env, inputStream));
	}

	private static InputStream packageZip(ProfileEnvironment env, InputStream profileJson) {
		String profileName = getName(env);

		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			try (ZipOutputStream zipStream = new ZipOutputStream(byteArrayOutputStream)) {
				//Write the profile json
				zipStream.putNextEntry(new ZipEntry(profileName + "/" + profileName + ".json"));
				IOUtils.copy(profileJson, zipStream);
				zipStream.closeEntry();

				//Write the dummy jar file
				zipStream.putNextEntry(new ZipEntry(profileName + "/" + profileName + ".jar"));
				zipStream.closeEntry();
			}

			//Is this really the best way??
			return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		} catch (IOException e) {
			throw new CompletionException(e);
		}
	}

	private static InputStream getProfileJsonStream(ProfileEnvironment env) {
		JsonObject jsonObject = buildProfileJson(env);
		return new ByteArrayInputStream(jsonObject.toString().getBytes());
	}

	//This is based of the installer code.
	private static JsonObject buildProfileJson(ProfileEnvironment env) {
		JsonObject launcherMeta = LoaderMeta.getMeta(env.loader());

		String profileName = getName(env);

		JsonObject librariesObject = launcherMeta.get("libraries").getAsJsonObject();
		// Build the libraries array with the existing libs + loader and intermediary
		JsonArray libraries = (JsonArray) librariesObject.get("common");

		if (env.game().version().obfuscated()) {
			libraries.add(formatLibrary(env.game().intermediary().getMaven(), Reference.FABRIC_MAVEN_URL));
		}

		libraries.add(formatLibrary(env.loader().getMaven(), Reference.FABRIC_MAVEN_URL));

		if (librariesObject.has(env.side())) {
			libraries.addAll(librariesObject.get(env.side()).getAsJsonArray());
		}

		String currentTime = ISO_8601.format(new Date());

		JsonObject profile = new JsonObject();
		profile.addProperty("id", profileName);
		profile.addProperty("inheritsFrom", env.game().version().id());
		profile.addProperty("releaseTime", currentTime);
		profile.addProperty("time", currentTime);
		profile.addProperty("type", "release");

		JsonElement mainClassElement = launcherMeta.get("mainClass");
		String mainClass;

		if (mainClassElement.isJsonObject()) {
			mainClass = mainClassElement.getAsJsonObject().get(env.side()).getAsString();
		} else {
			mainClass = mainClassElement.getAsString();
		}

		profile.addProperty("mainClass", mainClass);

		JsonObject arguments = new JsonObject();

		// I believe this is required to stop the launcher from complaining
		arguments.add("game", new JsonArray());

		if (env.side().equals("client")) {
			// add '-DFabricMcEmu= net.minecraft.client.main.Main ' to emulate vanilla MC presence for programs that check the process command line (discord, nvidia hybrid gpu, ..)
			JsonArray jvmArgs = new JsonArray();
			jvmArgs.add("-DFabricMcEmu= net.minecraft.client.main.Main ");
			arguments.add("jvm", jvmArgs);
		}

		profile.add("arguments", arguments);

		profile.add("libraries", libraries);

		return profile;
	}

	private static JsonObject formatLibrary(String mavenPath, String url) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("name", mavenPath);
		jsonObject.addProperty("url", url);
		return jsonObject;
	}
}
