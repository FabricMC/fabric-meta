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

package net.legacyfabric.meta.web.models;

import net.fabricmc.meta.web.models.MavenUrlVersion;

import net.legacyfabric.meta.utils.LegacyReference;

public class LegacyMavenUrlVersion extends MavenUrlVersion {
	public LegacyMavenUrlVersion(String maven) {
		super(maven);
	}

	@Override
	public String getMavenUrl() {
		return LegacyReference.LEGACY_FABRIC_MAVEN_URL;
	}
}
