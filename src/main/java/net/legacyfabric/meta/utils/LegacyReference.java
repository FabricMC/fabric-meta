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

package net.legacyfabric.meta.utils;

import net.fabricmc.meta.FabricMeta;

public class LegacyReference {
	/**
	 * Legacy Fabric maven url to expose to the user.
	 *
	 * <p>This shouldn't be directly accessed by this meta server instance!
	 */
	public static final String LEGACY_FABRIC_MAVEN_URL = "https://maven.legacyfabric.net/";

	/**
	 * Legacy Fabric maven url to access from this meta server instance.
	 *
	 * <p>This is not to be included in any output data!
	 */
	public static final String LOCAL_LEGACY_FABRIC_MAVEN_URL = FabricMeta.getConfig().getOrDefault("localLegacyFabricMavenUrl", LEGACY_FABRIC_MAVEN_URL);
}
