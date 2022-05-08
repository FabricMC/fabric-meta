/*
 * Copyright (c) 2021 Legacy Fabric/Quilt
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
import net.fabricmc.meta.data.VersionDatabase;
import net.fabricmc.meta.web.WebServer;
import net.fabricmc.meta.web.models.LoaderInfoBase;
import net.fabricmc.meta.web.models.MavenBuildVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoaderMeta {

	public static final File BASE_DIR = new File("metadata");

	public static JsonObject getMeta(LoaderInfoBase loaderInfo){
		String loaderMaven = loaderInfo.getLoader().getMaven();
		String[] split = loaderMaven.split(":");
		String path = String.format("%s/%s/%s", split[0].replaceAll("\\.","/"), split[1], split[2]);
		String filename = String.format("%s-%s.json", split[1], split[2]);

		File launcherMetaFile = new File(BASE_DIR, path + "/" + filename);
		if(!launcherMetaFile.exists()){
			try {
				String url = String.format("%s%s/%s", VersionDatabase.UPSTREAM_MAVEN_URL, path, filename);
				System.out.println("Downloading " + url);
				FileUtils.copyURLToFile(new URL(url), launcherMetaFile);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		try {
			return WebServer.GSON.fromJson(new FileReader(launcherMetaFile), JsonObject.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static int compareVersions(String a, String b) {
		if (a == null) return -1;
		if (b == null) return 1;
		if (a.equals(b)) return 0;
		Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)+");
		Matcher matcherA = pattern.matcher(a);
		Matcher matcherB = pattern.matcher(b);
		if (!matcherA.find() || !matcherB.find()) {
			throw new IllegalArgumentException("Invalid version");
		}
		String[] aParts =  matcherA.group(0).split("\\.");
		String[] bParts =  matcherB.group(0).split("\\.");
		for (int i = 0; i < aParts.length || i < bParts.length; i++) {
			if (i >= aParts.length && !"0".equals(bParts[i])) return -1;
			if (i >= bParts.length && !"0".equals(aParts[i])) return 1;
			int cmp = Integer.compare(Integer.parseInt(aParts[i]), Integer.parseInt(bParts[i]));
			if (cmp != 0) return cmp;
		}
		return 0;
	}

	public static boolean canUse(String gameVersion, MavenBuildVersion loaderVersion) {
		try {
			return compareVersions(loaderVersion.getVersion(), "0.13.0") >= 0;
		} catch (IllegalArgumentException ignored) {
			return false;
		}
	}
}
