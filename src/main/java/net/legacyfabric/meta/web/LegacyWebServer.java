/*
 * Copyright (c) 2021-2024 Legacy Fabric
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

package net.legacyfabric.meta.web;

import io.javalin.core.util.Header;
import io.javalin.http.Context;
import net.fabricmc.meta.web.WebServer;

import java.util.function.Function;
import java.util.function.Supplier;

public class LegacyWebServer extends WebServer {
    public static void stringGet(String route, Supplier<String> supplier) {
        javalin.get(route, ctx -> {
            String object = supplier.get();
            handleString(ctx, object);
        });
    }

    public static void stringGet(String route, Function<Context, String> supplier) {
        javalin.get(route, ctx -> {
            String object = supplier.apply(ctx);
            handleString(ctx, object);
        });
    }

    public static void handleString(Context ctx, String string) {
        if (string == null) {
            string = "";
            ctx.status(400);
        }

        ctx.contentType("application/json").header(Header.CACHE_CONTROL, "public, max-age=60").result(string);
    }
}
