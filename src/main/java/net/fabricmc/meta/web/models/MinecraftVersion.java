package net.fabricmc.meta.web.models;

public class MinecraftVersion extends BaseVersion{
    String semver;

    public MinecraftVersion(String version, boolean stable, String semver) {
        super(version, stable);
        this.semver = semver;
    }

    public String getSemver() {
        return semver;
    }
}
