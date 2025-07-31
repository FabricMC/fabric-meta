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

package net.fabricmc.meta.web.v1;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.get;

import java.time.Duration;

import io.javalin.apibuilder.EndpointGroup;

import net.fabricmc.meta.data.DataProvider;
import net.fabricmc.meta.web.Endpoint;

public class EndpointsV1 extends Endpoint {
	public EndpointsV1(DataProvider dataProvider) {
		super(dataProvider);
	}

	@Override
	public EndpointGroup routes() {
		return () -> {
			before(cache(Duration.ofMinutes(1)));

			get("versions", ctx -> jsonResult(ctx, dataProvider.getVersionDatabase()));
			get("versions/game", result(ModelsV1::gameVersions));
			get("versions/game/{game_version}", result("game_version", ModelsV1::gameVersions));
			get("versions/mappings", result(ModelsV1::mappingVersions));
			get("versions/mappings/{game_version}", result("game_version", ModelsV1::mappingVersions));
			get("versions/loader", result(ModelsV1::loaderVersions));
			get("versions/loader/{game_version}", result("game_version", ModelsV1::loaderInfo));
			get("versions/loader/{game_version}/{loader_version}", result("game_version", "loader_version", ModelsV1::loaderLauncherInfo));
		};
	}
}
