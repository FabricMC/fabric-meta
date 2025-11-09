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

package net.fabricmc.meta.web.models;

import org.jetbrains.annotations.Nullable;

public class MavenBuildVersion extends MavenVersion {
	String separator;
	int build;

	public MavenBuildVersion(String maven, String url, @Nullable String md5, @Nullable String sha1, @Nullable String sha256, @Nullable String sha512) {
		super(maven, url, md5, sha1, sha256, sha512);
		String version = maven.split(":")[2];

		if (version.contains("+build.")) {
			separator = "+build.";
		} else {
			separator = ".";
		}

		build = Integer.parseInt(version.substring(version.lastIndexOf(".") + 1));
	}

	public String getSeparator() {
		return separator;
	}

	public int getBuild() {
		return build;
	}
}
