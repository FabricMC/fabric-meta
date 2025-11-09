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
import net.fabricmc.meta.web.models.LegacyDbDump;
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

	private Map<String, GameVersionData> gameVersionIndex;
	private List<BaseVersion> gameModels;
	private List<MavenVersion> intermediaries;
	private List<MavenBuildGameVersion> yarns;
	private List<MavenBuildVersion> loader;
	public List<MavenUrlVersion> installer;

	private VersionDatabase() { }

	public static VersionDatabase generate(boolean initial) throws IOException, XMLStreamException {
		long start = System.nanoTime();
		VersionDatabase database = new VersionDatabase();
		database.yarns = MAPPINGS_PARSER.getMeta(MavenBuildGameVersion::new, "net.fabricmc:yarn:");
		database.intermediaries = INTERMEDIARY_PARSER.getMeta(MavenVersion::new, "net.fabricmc:intermediary:");
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
		if (yarns == null || intermediaries == null) {
			throw new RuntimeException("Mappings are null");
		}

		MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getAllMeta(initial);

		List<MavenVersion> newIntermediary = new ArrayList<>();
		Map<String, MavenVersion> intermediaryIndex = new HashMap<>();

		for (MavenVersion v : intermediaries) {
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
		intermediaries = newIntermediary;

		Map<String, List<MavenBuildGameVersion>> yarnIndex = new HashMap<>();

		for (MavenBuildGameVersion v : yarns) {
			yarnIndex.computeIfAbsent(v.getGameVersion(), ignore -> new ArrayList<>()).add(v);
		}

		gameVersionIndex = new HashMap<>(launcherMeta.getVersions().size());
		gameModels = new ArrayList<>(launcherMeta.getVersions().size());

		for (Version version : launcherMeta.getVersions()) {
			MavenVersion intermediary = intermediaryIndex.get(version.id());

			if (intermediary == null && !version.obfuscated()) {
				intermediary = Reference.NOOP_INTERMEDIARY_VERSION;
			}

			if (intermediary != null) {
				BaseVersion exposedModel = new BaseVersion(version.id(), version.isStable());
				List<MavenBuildGameVersion> versionYarns = yarnIndex.getOrDefault(version.id(), Collections.emptyList());
				gameVersionIndex.put(version.id(), new GameVersionData(version, gameModels.size(), exposedModel, intermediary, versionYarns));
				gameModels.add(exposedModel);
			}
		}
	}

	public GameVersionData getGameData(String version) {
		return gameVersionIndex.get(version);
	}

	public List<BaseVersion> getGameModels() {
		return gameModels;
	}

	public record GameVersionData(Version version, int index,
			BaseVersion exposedModel,
			MavenVersion intermediary, // Reference.NOOP_INTERMEDIARY_VERSION for unobf
			List<MavenBuildGameVersion> yarn) { }

	public List<MavenVersion> getIntermediaryModels() {
		return intermediaries;
	}

	public List<MavenBuildGameVersion> getYarnModels() {
		return yarns;
	}

	public MavenBuildVersion getLoader(String version) {
		for (MavenBuildVersion v : loader) {
			if (v.getVersion().equals(version)) {
				return v;
			}
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
		return loader;
	}

	public LegacyDbDump createLegacyDbDump() {
		return new LegacyDbDump(gameModels, yarns, intermediaries, loader, installer);
	}
}
