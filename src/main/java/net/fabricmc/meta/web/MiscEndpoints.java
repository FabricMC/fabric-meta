package net.fabricmc.meta.web;

public class MiscEndpoints {
	public static void setup() {
		WebServer.get("/misc/data/mcupdater", "text/xml", McUpdater::handleLegacy);
		WebServer.get("/misc/data/mcupdater/{game_version}/{loader_version}", "text/xml", McUpdater::handle);
	}
}
