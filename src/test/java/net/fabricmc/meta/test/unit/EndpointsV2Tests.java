/*
 * Copyright (c) 2023 FabricMC
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

package net.fabricmc.meta.test.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.WebServer;

public class EndpointsV2Tests {
	@BeforeAll
	static void beforeAll() {
		// TODO provide a way to pass in dummy data for constant test results
		FabricMeta.setupForTesting();
	}

	@Test
	void versions() {
		JavalinTest.test(WebServer.create(), (server, client) -> {
			Response response = client.get("/v2/versions");
			assertEquals(200, response.code());
			String body = response.body().string();
		});
	}
}
