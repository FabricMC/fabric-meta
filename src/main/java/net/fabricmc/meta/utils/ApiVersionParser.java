package net.fabricmc.meta.utils;

import net.fabricmc.meta.data.VersionDatabase;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ApiVersionParser {

    private List<String[]> hashes;
    private String apiVersion;
    private String majorMinecraftVersion;
    private List<String> minecraftVersions = new ArrayList<>();

    private String version;
    private int build;

    private URL versionMetaUrl;

    private static final String[] hashTypes =  new String[]{"md5", "sha1", "sha256", "sha512"};
    private HashParser hashParser = new HashParser(VersionDatabase.MAVEN_URL, "net/fabricmc/fabric-api/fabric-api/");

    public ApiVersionParser(String version) {
        this.version = version;

        try {
            // TODO use official url
            versionMetaUrl = new URL("https://raw.githubusercontent.com/dexman545/fabric-api-version-map/main/apiVersions.properties");
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

        this.hashes = this.hashParser.getHashes(this.version, "/fabric-api-", ".jar", hashTypes);

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
        return minecraftVersions;
    }

    public List<String[]> getHashes() {
        return hashes;
    }
}
