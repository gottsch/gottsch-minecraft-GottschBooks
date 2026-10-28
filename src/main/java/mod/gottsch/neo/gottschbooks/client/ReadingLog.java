/*
 * This file is part of Gottsch Books.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Gottsch Books is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gottsch Books is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Gottsch Books.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.neo.gottschbooks.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mod.gottsch.neo.gottschbooks.GottschBooks;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Which manual entries the player has opened, for the reader's "new" marks and the item lookup's fading hint. Kept on
 * the client, in {@code gottschbooks/reading.json} under the game folder, for every world alike: having read an
 * entry is the player's, not the world's. Saved whenever it changes.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class ReadingLog {
    private static final String FILE = "gottschbooks/reading.json";
    private static final String OPENED = "opened";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** each book's opened entries; loaded on first use */
    @Nullable
    private static Map<ResourceLocation, Set<String>> opened;

    private ReadingLog() {}

    /** whether the player has opened {@code entry} of {@code book} */
    public static boolean isOpened(ResourceLocation book, String entry) {
        return opened().getOrDefault(book, Set.of()).contains(entry);
    }

    /** records that the player opened {@code entry} of {@code book} */
    public static void open(ResourceLocation book, String entry) {
        if (opened().computeIfAbsent(book, k -> new TreeSet<>()).add(entry)) {
            save();
        }
    }

    /** records every one of {@code entries} as opened: "mark all read" */
    public static void openAll(ResourceLocation book, Collection<String> entries) {
        if (opened().computeIfAbsent(book, k -> new TreeSet<>()).addAll(entries)) {
            save();
        }
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FILE);
    }

    private static Map<ResourceLocation, Set<String>> opened() {
        if (opened == null) {
            opened = load(file());
        }
        return opened;
    }

    private static Map<ResourceLocation, Set<String>> load(Path file) {
        Map<ResourceLocation, Set<String>> loaded = new HashMap<>();
        if (!Files.exists(file)) {
            return loaded;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject books = root.has(OPENED) ? root.getAsJsonObject(OPENED) : new JsonObject();
            for (Map.Entry<String, JsonElement> book : books.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(book.getKey());
                if (id != null && book.getValue().isJsonArray()) {
                    Set<String> entries = new TreeSet<>();
                    book.getValue().getAsJsonArray().forEach(entry -> entries.add(entry.getAsString()));
                    loaded.put(id, entries);
                }
            }
        } catch (IOException | RuntimeException e) {
            // a broken file costs the "new" marks, never the reader
            GottschBooks.LOGGER.warn("couldn't read {}; every entry counts as new", file, e);
        }
        return loaded;
    }

    private static void save() {
        Path file = file();
        JsonObject books = new JsonObject();
        new TreeMap<>(Map.copyOf(opened)).forEach((book, entries) -> {
            JsonArray list = new JsonArray();
            entries.forEach(list::add);
            books.add(book.toString(), list);
        });
        JsonObject root = new JsonObject();
        root.add(OPENED, books);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            GottschBooks.LOGGER.warn("couldn't write {}", file, e);
        }
    }
}
