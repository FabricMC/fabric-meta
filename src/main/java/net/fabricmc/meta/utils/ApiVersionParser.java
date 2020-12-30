package net.fabricmc.meta.utils;

public class ApiVersionParser {

    private String apiVersion;
    private String minecraftVersion;

    private String version;
    private int build;

    public ApiVersionParser(String version) {
        this.version = version;

        // Fix old API versions not having MC version
        // Recreated from curseforge version index
        if (version.contains("0.3.1") || version.contains("0.3.0")) {
            version = version + "-1.14";
        }

        if (version.contains("+build.")) {
            //TODO legacy remove when no longer needed
            this.minecraftVersion = version.substring(version.lastIndexOf('-'));
            this.apiVersion = version.substring(0, version.lastIndexOf('+') - 1);
            this.build = Integer.parseInt(version.substring(version.lastIndexOf("+build.")+7, version.lastIndexOf('-')));
        } else {
            this.minecraftVersion = version.substring(version.lastIndexOf('+'));
            this.apiVersion = version.substring(0, version.lastIndexOf('+') - 1);
            this.build = 999; // Build number is no longer appended to versions, use dummy version
        }
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    public String toString() {
        return version;
    }

    public int getBuild() {
        return build;
    }
}
