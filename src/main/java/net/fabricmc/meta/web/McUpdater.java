/*
 * Copyright (c) 2026 FabricMC
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.data.VersionDatabase.GameVersionData;
import net.fabricmc.meta.utils.LoaderMeta;
import net.fabricmc.meta.utils.Reference;
import net.fabricmc.meta.utils.Util;
import net.fabricmc.meta.web.models.MavenBuildVersion;

final class McUpdater {
	static String handle(Context context) {
		GameVersionData game = ContextUtil.getGame(context);
		MavenBuildVersion loader = ContextUtil.getLoader(context);

		if (game == null) throw new NotFoundResponse("unknown game version: " + ContextUtil.getGameRaw(context));
		if (loader == null) throw new NotFoundResponse("unknown loader version: " + ContextUtil.getLoaderRaw(context));

		return handle(game, loader);
	}

	static String handleLegacy(Context context) {
		String intermediary = context.queryParam("intermediary");
		String yarn = context.queryParam("yarn");
		String loaderRaw = context.queryParam("loader");

		String gameVersion;

		if (intermediary != null) {
			gameVersion = intermediary;
		} else if (yarn != null) {
			int pos = yarn.indexOf('+'); // 19w34a+build.2

			if (pos < 0) {
				pos = yarn.lastIndexOf('.'); // 19w13b.1
				if (pos < 0) throw new BadRequestResponse("invalid yarn version: " + yarn);
			}

			gameVersion = yarn.substring(0, pos);
		} else {
			throw new BadRequestResponse("can't determine game version without intermediary or yarn versions being provided");
		}

		GameVersionData game = FabricMeta.database.getGameData(gameVersion);
		if (game == null) throw new NotFoundResponse("unknown game version: " + gameVersion);

		MavenBuildVersion loader;

		if (loaderRaw == null) {
			loader = FabricMeta.database.getLatestPublicLoader();
		} else {
			loader = FabricMeta.database.getLoader(loaderRaw);
			if (loader == null) throw new NotFoundResponse("unknown loader version: " + loaderRaw);
		}

		return handle(game, loader);
	}

	private static String handle(GameVersionData game, MavenBuildVersion loaderVersion) {
		JsonObject loaderMeta = LoaderMeta.getMeta(loaderVersion);
		if (loaderMeta == null) throw new NotFoundResponse("invalid loader version: " + loaderVersion);

		Document doc = Util.createXmlDocument();
		populateDocument(game, loaderVersion, loaderMeta, doc);

		return Util.serializeXml(doc);
	}

	private static void populateDocument(GameVersionData game, MavenBuildVersion loaderVersion, JsonObject loaderJson, Document doc) {
		String versionString = String.format("%s-%s", game.version().id(), loaderVersion.getVersion());

		Element root = doc.createElement("ServerPack");
		root.setAttribute("version", "3.3");
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.setAttribute("xmlns", "http://www.mcupdater.com");
		root.setAttribute("xsi:schemaLocation", "http://www.mcupdater.com http://files.mcupdater.com/ServerPackv2.xsd");
		doc.appendChild(root);

		Element server = doc.createElement("Server");
		server.setAttribute("id", "fabric");
		server.setAttribute("name", "Fabric and dependencies");
		server.setAttribute("version", game.version().id());
		server.setAttribute("abstract", "true");
		server.setAttribute("revision", versionString);

		JsonElement mainClassElem = loaderJson.get("mainClass");
		if (mainClassElem.isJsonObject()) mainClassElem = mainClassElem.getAsJsonObject().get("client");
		server.setAttribute("mainClass", mainClassElem.getAsString());

		server.setAttribute("serverClass", "net.fabricmc.loader.launch.server.FabricServerLauncher");

		root.appendChild(server);

		Element module = appendModule(server,
				"fabric-loader-" + versionString,
				"Fabric Loader",
				Reference.FABRIC_MAVEN_URL,
				loaderVersion.getMaven());

		if (game.version().obfuscated()) {
			appendModule(module,
					"intermediary-" + game.intermediary().getVersion(),
					"Intermediary Mappings",
					Reference.FABRIC_MAVEN_URL,
					game.intermediary().getMaven());
		}

		JsonObject libs = loaderJson.get("libraries").getAsJsonObject();

		for (String env : new String[] { "common", "client" }) {
			JsonElement envLibs = libs.get(env);
			if (envLibs == null) continue;

			for (JsonElement e : envLibs.getAsJsonArray()) {
				JsonObject lib = e.getAsJsonObject();

				String name = lib.get("name").getAsString();
				String[] parts = name.split(":");

				appendModule(module,
						String.format("%s-%s", parts[1], parts[2]),
						String.format("Library: %s", parts[1]),
						lib.get("url").getAsString(),
						name);
			}
		}
	}

	private static Element appendModule(Element parent, String id, String name, String baseUrl, String mavenId) {
		String[] parts = mavenId.split(":");
		String path = String.format("%s/%s/%s/%2$s-%3$s.jar", parts[0].replace('.', '/'), parts[1], parts[2]);

		Document doc = parent.getOwnerDocument();
		Element elem = doc.createElement(parent.getTagName().equals("Module") ? "Submodule" : "Module");
		elem.setAttribute("id", id);
		elem.setAttribute("name", name);
		parent.appendChild(elem);

		Element urlElem = doc.createElement("URL");
		urlElem.setAttribute("priority", "0");
		urlElem.setTextContent(baseUrl + path);
		elem.appendChild(urlElem);

		Element modTypeElem = doc.createElement("ModType");
		modTypeElem.setTextContent("Library");
		elem.appendChild(modTypeElem);

		Element requiredElem = doc.createElement("Required");
		requiredElem.setTextContent("true");
		elem.appendChild(requiredElem);

		Element modPathElem = doc.createElement("ModPath");
		modPathElem.setTextContent("libraries/" + path);
		elem.appendChild(modPathElem);

		return elem;
	}
}
