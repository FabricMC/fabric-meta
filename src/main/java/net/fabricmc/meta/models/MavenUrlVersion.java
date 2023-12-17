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

package net.fabricmc.meta.models;

import net.fabricmc.meta.utils.Reference;

public class MavenUrlVersion extends MavenVersion {
	public final String url;

	public MavenUrlVersion(String maven) {
		super(maven);
		String[] split = maven.split(":");
		this.url = String.format("%s%s/%s/%s/%s-%s.jar", Reference.FABRIC_MAVEN_URL,
				split[0].replace('.', '/'),
				split[1],
				split[2],
				split[1],
				split[2]
				);
	}
}
