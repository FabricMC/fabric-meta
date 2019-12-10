package net.fabricmc.meta.web.models;

import net.fabricmc.meta.data.VersionDatabase;

public class MavenUrlVersion extends MavenVersion {

    public final String url;

    public MavenUrlVersion(String maven) {
        super(maven);
        String[] split = maven.split(":");
        this.url = String.format("%s%s/%s/%s/%s-%s.jar", VersionDatabase.MAVEN_URL,
                split[0].replaceAll("\\.", "/"),
                split[1],
                split[2],
                split[1],
                split[2]
        );
    }
}
