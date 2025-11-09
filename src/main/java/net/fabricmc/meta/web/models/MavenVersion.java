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

public class MavenVersion extends BaseVersion {
	String maven;
	String url;
	String md5;
	String sha1;
	String sha256;
	String sha512;

	public MavenVersion(String maven, String url, @Nullable String md5, @Nullable String sha1, @Nullable String sha256, @Nullable String sha512, boolean stable) {
		super(maven.split(":")[2], stable);
		this.maven = maven;
		this.url = url;
		this.md5 = md5;
		this.sha1 = sha1;
		this.sha256 = sha256;
		this.sha512 = sha512;
	}

	public MavenVersion(String maven, String url, @Nullable String md5, @Nullable String sha1, @Nullable String sha256, @Nullable String sha512) {
		this(maven, url, md5, sha1, sha256, sha512, false);
	}

	public String getMaven() {
		return maven;
	}
}
