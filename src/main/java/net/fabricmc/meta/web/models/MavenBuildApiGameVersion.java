package net.fabricmc.meta.web.models;

import net.fabricmc.meta.utils.ApiVersionParser;

public class MavenBuildApiGameVersion extends MavenBuildVersion{

    String gameVersion;

    public MavenBuildApiGameVersion(String maven) {
        super(maven);

        String[] mavenP = maven.split(":");
        String version = mavenP[mavenP.length-1];
        ApiVersionParser parser = new ApiVersionParser(version);
        gameVersion = parser.getMinecraftVersion();
        this.build = parser.getBuild();
        System.out.println(version);
        this.version = version.substring(0, version.lastIndexOf(gameVersion.charAt(0)) != -1 ? version.lastIndexOf(gameVersion.charAt(0)) : version.length() - 1);
        this.gameVersion = gameVersion.replaceAll("[+-]", ""); // Remove prefix character
    }

    public String getGameVersion() {
        return gameVersion;
    }

    @Override
    public boolean test(String s) {
        return getGameVersion().equals(s);
    }
}
