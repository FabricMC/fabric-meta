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

package net.fabricmc.meta.web;

import io.javalin.core.util.Header;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.InternalServerErrorResponse;
import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.models.BaseVersion;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerBootstrap {
    private static final Path CACHE_DIR = Paths.get("metadata", "installer");
    private static final Executor WORKER_EXECUTOR = Executors.newSingleThreadExecutor();

    public static void setup() {
        // http://localhost:5555/v2/versions/loader/1.17.1/0.12.0/0.8.0/server/jar
        WebServer.javalin.get("/v2/versions/loader/:game_version/:loader_version/:installer_version/server/jar", boostrapHandler());
    }

    private static Handler boostrapHandler() {
        return ctx -> {
            if (!ctx.queryParamMap().isEmpty()) {
                // Cannot really afford people to cache bust this.
                throw new BadRequestResponse("Query params not allowed on this endpoint.");
            }

            final String installerVersion = getAndValidateVersion(ctx, FabricMeta.database.installer, "installer_version");
            final String gameVersion = getAndValidateVersion(ctx, FabricMeta.database.intermediary, "game_version");
            final String loaderVersion = getAndValidateVersion(ctx, FabricMeta.database.getAllLoader(), "loader_version");

            validateLoaderVersion(loaderVersion);
            validateInstallerVersion(installerVersion);

            // Set the filename and cache headers
            final String filename = String.format("fabric-server-mc.%s-loader.%s-launcher.%s.jar", gameVersion, loaderVersion, installerVersion);
            ctx.header(Header.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", filename));
            ctx.header(Header.CACHE_CONTROL, "public, max-age=86400");
            ctx.contentType("application/java-archive");

            ctx.result(getResultStream(installerVersion, gameVersion, loaderVersion));
        };
    }

    private static <V extends BaseVersion> String getAndValidateVersion(Context ctx, List<V> versions, String name) {
        String version = ctx.pathParam(name);

        for (V v : versions) {
            if (v.getVersion().equals(version)) {
                return version;
            }
        }

        throw new BadRequestResponse("Unable to find valid version for " + name);
    }

    private static void validateLoaderVersion(String loaderVersion) {
        String[] versionSplit = loaderVersion.split("\\.");

        // future 1.x versions
        if (Integer.parseInt(versionSplit[0]) > 0) {
            return;
        }

        // 0.12.x or newer
        if (Integer.parseInt(versionSplit[1]) >= 12) {
            return;
        }

        throw new BadRequestResponse("Fabric loader 0.12 or higher is required for unattended server installs. Please use a newer fabric loader version, or the full installer.");
    }

    private static void validateInstallerVersion(String installerVersion) {
        String[] versionSplit = installerVersion.split("\\.");

        // future 1.x versions
        if (Integer.parseInt(versionSplit[0]) > 0) {
            return;
        }

        // 0.8.x or newer
        if (Integer.parseInt(versionSplit[1]) >= 8) {
            return;
        }

        throw new BadRequestResponse("Fabric Installer 0.8 or higher is required for unattended server installs.");
    }

    private static CompletableFuture<InputStream> getResultStream(String installerVersion, String gameVersion, String loaderVersion) {
        Path bundledJar = CACHE_DIR.resolve(String.format("fabric-server-mc.%s-loader.%s-launcher.%s.jar", gameVersion, loaderVersion, installerVersion));

        if (!Files.exists(bundledJar)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    if (!Files.exists(bundledJar)) {
                        Path installerJar = getInstallerJar(installerVersion);
                        writePropertiesToJar(installerJar, bundledJar, loaderVersion, gameVersion);
                    }

                    return Files.newInputStream(bundledJar);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new InternalServerErrorResponse("Failed to generate bundled jar");
                }
            }, WORKER_EXECUTOR);
        }

        try {
            return CompletableFuture.completedFuture(Files.newInputStream(bundledJar));
        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalServerErrorResponse("Failed to serve bundled jar");
        }
    }

    private static Path getInstallerJar(String installerVersion) throws IOException {
        Path installerJar = CACHE_DIR.resolve(String.format("fabric-installer-%s.jar", installerVersion));

        if (Files.exists(installerJar)) {
            return installerJar;
        }

        return downloadInstallerJar(installerJar, installerVersion);
    }

    private static Path downloadInstallerJar(Path jar, String installerVersion) throws IOException {
        final String url = String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%1$s/fabric-installer-%1$s-server.jar", installerVersion);

        System.out.println("Downloading: " + url);
        FileUtils.copyURLToFile(new URL(url), jar.toFile());
        return jar;
    }

    private static void writePropertiesToJar(Path inputJar, Path outputJar, String loaderVersion, String gameVersion) throws IOException {
        String data = String.format("fabric-loader-version=%s\ngame-version=%s", loaderVersion, gameVersion);

        Path workingFile = Paths.get(outputJar.toAbsolutePath() + ".tmp");
        Files.copy(inputJar, workingFile);

        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:" + workingFile.toUri());

        try (FileSystem zipFs = FileSystems.newFileSystem(uri, env)) {
            Files.write(zipFs.getPath("install.properties"), data.getBytes(StandardCharsets.UTF_8));
        }

        Files.copy(workingFile, outputJar);
        Files.delete(workingFile);
    }
}
