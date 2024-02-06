package net.legacyfabric.meta.web.models;

public class MetaServerInfo {
    public static final MetaServerInfo INSTANCE = new MetaServerInfo();

    private static final String VERSION = "@VERSION@";

    String version;

    private MetaServerInfo() {
        this.version = VERSION;
    }
}
