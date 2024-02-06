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

package net.legacyfabric.meta.data;

import net.fabricmc.meta.data.VersionDatabase;
import net.fabricmc.meta.utils.PomParser;
import net.fabricmc.meta.web.models.BaseVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;
import net.fabricmc.meta.web.models.MavenUrlVersion;
import net.legacyfabric.meta.utils.LegacyReference;
import net.legacyfabric.meta.web.models.LegacyMavenUrlVersion;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LegacyVersionDatabase extends VersionDatabase {
    public static final PomParser MAPPINGS_PARSER = new PomParser(LegacyReference.LOCAL_LEGACY_FABRIC_MAVEN_URL + "net/legacyfabric/yarn/maven-metadata.xml");
    public static final PomParser INTERMEDIARY_PARSER = new PomParser(LegacyReference.LOCAL_LEGACY_FABRIC_MAVEN_URL + "net/legacyfabric/intermediary/maven-metadata.xml");
    public static final PomParser INSTALLER_PARSER = new PomParser(LegacyReference.LOCAL_LEGACY_FABRIC_MAVEN_URL + "net/legacyfabric/fabric-installer/maven-metadata.xml");

    public PomParser getMappingsParser() {
        return MAPPINGS_PARSER;
    }

    public String getMappingsPrefix() {
        return "net.legacyfabric:yarn:";
    }

    public PomParser getIntermediaryParser() {
        return INTERMEDIARY_PARSER;
    }

    public String getIntermediaryPrefix() {
        return "net.legacyfabric:intermediary:";
    }

    public PomParser getInstallerParser() {
        return INSTALLER_PARSER;
    }

    public String getInstallerPrefix() {
        return "net.legacyfabric:fabric-installer:";
    }

    @Override
    public Function<String, MavenUrlVersion> getMavenUrlVersionBuilder() {
        return LegacyMavenUrlVersion::new;
    }

    public List<MavenBuildVersion> getLoader() {
        return loader.stream().filter(LegacyVersionDatabase::isPublicLoaderVersion).collect(Collectors.toList());
    }

    private static boolean isPublicLoaderVersion(BaseVersion version) {
        String[] ver = version.getVersion().split("\\.");
        return Integer.parseInt(ver[1]) >= 13
                || Integer.parseInt(ver[0]) > 0;
    }
}
