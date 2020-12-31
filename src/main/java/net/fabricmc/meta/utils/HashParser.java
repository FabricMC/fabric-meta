package net.fabricmc.meta.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HashParser {

     String path;
     List<String[]> hashes = new ArrayList<>();

    public HashParser(String rootMavenUrl, String prefix) {
        this.path = rootMavenUrl + prefix;
    }

    public List<String[]> getHashes(String version, String prefix, String fileExtension, String[] hashExtensions) {
        this.hashes.clear();
        version = version.replaceAll("\\+", "%2B"); // Remove "+"
        version = version + prefix + version;
        for (String hashExtension : hashExtensions) {
            URL url;
            try {
                url = new URL(this.path + version + fileExtension + "." + hashExtension);

                try {
                    Scanner s = new Scanner(url.openStream());
                    String hash = s.next(); // Assumes only one line in hash file
                    this.hashes.add(new String[]{hashExtension, hash});
                } catch (IOException e) { // Hash-type doesn't exist (recent api has sha256, older ones don't)
                    this.hashes.add(new String[]{hashExtension, ""});
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return this.hashes;

    }

}
