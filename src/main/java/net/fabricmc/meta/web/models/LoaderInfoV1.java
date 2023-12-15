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

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.meta.utils.LoaderMeta;

public class LoaderInfoV1 implements LoaderInfoBase {
	MavenBuildVersion loader;
	MavenBuildGameVersion mappings;

	@Nullable
	JsonObject launcherMeta;

	public LoaderInfoV1(MavenBuildVersion loader, MavenBuildGameVersion mappings) {
		this.loader = loader;
		this.mappings = mappings;
	}

	public LoaderInfoV1 populateMeta() {
		launcherMeta = LoaderMeta.getMeta(this);
		return this;
	}

	@Override
	public MavenBuildVersion getLoader() {
		return loader;
	}
}
