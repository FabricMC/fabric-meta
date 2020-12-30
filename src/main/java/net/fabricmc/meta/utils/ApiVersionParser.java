package net.fabricmc.meta.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ApiVersionParser {

    private String apiVersion;
    private String majorMinecraftVersion;
    private List<String> minecraftVersions = new ArrayList<>();

    private String version;
    private int build;

    private URL versionMetaUrl;

    public ApiVersionParser(String version) {
        this.version = version;

        try {
            // TODO use official url
            versionMetaUrl = new URL("https://gist.githubusercontent.com/dexman545/478d9ca5598a48ed2960fbb5d06ef0be/raw/apiversions.properties");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Fix old API versions not having MC version
        // Recreated from curseforge version index
        if (version.contains("0.3.1") || version.contains("0.3.0")) {
            version = version + "-1.14";
        }

        if (version.contains("+build.")) {
            //TODO legacy remove when no longer needed
            this.majorMinecraftVersion = version.substring(version.lastIndexOf('-'));
            this.apiVersion = version.substring(0, version.lastIndexOf('+') - 1);
            this.build = Integer.parseInt(version.substring(version.lastIndexOf("+build.")+7, version.lastIndexOf('-')));
        } else {
            this.majorMinecraftVersion = version.substring(version.lastIndexOf('+'));
            this.apiVersion = version.substring(0, version.lastIndexOf('+') - 1);
            this.build = 999; // Build number is no longer appended to versions, use dummy version
        }

        Properties mc2Api = new Properties();
        try {
            mc2Api.load(versionMetaUrl.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String finalVersion = this.version;

        mc2Api.forEach((k, v) -> {
            String[] apiVersions = v.toString().split(",");
            Arrays.stream(apiVersions).map(String::trim).toArray(unused -> apiVersions);

            if (Arrays.asList(apiVersions).contains(finalVersion)) {
                this.minecraftVersions.add((String) k);
            }
        });
    }

    public String getVersion() {
        return this.version.substring(0, this.version.lastIndexOf(this.majorMinecraftVersion.charAt(0)) != -1 ? version.lastIndexOf(this.majorMinecraftVersion.charAt(0)) : this.version.length() - 1);
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getMajorMinecraftVersion() {
        return majorMinecraftVersion;
    }

    @Override
    public String toString() {
        return version;
    }

    public int getBuild() {
        return build;
    }

    public List<String> getMinecraftVersions() {
        //System.out.println(this.minecraftVersions);
        return minecraftVersions;
    }
}
