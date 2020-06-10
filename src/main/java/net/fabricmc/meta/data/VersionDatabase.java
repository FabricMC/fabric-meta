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
import net.fabricmc.meta.web.models.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class VersionDatabase {

	public static final String MAVEN_URL = "https://maven.fabricmc.net/";

	public static final PomParser MAPPINGS_PARSER = new PomParser(MAVEN_URL + "net/fabricmc/yarn/maven-metadata.xml");
	public static final PomParser INTERMEDIARY_PARSER = new PomParser(MAVEN_URL + "net/fabricmc/intermediary/maven-metadata.xml");
	public static final PomParser LOADER_PARSER = new PomParser(MAVEN_URL + "net/fabricmc/fabric-loader/maven-metadata.xml");
	public static final PomParser INSTALLER_PARSER = new PomParser(MAVEN_URL + "net/fabricmc/fabric-installer/maven-metadata.xml");

	public List<BaseVersion> game;
	public List<MavenBuildGameVersion> mappings;
	public List<MavenVersion> intermediary;
	public List<MavenBuildVersion> loader;
	public List<MavenUrlVersion> installer;

	private VersionDatabase() {
	}

	public static VersionDatabase generate() throws IOException, XMLStreamException {
		long start = System.currentTimeMillis();
		VersionDatabase database = new VersionDatabase();
		database.mappings = MAPPINGS_PARSER.getMeta(MavenBuildGameVersion::new, "net.fabricmc:yarn:");
		database.intermediary = INTERMEDIARY_PARSER.getMeta(MavenVersion::new, "net.fabricmc:intermediary:");
		database.loader = LOADER_PARSER.getMeta(MavenBuildVersion::new, "net.fabricmc:fabric-loader:");
		database.installer = INSTALLER_PARSER.getMeta(MavenUrlVersion::new, "net.fabricmc:fabric-installer:");
		database.loadMcData();
		System.out.println("DB update took " + (System.currentTimeMillis() - start) + "ms");
		return database;
	}

	private void loadMcData() throws IOException {
		if (mappings == null || intermediary == null) {
			throw new RuntimeException("Mappings are null");
		}
		MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getMeta();

		//Sorts in the order of minecraft release dates
		intermediary = new ArrayList<>(intermediary);
		intermediary.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersion())));
		intermediary.forEach(version -> version.setStable(true));

		List<String> minecraftVersions = new ArrayList<>();
		for (MavenVersion gameVersion : intermediary) {
			if (!minecraftVersions.contains(gameVersion.getVersion())) {
				minecraftVersions.add(gameVersion.getVersion());
			}
		}

		//1.14_combat-0/1.14_combat-3 was floating around the list as it doesnt have an official release date, this forces it to the bottom of the list.
		String oldMcVersion = "combat";
		minecraftVersions.sort((o1, o2) -> {
			if (o1.contains(oldMcVersion) && !o2.contains(oldMcVersion)) {
				return 1;
			} else if (!o1.contains(oldMcVersion) && o2.contains(oldMcVersion)) {
				return -1;
			}
			return 0;
		});

		game = minecraftVersions.stream().map(s -> new BaseVersion(s, launcherMeta.isStable(s))).collect(Collectors.toList());
	}

}
