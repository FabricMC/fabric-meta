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

package net.fabricmc.meta.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.fabricmc.meta.web.models.BaseVersion;

import org.jetbrains.annotations.Nullable;

public class PomParser {
	public String path;
	public String prefix;
	public boolean loadHashes;

	public FullVersion latestVersion = null;
	public List<FullVersion> versions = new ArrayList<>();

	public PomParser(String path, String prefix, boolean loadHashes) {
		this.path = path;
		this.prefix = prefix;
		this.loadHashes = loadHashes;
	}

	private void load() throws IOException, XMLStreamException {
		versions.clear();

		URL url = new URL(path);
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(3000);
		conn.setReadTimeout(3000);
		conn.setRequestProperty("Connection", "Keep-Alive");
		XMLStreamReader reader = null;

		try {
			reader = XMLInputFactory.newInstance().createXMLStreamReader(conn.getInputStream());

			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("version")) {
					String text = reader.getElementText();
					String version = this.prefix + text;

					String[] split = version.split(":");
					String versionUrl = String.format("%s%s/%s/%s/%s-%s.jar", Reference.FABRIC_MAVEN_URL,
						split[0].replace('.', '/'),
						split[1],
						split[2],
						split[1],
						split[2]
					);

					CompletableFuture<String> md5 = this.loadHashes ? CompletableFuture.supplyAsync(() -> loadHash(url + ".md5", 32)) : CompletableFuture.completedFuture(null);
					CompletableFuture<String> sha1 = this.loadHashes ? CompletableFuture.supplyAsync(() -> loadHash(url + ".sha1", 40)) : CompletableFuture.completedFuture(null);
					CompletableFuture<String> sha256 = this.loadHashes ? CompletableFuture.supplyAsync(() -> loadHash(url + ".sha256", 64)) : CompletableFuture.completedFuture(null);
					CompletableFuture<String> sha512 = this.loadHashes ? CompletableFuture.supplyAsync(() -> loadHash(url + ".sha512", 128)) : CompletableFuture.completedFuture(null);

					versions.add(new FullVersion(version, versionUrl, md5.join(), sha1.join(), sha256.join(), sha512.join()));
				}
			}
		} finally {
			if (reader != null) reader.close();
		}

		Collections.reverse(versions);
		latestVersion = versions.get(0);
	}

	private @Nullable String loadHash(String urlStr, int expectedLength) {
		try {
			HttpURLConnection conn = null;
			InputStream inputStream = null;
			try {
				URL url = new URL(urlStr);
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setReadTimeout(3000);
				conn.setRequestProperty("Connection", "Keep-Alive");
				inputStream = conn.getInputStream();
				String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
				if (result.length() == expectedLength) {
					return result;
				}
			} finally {
				if (inputStream != null) inputStream.close();
				if (conn != null) conn.disconnect();
			}
		} catch (IOException ignored) {
		}
		return null;
	}

	public <T extends BaseVersion> List<T> getMeta(BaseVersionConstructor<T> function) throws IOException, XMLStreamException {
		return getMeta(function, list -> {
			if (!list.isEmpty()) list.get(0).setStable(true);
		});
	}

	public <T extends BaseVersion> List<T> getMeta(BaseVersionConstructor<T> function, StableVersionIdentifier stableIdentifier) throws IOException, XMLStreamException {
		try {
			load();
		} catch (IOException e) {
			throw new IOException("Failed to load " + path, e);
		}

		List<T> list = versions.stream()
				.map(v -> function.create(v.version, v.url, v.md5, v.sha1, v.sha256, v.sha512))
				.collect(Collectors.toList());

		Path unstableVersionsPath = Paths.get(this.prefix
				.replace(":", "_")
				.replace(".", "_")
				.replaceFirst(".$", "")
				+ ".txt");

		if (Files.exists(unstableVersionsPath)) {
			// Read a file containing a new line separated list of versions that should not be marked as stable.
			List<String> unstableVersions = Files.readAllLines(unstableVersionsPath);
			list.stream()
					.filter(v -> !unstableVersions.contains(v.getVersion()))
					.findFirst()
					.ifPresent(v -> v.setStable(true));
		} else {
			stableIdentifier.process(list);
		}

		return Collections.unmodifiableList(list);
	}

	public class FullVersion {
		public final String version;
		public final String url;
		public final @Nullable String md5;
		public final @Nullable String sha1;
		public final @Nullable String sha256;
		public final @Nullable String sha512;

		public FullVersion(String version, String url, @Nullable String md5, @Nullable String sha1, @Nullable String sha256, @Nullable String sha512) {
			this.version = version;
			this.url = url;
			this.md5 = md5;
			this.sha1 = sha1;
			this.sha256 = sha256;
			this.sha512 = sha512;
		}
	}

	public interface BaseVersionConstructor<T extends BaseVersion> {
		T create(String version, String url, @Nullable String md5, @Nullable String sha1, @Nullable String sha256, @Nullable String sha512);
	}

	public interface StableVersionIdentifier {
		void process(List<? extends BaseVersion> versions);
	}
}
