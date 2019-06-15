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

package net.fabricmc.meta.data;

import net.fabricmc.meta.utils.MinecraftLauncherMeta;
import net.fabricmc.meta.utils.PomParser;
import net.fabricmc.meta.web.models.BaseVersion;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;
import net.fabricmc.meta.web.models.MavenVersion;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class VersionDatabase {

	public static final PomParser MAPPINGS_PARSER = new PomParser("https://maven.fabricmc.net/net/fabricmc/yarn/maven-metadata.xml");
	public static final PomParser INTERMEDIARY_PARSER = new PomParser("https://maven.fabricmc.net/net/fabricmc/intermediary/maven-metadata.xml");
	public static final PomParser LOADER_PARSER = new PomParser("https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml");

	public List<BaseVersion> game;
	public List<MavenBuildGameVersion> mappings;
	public List<MavenVersion> intermedairy;
	public List<MavenBuildVersion> loader;

	private VersionDatabase() {
	}

	public static VersionDatabase generate() throws IOException, XMLStreamException {
		long start = System.currentTimeMillis();
		VersionDatabase database = new VersionDatabase();
		database.mappings = MAPPINGS_PARSER.getMeta(MavenBuildGameVersion.class, "net.fabricmc:yarn:");
		database.intermedairy = INTERMEDIARY_PARSER.getMeta(MavenVersion.class, "net.fabricmc:intermediary:");
		database.loader = LOADER_PARSER.getMeta(MavenBuildVersion.class, "net.fabricmc:fabric-loader:");
		database.loadMcData();
		System.out.println("DB update took " + (System.currentTimeMillis() - start) + "ms");
		return database;
	}

	private void loadMcData() throws IOException {
		if (mappings == null || intermedairy == null) {
			throw new RuntimeException("Mappings are null");
		}
		MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getMeta();

		//Sorts in the order of minecraft release dates
		intermedairy = new ArrayList<>(intermedairy);
		intermedairy.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersion())));
		intermedairy.forEach(version -> version.setStable(launcherMeta.isStable(version.getVersion())));

		List<String> minecraftVersions = new ArrayList<>();
		for (MavenVersion gameVersion : intermedairy) {
			if (!minecraftVersions.contains(gameVersion.getVersion())) {
				minecraftVersions.add(gameVersion.getVersion());
			}
		}

		game = minecraftVersions.stream().map(s -> new BaseVersion(s, launcherMeta.isStable(s))).collect(Collectors.toList());
	}

}
