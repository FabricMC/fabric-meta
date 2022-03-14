package net.fabricmc.meta.utils;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

public class PageParser extends Parser {
    public PageParser(String path) {
        super(path);
    }

    @Override
    public void load() throws IOException, XMLStreamException {
        versions.clear();

        URL url = new URL(path);

        String originalText = new BufferedReader(
                new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"))
                .replaceAll("<hr>", "");

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(originalText));
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("a")) {
                String text = reader.getElementText();
                versions.add(text.replace("/", ""));
            }
        }
        reader.close();

        versions.remove(0);

        Collections.reverse(versions);

        versions.remove(0);
        versions.remove(0);
        versions.remove(0);
        versions.remove(0);
        versions.remove(0);

        latestVersion = versions.get(0);
    }
}
