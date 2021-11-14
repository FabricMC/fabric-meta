package net.fabricmc.meta.web.models;

/**
 * @author Jamalam360
 */
public class RecommendedVersions {
    String gameVersion;
    MavenBuildGameVersion yarn;
    MavenBuildVersion loader;
    //FAPI is yet to be added since it isn't available via this API yet. Users could use the CurseForge API meanwhile

    public RecommendedVersions(String gameVersion, MavenBuildGameVersion yarn, MavenBuildVersion loader) {
        this.gameVersion = gameVersion;
        this.yarn = yarn;
        this.loader = loader;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public MavenBuildGameVersion getYarnVersion() {
        return yarn;
    }

    public MavenBuildVersion getLoaderVersion() {
        return loader;
    }
}
