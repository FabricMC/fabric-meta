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

public class ServerBoostrap {
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

            final CompletableFuture<InputStream> future = getBootstrapPath(installerVersion, gameVersion, loaderVersion)
                    .thenApplyAsync(ServerBoostrap::readInstallerJar);

            // Set the filename and cache headers
            final String filename = String.format("fabric-server-mc.%s-loader.%s-installer.%s.jar", gameVersion, loaderVersion, installerVersion);
            ctx.header(Header.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", filename));
            ctx.header(Header.CACHE_CONTROL, "public, max-age=86400");
            ctx.contentType("application/java-archive");

            ctx.result(future);
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

    private static CompletableFuture<Path> getBootstrapPath(String installerVersion, String gameVersion, String loaderVersion) {
        Path bundledJar = CACHE_DIR.resolve(String.format("fabric-server+mc.%s-loader.%s-installer.%s.jar", gameVersion, loaderVersion, installerVersion));

        if (Files.exists(bundledJar)) {
            return CompletableFuture.completedFuture(bundledJar);
        }

        return getInstallerJar(installerVersion)
                .thenApplyAsync(inputJar -> {
                    try {
                        writePropertiesToJar(inputJar, bundledJar, loaderVersion, gameVersion);
                        return bundledJar;
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new InternalServerErrorResponse("Failed to write properties to jar");
                    }
                }, WORKER_EXECUTOR);
    }

    private static CompletableFuture<Path> getInstallerJar(String installerVersion) {
        Path installerJar = CACHE_DIR.resolve(String.format("fabric-installer-%s.jar", installerVersion));

        if (Files.exists(installerJar)) {
            return CompletableFuture.completedFuture(installerJar);
        }

        return downloadInstallerJar(installerJar, installerVersion);
    }

    private static InputStream readInstallerJar(Path jar) {
        try {
            return Files.newInputStream(jar);
        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalServerErrorResponse("Failed to read installer jar");
        }
    }

    private static CompletableFuture<Path> downloadInstallerJar(Path jar, String installerVersion) {
        final String url = String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%1$s/fabric-installer-%1$s-server.jar", installerVersion);
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Downloading: " + url);
                FileUtils.copyURLToFile(new URL(url), jar.toFile());
                return jar;
            } catch (IOException e) {
                e.printStackTrace();
                throw new InternalServerErrorResponse("Failed to download installer jar");
            }
        }, WORKER_EXECUTOR);
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
