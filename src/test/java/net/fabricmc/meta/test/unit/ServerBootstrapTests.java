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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.WebServer;

public class ServerBootstrapTests {
	@TempDir
	static Path tempDir;

	@BeforeAll
	static void beforeAll() {
		// TODO provide a way to pass in dummy data for constant test results
		FabricMeta.setupForTesting();
	}

	@Test
	void serverJar() {
		JavalinTest.test(WebServer.create(), (server, client) -> {
			Response response = client.get("/v2/versions/loader/stable/stable/stable/server/jar");
			assertEquals(200, response.code());
			Path jarFile = tempDir.resolve("server.jar");
			Files.copy(response.body().byteStream(), jarFile);
			assertTrue(Files.size(jarFile) > 0);
		});
	}
}
