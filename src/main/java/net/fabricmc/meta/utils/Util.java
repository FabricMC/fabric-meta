/*
 * Copyright (c) 2026 FabricMC
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

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.w3c.dom.Document;

public final class Util {
	public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final int MAX_SB_CACHE_SIZE = Runtime.getRuntime().availableProcessors();
	private static final Deque<StringBuilder> SB_CACHE = new ArrayDeque<>(MAX_SB_CACHE_SIZE);

	public static String serializeJson(Object object) {
		if (object == null) return "null";
		if (object instanceof String s) return s;

		// cache string builder to reduce allocation/resizing pressure
		StringBuilder sb;

		synchronized (SB_CACHE) {
			sb = SB_CACHE.pollLast();

			if (sb == null) {
				sb = new StringBuilder(10_000);
			} else {
				sb.setLength(0);
			}
		}

		try {
			GSON.toJson(object, sb);

			return sb.toString();
		} finally {
			synchronized (SB_CACHE) {
				if (SB_CACHE.size() < MAX_SB_CACHE_SIZE) {
					SB_CACHE.addLast(sb);
				}
			}
		}
	}

	public static Document createXmlDocument() {
		try {
			return DocumentBuilderFactory.newInstance()
					.newDocumentBuilder()
					.newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public static String serializeXml(Document doc) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));

			return writer.toString();
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}
}
