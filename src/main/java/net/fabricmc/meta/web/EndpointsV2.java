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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.javalin.http.Context;
import io.javalin.http.Header;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.data.VersionDatabase.GameVersionData;
import net.fabricmc.meta.web.models.BaseVersion;
import net.fabricmc.meta.web.models.LoaderInfoV2;
import net.fabricmc.meta.web.models.MavenBuildGameVersion;
import net.fabricmc.meta.web.models.MavenBuildVersion;

@SuppressWarnings("Duplicates")
public class EndpointsV2 {
	public static void setup() {
		WebServer.jsonGet("/v2/versions", () -> FabricMeta.database.createLegacyDbDump());

		WebServer.jsonGet("/v2/versions/game", () -> FabricMeta.database.getGameModels());
		WebServer.jsonGet("/v2/versions/game/yarn", () -> toBaseVersion(FabricMeta.database.getYarnModels(), MavenBuildGameVersion::getGameVersion, v -> new BaseVersion(v.getGameVersion(), v.isStable())));
		WebServer.jsonGet("/v2/versions/game/intermediary", () -> toBaseVersion(FabricMeta.database.getIntermediaryModels(), BaseVersion::getVersion, v -> new BaseVersion(v.getVersion(), v.isStable())));

		WebServer.jsonGet("/v2/versions/yarn", context -> withLimitSkip(context, FabricMeta.database.getYarnModels()));
		WebServer.jsonGet("/v2/versions/yarn/{game_version}", context -> withLimitSkip(context, ContextUtil.getYarn(context)));

		WebServer.jsonGet("/v2/versions/intermediary", () -> FabricMeta.database.getIntermediaryModels());
		WebServer.jsonGet("/v2/versions/intermediary/{game_version}", EndpointsV2::getIntermediaryInfo);

		WebServer.jsonGet("/v2/versions/loader", context -> withLimitSkip(context, FabricMeta.database.getLoader()));
		WebServer.jsonGet("/v2/versions/loader/{game_version}", context -> withLimitSkip(context, EndpointsV2.getLoaderInfoAll(context)));
		WebServer.jsonGet("/v2/versions/loader/{game_version}/{loader_version}", EndpointsV2::getLoaderInfo);

		WebServer.jsonGet("/v2/versions/installer", context -> withLimitSkip(context, FabricMeta.database.installer));

		ProfileHandler.setup();
		ServerBootstrap.setup();
	}

	private static <T> List<T> withLimitSkip(Context context, List<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}

		int limit = context.queryParamAsClass("limit", Integer.class).check(i -> i >= 0, "limit must be larger than one").getOrDefault(0);
		int skip = context.queryParamAsClass("skip", Integer.class).check(i -> i >= 0, "skip must be larger than one").getOrDefault(0);

		Stream<T> listStream = list.stream().skip(skip);

		if (limit > 0) {
			listStream = listStream.limit(limit);
		}

		return listStream.collect(Collectors.toList());
	}

	private static List<?> getIntermediaryInfo(Context context) {
		GameVersionData data = ContextUtil.getGame(context);
		if (data == null) return Collections.emptyList();

		return ContextUtil.toList(data.intermediary());
	}

	private static Object getLoaderInfo(Context context) {
		MavenBuildVersion loader = ContextUtil.getLoader(context);

		if (loader == null) {
			context.status(400);
			return "no loader version found for " + ContextUtil.getLoaderRaw(context);
		}

		GameVersionData game = ContextUtil.getGame(context);

		if (game == null) {
			context.status(400);
			return "no mappings version found for " + ContextUtil.getGameRaw(context); // wording due to legacy compat
		}

		return new LoaderInfoV2(loader, game.intermediary()).populateMeta();
	}

	private static List<?> getLoaderInfoAll(Context context) {
		GameVersionData game = ContextUtil.getGame(context);

		if (game == null) {
			context.status(400);
			return List.of();
		}

		List<LoaderInfoV2> infoList = new ArrayList<>();

		for (MavenBuildVersion loader : FabricMeta.database.getLoader()) {
			infoList.add(new LoaderInfoV2(loader, game.intermediary()).populateMeta());
		}

		return infoList;
	}

	private static <T extends BaseVersion> Collection<BaseVersion> toBaseVersion(List<T> list, Function<T, String> gameVersionSupplier, Function<T, BaseVersion> baseVersionSupplier) {
		Map<String, BaseVersion> ret = new LinkedHashMap<>(list.size());

		for (T entry : list) {
			String version = gameVersionSupplier.apply(entry);

			if (!ret.containsKey(version)) {
				ret.put(version, new BaseVersion(version, entry.isStable()));
			}
		}

		return ret.values();
	}

	public static void fileDownload(String side, String ext,
			Function<ProfileEnvironment, String> fileNameFunction,
			Function<ProfileEnvironment, CompletableFuture<InputStream>> streamSupplier) {
		String path = switch (side) {
		case "client" -> "profile";
		case "server" -> "server";
		default -> throw new IllegalArgumentException(side);
		};

		WebServer.javalin.get("/v2/versions/loader/{game_version}/{loader_version}/" + path + "/" + ext, ctx -> {
			MavenBuildVersion loader = ContextUtil.getLoader(ctx);

			if (loader == null) {
				finishWithError(ctx, "no loader version found for " + ContextUtil.getLoaderRaw(ctx));
				return;
			}

			GameVersionData game = ContextUtil.getGame(ctx);

			if (game == null) {
				finishWithError(ctx, "no mappings version found for " + ContextUtil.getGameRaw(ctx)); // wording due to legacy compat
				return;
			}

			ProfileEnvironment subCtx = new ProfileEnvironment(game, loader, side, ext);

			if (ext.equals("zip")) {
				//Set the filename to download
				ctx.header(Header.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", fileNameFunction.apply(subCtx)));

				ctx.contentType("application/zip");
			} else {
				ctx.contentType("application/json");
			}

			//Cache for a day
			ctx.header(Header.CACHE_CONTROL, "public, max-age=86400");

			ctx.future(() -> streamSupplier.apply(subCtx).thenApply(ctx::result));
		});
	}

	private static void finishWithError(Context context, String msg) {
		context.status(400);
		context.result(msg);
	}

	record ProfileEnvironment(GameVersionData game, MavenBuildVersion loader, String side, String ext) { }
}
