package net.fabricmc.meta.utils;

import net.fabricmc.meta.web.models.BaseVersion;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Parser {

    public String path;

    public String latestVersion = "";
    public List<String> versions = new ArrayList<>();

    public Parser(String path) {
        this.path = path;
    }

    public abstract void load() throws IOException, XMLStreamException;

    public <T extends BaseVersion> List<T> getMeta(Function<String, T> function, String prefix) throws IOException, XMLStreamException {
        try {
            load();
        } catch (IOException e){
            throw new IOException("Failed to load " + path, e);
        }

        List<T> list = versions.stream()
                .map((version) -> prefix + version)
                .map(function)
                .collect(Collectors.toList());

        Path unstableVersionsPath = Paths.get(prefix
                .replace(":", "_")
                .replace(".", "_")
                .replaceFirst(".$","")
                + ".txt");

        if (Files.exists(unstableVersionsPath)) {
            // Read a file containing a new line separated list of versions that should not be marked as stable.
            List<String> unstableVersions = Files.readAllLines(unstableVersionsPath);
            list.stream()
                    .filter(v -> !unstableVersions.contains(v.getVersion()))
                    .findFirst()
                    .ifPresent(v -> v.setStable(true));
        } else {
            if(list.get(0) != null){
                list.get(0).setStable(true);
            }
        }

        return Collections.unmodifiableList(list);
    }
}
