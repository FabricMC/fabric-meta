package net.legacyfabric.meta.web;

import io.javalin.http.Context;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.WebServer;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;

import net.legacyfabric.meta.utils.LegacyReference;
import net.legacyfabric.meta.web.models.MappingsDiff;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class YarnDiff {
	private static final Path MAPPINGS_CACHE_DIR = Paths.get("metadata", "mappings");
	private static final Path DIFF_CACHE_DIR = Paths.get("metadata", "diffs");
	private static final Executor WORKER_EXECUTOR = Executors.newSingleThreadExecutor();

	public static void setup() {
		LegacyWebServer.stringGet("/v2/diff/{source_mappings}/{target_mappings}", YarnDiff::getDiff);
	}

	private static String getDiff(Context context) {
		if (!context.pathParamMap().containsKey("source_mappings")) {
			return null;
		}

		if (!context.pathParamMap().containsKey("target_mappings")) {
			return null;
		}

		String sourceMappings = context.pathParam("source_mappings");
		String targetMappings = context.pathParam("target_mappings");

		var mappings = FabricMeta.database.mappings;
		var source = mappings.stream().filter(t -> t.getVersion().equals(sourceMappings)).findFirst().orElse(null);
		var target = mappings.stream().filter(t -> t.getVersion().equals(targetMappings)).findFirst().orElse(null);

		if (source == null || target == null) {
			return null;
		}

		if (!source.test(target.getGameVersion())) {
			return null;
		}

		return getDiffFile(source, target);
	}

	private static String getDiffFile(MavenBuildGameVersion source, MavenBuildGameVersion target) {
		var diffPath = DIFF_CACHE_DIR.resolve(source.getVersion()).resolve(target.getVersion() + ".json");

		if (!Files.exists(diffPath)) {
			try {
				generateDiffFile(source, target, diffPath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		try {
			return Files.readString(diffPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void generateDiffFile(MavenBuildGameVersion source, MavenBuildGameVersion target, Path diffPath) throws IOException {
		var sourcePath = getMappingsPath(source);
		var targetPath = getMappingsPath(target);

		MemoryMappingTree sourceTree = new MemoryMappingTree();
		MappingReader.read(getMappingsReader(sourcePath), sourceTree);

		MemoryMappingTree targetTree = new MemoryMappingTree();
		MappingReader.read(getMappingsReader(targetPath), targetTree);

		var diff = new MappingsDiff();

		populateDiff(diff, sourceTree, targetTree);

		if (!Files.exists(diffPath.getParent())) Files.createDirectories(diffPath.getParent());
		if (!Files.exists(diffPath)) Files.createFile(diffPath);
		Files.writeString(diffPath, WebServer.GSON.toJson(diff));
	}

	private static void populateDiff(MappingsDiff diff, MemoryMappingTree sourceTree, MemoryMappingTree targetTree) {
		var targetNamespace = "named";

		for (var classMapping : sourceTree.getClasses()) {
			String sourceName = classMapping.getSrcName();
			String namedSource = classMapping.getName(targetNamespace);
			var targetClass = targetTree.getClass(sourceName);

			if (targetClass == null) continue;

			if (!Objects.equals(namedSource, targetClass.getName(targetNamespace))) {
				diff.classes.add(new MappingsDiff.Entry(namedSource, targetClass.getName(targetNamespace)));
			}

			for (var fieldMapping : classMapping.getFields()) {
				String sourceName2 = fieldMapping.getSrcName();
				String sourceDesc = fieldMapping.getSrcDesc();

				var targetField = targetClass.getField(sourceName2, sourceDesc);

				if (targetField == null) continue;

				if (!Objects.equals(fieldMapping.getName(targetNamespace), targetField.getName(targetNamespace))) {
					diff.fields.add(new MappingsDiff.ClassEntry(
							namedSource,
							fieldMapping.getName(targetNamespace),
							targetField.getName(targetNamespace),
							fieldMapping.getDesc(targetNamespace),
							targetField.getDesc(targetNamespace)
					));
				}
			}

			for (var fieldMapping : classMapping.getMethods()) {
				String sourceName2 = fieldMapping.getSrcName();
				String sourceDesc = fieldMapping.getSrcDesc();

				var targetField = targetClass.getMethod(sourceName2, sourceDesc);

				if (targetField == null) continue;

				if (!Objects.equals(fieldMapping.getName(targetNamespace), targetField.getName(targetNamespace)) ||
						!Objects.equals(fieldMapping.getDesc(targetNamespace), targetField.getDesc(targetNamespace))) {
					diff.methods.add(new MappingsDiff.ClassEntry(
							namedSource,
							fieldMapping.getName(targetNamespace),
							targetField.getName(targetNamespace),
							fieldMapping.getDesc(targetNamespace),
							targetField.getDesc(targetNamespace)
					));
				}
			}
		}
	}

	private static Reader getMappingsReader(Path path) throws IOException {
		return new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile())));
	}

	private static Path getMappingsPath(MavenBuildGameVersion version) {
		var path = MAPPINGS_CACHE_DIR.resolve(version.getVersion() + ".gz");

		if (!Files.exists(path)) {
			var url = String.format(getMappingsURL(), version.getVersion());
			try {
				FileUtils.copyURLToFile(new URL(url), path.toFile());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return path;
	}



	private static String getMappingsURL() {
		return LegacyReference.LOCAL_LEGACY_FABRIC_MAVEN_URL + "net/legacyfabric/yarn/%1$s/yarn-%1$s-tiny.gz";
	}
}
