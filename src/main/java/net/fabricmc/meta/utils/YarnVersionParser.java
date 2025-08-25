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

public class YarnVersionParser {
	private String mappingsVersion;
	private String minecraftVersion;

	private String version;

	public YarnVersionParser(String version) {
		this.version = version;

		if (version.contains("+build.")) {
			this.minecraftVersion = version.substring(0, version.lastIndexOf('+'));
			this.mappingsVersion = version.substring(version.lastIndexOf('.') + 1);
		} else {
			//TODO legacy remove when no longer needed
			char verSep = version.contains("-") ? '-' : '.';
			this.minecraftVersion = version.substring(0, version.lastIndexOf(verSep));
			this.mappingsVersion = version.substring(version.lastIndexOf(verSep) + 1);
		}
	}

	public String getMappingsVersion() {
		return mappingsVersion;
	}

	public String getMinecraftVersion() {
		return minecraftVersion;
	}

	@Override
	public String toString() {
		return version;
	}
}
