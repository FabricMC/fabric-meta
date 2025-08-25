/*
 * Copyright (c) 2021-2025 Legacy Fabric
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

package net.legacyfabric.meta.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.javalin.http.InternalServerErrorResponse;
import org.jetbrains.annotations.NotNull;

import net.fabricmc.meta.web.EndpointsV2;
import net.fabricmc.meta.web.models.LoaderInfoV2;

import net.legacyfabric.meta.utils.LWJGLVersions;

public class MMCInstance {
	private static final Path CACHE_DIR = Paths.get("metadata", "mmc");
	private static final Executor WORKER_EXECUTOR = Executors.newSingleThreadExecutor();

	public static void setup() {
		EndpointsV2.fileDownload("instance", "zip", MMCInstance::getZipFileName, MMCInstance::instanceZip);
	}

	private static String getZipFileName(LoaderInfoV2 infoV2) {
		return String.format("legacyfabric-%s+loader.%s.zip", infoV2.getIntermediary().getVersion(), infoV2.getLoader().getVersion());
	}

	private static CompletableFuture<InputStream> instanceZip(LoaderInfoV2 infoV2) {
		String fileName = getZipFileName(infoV2);
		Path instanceZip = CACHE_DIR.resolve(fileName);

		if (!Files.exists(instanceZip)) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					if (!Files.exists(instanceZip)) {
						generateInstanceZip(infoV2, instanceZip);
					}

					return Files.newInputStream(instanceZip);
				} catch (IOException e) {
					e.printStackTrace();
					throw new InternalServerErrorResponse("Failed to serve mmc instance zip");
				}
			}, WORKER_EXECUTOR);
		}

		try {
			return CompletableFuture.completedFuture(Files.newInputStream(instanceZip));
		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalServerErrorResponse("Failed to serve mmc instance zip");
		}
	}

	private static String readAndReplace(String template, LoaderInfoV2 infoV2, LWJGLVersions lwjgl) throws IOException {
		try (InputStream is = MMCInstance.class.getResourceAsStream("/template/" + template)) {
			return new String(is.readAllBytes())
					.replace("${minecraft_version}", infoV2.getIntermediary().getVersion())
					.replace("${loader_version}", infoV2.getLoader().getVersion())
					.replace("${lwjgl_name}", lwjgl.name)
					.replace("${lwjgl_uid}", lwjgl.uid)
					.replace("${lwjgl_version}", lwjgl.version);
		}
	}

	private static void generateInstanceZip(LoaderInfoV2 infoV2, Path instanceZip) throws IOException {
		Files.createDirectories(instanceZip.getParent());

		var lwjgl = Objects.equals(infoV2.getIntermediary().getVersion(), "1.13.2") ? LWJGLVersions.LWJGL3 : LWJGLVersions.LWJGL2;

		var files = getFileEntries(infoV2, lwjgl);

		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(instanceZip))) {
			// Add legacyfabric.png
			zos.putNextEntry(new ZipEntry("legacyfabric.png"));

			try (InputStream is = MMCInstance.class.getResourceAsStream("/template/legacyfabric.png")) {
				is.transferTo(zos);
			}

			zos.closeEntry();

			for (FileEntry entry : files) {
				zos.putNextEntry(new ZipEntry(entry.fileName));
				zos.write(readAndReplace(entry.templateName, infoV2, lwjgl).getBytes());
				zos.closeEntry();
			}
		}
	}

	private static @NotNull ArrayList<FileEntry> getFileEntries(LoaderInfoV2 infoV2, LWJGLVersions lwjgl) {
		var files = new ArrayList<FileEntry>();

		files.add(new FileEntry("instance.cfg"));
		files.add(new FileEntry("mmc-pack.json"));

		if (lwjgl == LWJGLVersions.LWJGL2) files.add(new FileEntry("patches/org.lwjgl.lwjgl.json"));

		String intermediaryPatch = switch (infoV2.getIntermediary().getVersion()) {
		case "1.6.4" -> "patches/net.fabricmc.intermediary.pre-1.7.json";
		case "1.5.2", "1.4.7", "1.3.2" -> "patches/net.fabricmc.intermediary.pre-1.6.json";
		default -> "patches/net.fabricmc.intermediary.json";
		};

		files.add(new FileEntry(intermediaryPatch, "patches/net.fabricmc.intermediary.json"));
		return files;
	}

	private record FileEntry(String templateName, String fileName) {
		FileEntry(String name) {
			this(name, name);
		}
	}
}
