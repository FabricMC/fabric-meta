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

import java.util.List;

public class LegacyDbDump {
	public final List<BaseVersion> game;
	public final List<MavenBuildGameVersion> mappings;
	public final List<MavenVersion> intermediary;
	public final List<MavenBuildVersion> loader;
	public final List<MavenUrlVersion> installer;

	public LegacyDbDump(List<BaseVersion> game,
			List<MavenBuildGameVersion> mappings,
			List<MavenVersion> intermediary,
			List<MavenBuildVersion> loader,
			List<MavenUrlVersion> installer) {
		this.game = game;
		this.mappings = mappings;
		this.intermediary = intermediary;
		this.loader = loader;
		this.installer = installer;
	}
}
