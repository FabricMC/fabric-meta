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

package net.fabricmc.meta;

import net.fabricmc.meta.data.VersionDatabase;
import net.fabricmc.meta.web.WebServer;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FabricMeta {

	public static VersionDatabase database;

	public static void main(String[] args) {

		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(() -> {
			try {
				database = VersionDatabase.generate();
			} catch (IOException | XMLStreamException e) {
				if(database == null){
					throw new RuntimeException(e);
				} else {
					e.printStackTrace();
				}
			}
		}, 0, 5, TimeUnit.MINUTES);

		WebServer.start();
	}

}
