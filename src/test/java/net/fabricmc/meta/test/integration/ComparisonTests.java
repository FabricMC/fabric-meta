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

package net.fabricmc.meta.test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

import io.javalin.testtools.HttpClient;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.WebServer;

// Tests that the local response matches the remote response of the version in prod
public class ComparisonTests {
	private static final String REMOTE_FABRIC_META_URL = "https://meta.fabricmc.net";

	@BeforeAll
	static void beforeAll() {
		FabricMeta.setupForTesting();
	}

	public static Stream<Arguments> provideEndpoints() {
		return Stream.of(
				// V1
				"/v1/versions",
				"/v1/versions/game",
				"/v1/versions/game/1.14.4",
				"/v1/versions/mappings",
				"/v1/versions/mappings/1.16.5",
				"/v1/versions/loader",
				"/v1/versions/loader/1.20.4",
				"/v1/versions/loader/1.20.4/0.15.2",

				// V2
				"/v2/versions",
				"/v2/versions/game",
				"/v2/versions/game/yarn",
				"/v2/versions/game/intermediary",
				"/v2/versions/yarn",
				"/v2/versions/yarn/1.20.4",
				"/v2/versions/intermediary",
				"/v2/versions/intermediary/1.20.4",
				"/v2/versions/loader",
				"/v2/versions/loader?limit=5",
				"/v2/versions/loader?limit=5&skip=5",
				// Disabled as this forces all the load metadata to be downloaded, timing out the test.
				//"/v2/versions/loader/1.20.4",
				"/v2/versions/loader/1.20.4/0.15.2",
				"/v2/versions/installer"
				// Disabled as this includes the release time, and is not stable
				//"/v2/versions/loader/1.20.4/0.15.2/profile/json"
		).map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("provideEndpoints")
	void compareEndpoint(String endpoint) {
		JavalinTest.test(WebServer.create(), (server, client) -> {
			compareEndpoint(endpoint, client);
		});
	}

	private static void compareEndpoint(String endpoint, HttpClient client) throws Exception {
		Response response = client.get(endpoint);
		assertEquals(200, response.code());
		String localResponse = response.body().string();

		String remoteResponse = getRemoteEndpoint(endpoint);
		assertEquals(remoteResponse, localResponse);
	}

	private static String getRemoteEndpoint(String endpoint) throws Exception {
		try (var httpClient = java.net.http.HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(REMOTE_FABRIC_META_URL + endpoint))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			assertEquals(200, response.statusCode());
			return response.body();
		}
	}
}
