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

import net.fabricmc.meta.FabricMeta;

public final class Reference {
	/**
	 * Fabric maven url to expose to the user.
	 *
	 * <p>This shouldn't be directly accessed by this meta server instance!
	 */
	public static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/";

	/**
	 * Fabric maven url to access from this meta server instance.
	 *
	 * <p>This is not to be included in any output data!
	 */
	public static final String LOCAL_FABRIC_MAVEN_URL = FabricMeta.getConfig().getOrDefault("localFabricMavenUrl", FABRIC_MAVEN_URL);
}
