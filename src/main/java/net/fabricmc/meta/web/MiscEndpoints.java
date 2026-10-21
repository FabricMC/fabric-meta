/*
 * Copyright (c) 2026 FabricMC
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

package net.fabricmc.meta.web;

public class MiscEndpoints {
	public static void setup() {
		WebServer.get("/misc/data/mcupdater", "text/xml", McUpdater::handleLegacy);
		WebServer.get("/misc/data/mcupdater/{game_version}/{loader_version}", "text/xml", McUpdater::handle);
	}
}
