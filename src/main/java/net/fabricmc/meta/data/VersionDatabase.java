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

import static net.fabricmc.meta.utils.Reference.LOCAL_FABRIC_MAVEN_URL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.meta.utils.MinecraftLauncherMeta;
import net.fabricmc.meta.utils.MinecraftLauncherMeta.Version;
import net.fabricmc.meta.utils.PomParser;
import net.fabricmc.meta.utils.Reference;
import net.fabricmc.meta.web.models.BaseVersion;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;
import net.fabricmc.meta.web.models.MavenUrlVersion;
import net.fabricmc.meta.web.models.MavenVersion;

public class VersionDatabase {
	private static final PomParser MAPPINGS_PARSER = new PomParser(LOCAL_FABRIC_MAVEN_URL + "net/fabricmc/yarn/maven-metadata.xml");
	private static final PomParser INTERMEDIARY_PARSER = new PomParser(LOCAL_FABRIC_MAVEN_URL + "net/fabricmc/intermediary/maven-metadata.xml");
	private static final PomParser LOADER_PARSER = new PomParser(LOCAL_FABRIC_MAVEN_URL + "net/fabricmc/fabric-loader/maven-metadata.xml");
	private static final PomParser INSTALLER_PARSER = new PomParser(LOCAL_FABRIC_MAVEN_URL + "net/fabricmc/fabric-installer/maven-metadata.xml");

	private static final Set<String> incorrectIntermediaryVersions = new HashSet<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionDatabase.class);

	public List<BaseVersion> game;
	public List<MavenBuildGameVersion> mappings;
	public List<MavenVersion> intermediary;
	private List<MavenBuildVersion> loader;
	public List<MavenUrlVersion> installer;

	private VersionDatabase() {
	}

	public static VersionDatabase generate(boolean initial) throws IOException, XMLStreamException {
		long start = System.nanoTime();
		VersionDatabase database = new VersionDatabase();
		database.mappings = MAPPINGS_PARSER.getMeta(MavenBuildGameVersion::new, "net.fabricmc:yarn:");
		database.intermediary = INTERMEDIARY_PARSER.getMeta(MavenVersion::new, "net.fabricmc:intermediary:");
		database.loader = LOADER_PARSER.getMeta(MavenBuildVersion::new, "net.fabricmc:fabric-loader:", list -> {
			for (BaseVersion version : list) {
				if (isPublicLoaderVersion(version)) {
					version.setStable(true);
					break;
				}
			}
		});
		database.installer = INSTALLER_PARSER.getMeta(MavenUrlVersion::new, "net.fabricmc:fabric-installer:");
		database.loadMcData(initial);
		LOGGER.info("DB update took {} ms", (System.nanoTime() - start) / 1_000_000);
		return database;
	}

	private void loadMcData(boolean initial) throws IOException {
		if (mappings == null || intermediary == null) {
			throw new RuntimeException("Mappings are null");
		}

		MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getAllMeta(initial);

		List<MavenVersion> newIntermediary = new ArrayList<>();
		Map<String, MavenVersion> intermediaryIndex = new HashMap<>();

		for (MavenVersion v : intermediary) {
			// Skip entries that do not match a valid mc version.
			if (launcherMeta.getVersion(v.getVersion()) == null) {
				// only print unmatched versions once so that it doesn't spam the console with "Removing ..." messages
				if (incorrectIntermediaryVersions.add(v.getVersion())) {
					LOGGER.warn("Removing intermediary for {} as it doesn't match a valid mc version", v.getVersion());
				}

				continue;
			}

			v.setStable(true);
			newIntermediary.add(v);
			intermediaryIndex.put(v.getVersion(), v);
		}

		//Sorts in the order of minecraft release dates
		newIntermediary.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersion())));
		intermediary = newIntermediary;

		game = new ArrayList<>(intermediary.size());

		for (Version version : launcherMeta.getVersions()) {
			if (!version.obfuscated()
					|| intermediaryIndex.containsKey(version.id())) {
				game.add(new BaseVersion(version.id(), version.isStable()));
			}
		}
	}

	BaseVersion getGame(String version) {
		for (BaseVersion v : game) {
			if (v.test(version)) return v;
		}

		return null;
	}

	public MavenVersion getIntermediary(String version, boolean substituteForUnobf) {
		for (MavenVersion v : intermediary) {
			if (v.test(version)) return v;
		}

		if (substituteForUnobf && getGame(version) != null) {
			return Reference.NOOP_INTERMEDIARY_VERSION;
		}

		return null;
	}

	public List<MavenBuildVersion> getLoader() {
		return loader.stream().filter(VersionDatabase::isPublicLoaderVersion).collect(Collectors.toList());
	}

	private static boolean isPublicLoaderVersion(BaseVersion version) {
		return true;
	}

	public List<MavenBuildVersion> getAllLoader() {
		return Collections.unmodifiableList(loader);
	}
}
