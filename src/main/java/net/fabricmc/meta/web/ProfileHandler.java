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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.meta.utils.LoaderMeta;
import net.fabricmc.meta.web.models.LoaderInfoV2;
import org.apache.commons.io.IOUtils;

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

public class ProfileHandler {

	private static final Executor EXECUTOR = Executors.newFixedThreadPool(2);
	private static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public static void setup() {
		EndpointsV2.fileDownload("profile", "json", ProfileHandler::getJsonFileName, ProfileHandler::profileJson);
		EndpointsV2.fileDownload("profile", "zip", ProfileHandler::getZipFileName, ProfileHandler::profileZip);

		EndpointsV2.fileDownload("server", "json", ProfileHandler::getJsonFileName, ProfileHandler::serverJson);
	}

	private static String getJsonFileName(LoaderInfoV2 info) {
		return getJsonFileName(info, "json");
	}

	private static String getZipFileName(LoaderInfoV2 info) {
		return getJsonFileName(info, "zip");
	}

	private static String getJsonFileName(LoaderInfoV2 info, String ext) {
		return String.format("fabric-loader-%s-%s.%s", info.getLoader().getVersion(), info.getIntermediary().getVersion(), ext);
	}

	private static CompletableFuture<InputStream> profileJson(LoaderInfoV2 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(info, "client"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> serverJson(LoaderInfoV2 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(info, "server"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> profileZip(LoaderInfoV2 info) {
		return profileJson(info)
				.thenApply(inputStream -> packageZip(info, inputStream));
	}

	private static InputStream packageZip(LoaderInfoV2 info, InputStream profileJson)  {
		String profileName = String.format("fabric-loader-%s-%s", info.getLoader().getVersion(), info.getIntermediary().getVersion());

		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			try (ZipOutputStream zipStream = new ZipOutputStream(byteArrayOutputStream))  {
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

	private static InputStream getProfileJsonStream(LoaderInfoV2 info, String side) {
		JsonObject jsonObject = buildProfileJson(info, side);
		return new ByteArrayInputStream(jsonObject.toString().getBytes());
	}

	//This is based of the installer code.
	private static JsonObject buildProfileJson(LoaderInfoV2 info, String side) {
		JsonObject launcherMeta = info.getLauncherMeta();

		String profileName = String.format("fabric-loader-%s-%s", info.getLoader().getVersion(), info.getIntermediary().getVersion());

		JsonObject librariesObject = launcherMeta.get("libraries").getAsJsonObject();
		// Build the libraries array with the existing libs + loader and intermediary
		JsonArray libraries = (JsonArray) librariesObject.get("common");
		libraries.add(getLibrary(info.getIntermediary().getMaven(), LoaderMeta.MAVEN_URL));
		libraries.add(getLibrary(info.getLoader().getMaven(), LoaderMeta.MAVEN_URL));

		if (librariesObject.has(side)) {
			libraries.addAll(librariesObject.get(side).getAsJsonArray());
		}

		String currentTime = ISO_8601.format(new Date());

		JsonObject profile = new JsonObject();
		profile.addProperty("id", profileName);
		profile.addProperty("inheritsFrom", info.getIntermediary().getVersion());
		profile.addProperty("releaseTime", currentTime);
		profile.addProperty("time", currentTime);
		profile.addProperty("type", "release");

		profile.addProperty("mainClass", launcherMeta.get("mainClass").getAsJsonObject().get(side).getAsString());

		// I believe this is required to stop the launcher from complaining
		JsonObject arguments = new JsonObject();
		arguments.add("game", new JsonArray());
		profile.add("arguments", arguments);

		profile.add("libraries", libraries);

		return profile;
	}

	private static JsonObject getLibrary(String mavenPath, String url) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("name", mavenPath);
		jsonObject.addProperty("url", url);
		return jsonObject;
	}
}
