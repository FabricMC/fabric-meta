/*
 * Copyright (c) 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.meta.utils;

import net.fabricmc.meta.web.models.BaseVersion;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PomParser {

	public String path;

	public String latestVersion = "";
	public List<String> versions = new ArrayList<>();

	public PomParser(String path) {
		this.path = path;
	}

	private void load() throws IOException, XMLStreamException {
		versions.clear();

		URL url = new URL(path);
		XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(url.openStream());
		while (reader.hasNext()) {
			if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("version")) {
				String text = reader.getElementText();
				versions.add(text);
			}
		}
		reader.close();
		Collections.reverse(versions);
		latestVersion = versions.get(0);
	}

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

		if(list.get(0) != null){
			list.get(0).setStable(true);
		}

		return Collections.unmodifiableList(list);
	}

}
