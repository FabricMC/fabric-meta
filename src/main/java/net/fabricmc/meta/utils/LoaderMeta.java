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

import com.google.gson.JsonObject;
import net.fabricmc.meta.web.WebServer;
import net.fabricmc.meta.web.models.LoaderInfoBase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class LoaderMeta {

	public static final File BASE_DIR = new File("metadata");
	public static final String MAVEN_URL = "https://maven.fabricmc.net/";
	public static final String LEGACY_MAVEN_URL = "https://dl.bintray.com/legacy-fabric/Legacy-Fabric-Maven/";

	public static JsonObject getMeta(LoaderInfoBase loaderInfo, boolean guava){
		String loaderMaven = loaderInfo.getLoader().getMaven();
		String[] split = loaderMaven.split(":");
		String path = String.format("%s/%s/%s", split[0].replaceAll("\\.","/"), split[1], split[2]);
		String filename = String.format("%s-%s.json", split[1], split[2]);

		File launcherMetaFile = new File(BASE_DIR, path + "/" + filename);
		if(!launcherMetaFile.exists()){
			try {
				String maven = guava ? LEGACY_MAVEN_URL : MAVEN_URL;
				String url = String.format("%s%s/%s", maven, path, filename);
				System.out.println("Downloading " + url);
				FileUtils.copyURLToFile(new URL(url), launcherMetaFile);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		try {
			JsonObject jsonObject = WebServer.GSON.fromJson(new FileReader(launcherMetaFile), JsonObject.class);
			return jsonObject;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
