package net.fabricmc.meta.web.models;

import net.fabricmc.meta.utils.ApiVersionParser;

import java.util.List;

public class MavenBuildApiGameVersion extends MavenBuildVersion{

    private List<String[]> hashes;
    String gameMajorVersion;
    List<String> gameVersions;

    public MavenBuildApiGameVersion(String maven) {
        super(maven);

        String[] mavenP = maven.split(":");
        String version = mavenP[mavenP.length-1];
        ApiVersionParser parser = new ApiVersionParser(version);
        gameMajorVersion = parser.getMajorMinecraftVersion();

        this.hashes = parser.getHashes();

        this.build = parser.getBuild();
        this.version = parser.getVersion();
        this.gameMajorVersion = gameMajorVersion.replaceAll("[+-]", ""); // Remove prefix character
        this.gameVersions = parser.getMinecraftVersions();
    }

    public String getGameMajorVersion() {
        return gameMajorVersion;
    }

    @Override
    public boolean test(String s) {
        return this.gameVersions.contains(s);
    }

    public List<String[]> getHashes() {
        return hashes;
    }
}
