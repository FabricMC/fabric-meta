package net.fabricmc.meta.web;

import io.javalin.core.util.Header;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.InternalServerErrorResponse;
import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.models.BaseVersion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ServerBoostrap {
    private static final Path CACHE_DIR = Paths.get("metadata", "installer");
    private static final Executor INSTALLER_DL_EXECUTOR = Executors.newSingleThreadExecutor();

    public static void setup() {
        // http://localhost:5555/v2/versions/loader/1.17.1/0.12.0/0.8.0/server/jar
        WebServer.javalin.get("/v2/versions/loader/:game_version/:loader_version/:installer_version/server/jar", boostrapHandler());
    }

    private static Handler boostrapHandler() {
        return ctx -> {
            if (!ctx.queryParamMap().isEmpty()) {
                // Cannot really afford people to cache bust this.
                throw new BadRequestResponse("Query params not supported on this endpoint.");
            }

            ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "https://fabricmc.net");

            final String installerVersion = getAndValidateVersion(ctx, FabricMeta.database.installer, "installer_version");
            final String gameVersion = getAndValidateVersion(ctx, FabricMeta.database.intermediary, "game_version");
            final String loaderVersion = getAndValidateVersion(ctx, FabricMeta.database.getAllLoader(), "loader_version");

            final CompletableFuture<InputStream> future = packageBootstrap(installerVersion, gameVersion, loaderVersion);

            // Set the filename and cache headers
            final String filename = String.format("fabric-server+mc.%s-loader.%s-installer.%s.jar", gameVersion, loaderVersion, installerVersion);
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

    private static CompletableFuture<InputStream> packageBootstrap(String installerVersion, String gameVersion, String loaderVersion) {
        return getInstallerJar(installerVersion)
                .thenApplyAsync(inputStream -> {
                    try {
                        return writePropertiesToJar(inputStream, loaderVersion, gameVersion);
                    } catch (IOException e) {
                        throw new InternalServerErrorResponse("Failed to write properties to jar");
                    }
                });
    }

    private static CompletableFuture<InputStream> getInstallerJar(String installerVersion) {
        Path installerJar = CACHE_DIR.resolve(String.format("fabric-installer-%s", installerVersion));

        if (Files.exists(installerJar)) {
            return readInstallerJar(installerJar);
        }

        return downloadInstallerJar(installerJar, installerVersion);
    }

    private static CompletableFuture<InputStream> readInstallerJar(Path jar) {
        try {
            return CompletableFuture.completedFuture(Files.newInputStream(jar));
        } catch (IOException e) {
            throw new InternalServerErrorResponse("Failed to read installer jar");
        }
    }

    private static CompletableFuture<InputStream> downloadInstallerJar(Path jar, String installerVersion) {
        final String url = String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%1$s/fabric-installer-%1$s-server.jar", installerVersion);
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Downloading: " + url);
                FileUtils.copyURLToFile(new URL(url), jar.toFile());
                return Files.newInputStream(jar);
            } catch (IOException e) {
                throw new InternalServerErrorResponse("Failed to download installer jar");
            }
        }, INSTALLER_DL_EXECUTOR);
    }

    // Is this really the best way? Can we somehow save repacking the whole zip?
    private static InputStream writePropertiesToJar(InputStream inputStream, String loaderVersion, String gameVersion) throws IOException {
        String data = String.format("fabric-loader-version=%s\ngame-version=%s", loaderVersion, gameVersion);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ZipOutputStream outputZip = new ZipOutputStream(outputStream);
             ZipInputStream inputZip = new ZipInputStream(inputStream))  {
            // Copy the existing installer jar contents
            copyZipEntries(inputZip, outputZip);

            // Add the installer properties
            outputZip.putNextEntry(new ZipEntry("install.properties"));
            IOUtils.write(data, outputZip, StandardCharsets.UTF_8);
            outputZip.closeEntry();
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private static void copyZipEntries(ZipInputStream input, ZipOutputStream output) throws IOException {
        ZipEntry currentEntry;

        while ((currentEntry = input.getNextEntry()) != null) {
            ZipEntry newEntry = new ZipEntry(currentEntry.getName());
            output.putNextEntry(newEntry);
            IOUtils.copy(input, output);
            output.closeEntry();
        }
    }
}
