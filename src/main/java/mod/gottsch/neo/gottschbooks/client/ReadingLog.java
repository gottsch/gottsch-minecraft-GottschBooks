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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Which manual entries the player has opened, for the reader's "new" marks and the item lookup's fading hint, and the
 * player's bookmarks. Kept on the client, in {@code gottschbooks/reading.json} under the game folder, for every world
 * alike: what a player has read and marked is theirs, not the world's. Saved whenever it changes.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class ReadingLog {
    private static final String FILE = "gottschbooks/reading.json";
    private static final String OPENED = "opened", BOOKMARKS = "bookmarks";
    /** the most bookmarks a book keeps: as many ribbons as fit down the book's edge */
    public static final int MAX_BOOKMARKS = 10;

    /**
     * A bookmark: an entry ({@code ""} in a lore book), and the block (paragraph, heading, picture...) the marked page
     * starts with, so it finds its page however the window lays the text out; {@code text} is the start of that page,
     * to show on the ribbon's tooltip.
     */
    public record Bookmark(String entry, int block, String text) {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** each book's opened entries, and its bookmarks; loaded on first use */
    @Nullable
    private static Map<ResourceLocation, Set<String>> opened;
    private static final Map<ResourceLocation, List<Bookmark>> bookmarks = new HashMap<>();

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

    /** the book's bookmarks, oldest first */
    public static List<Bookmark> bookmarks(ResourceLocation book) {
        opened();
        return List.copyOf(bookmarks.getOrDefault(book, List.of()));
    }

    /** adds a bookmark, unless the book already has {@link #MAX_BOOKMARKS} */
    public static boolean addBookmark(ResourceLocation book, Bookmark bookmark) {
        opened();
        List<Bookmark> marks = bookmarks.computeIfAbsent(book, k -> new ArrayList<>());
        if (marks.size() >= MAX_BOOKMARKS) {
            return false;
        }
        marks.add(bookmark);
        save();
        return true;
    }

    public static void removeBookmark(ResourceLocation book, Bookmark bookmark) {
        opened();
        if (bookmarks.getOrDefault(book, new ArrayList<>()).remove(bookmark)) {
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
            JsonObject marked = root.has(BOOKMARKS) ? root.getAsJsonObject(BOOKMARKS) : new JsonObject();
            for (Map.Entry<String, JsonElement> book : marked.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(book.getKey());
                if (id != null && book.getValue().isJsonArray()) {
                    List<Bookmark> marks = new ArrayList<>();
                    for (JsonElement element : book.getValue().getAsJsonArray()) {
                        JsonObject mark = element.getAsJsonObject();
                        marks.add(new Bookmark(mark.get("entry").getAsString(), mark.get("block").getAsInt(),
                                mark.has("text") ? mark.get("text").getAsString() : ""));
                    }
                    bookmarks.put(id, marks);
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
        JsonObject marked = new JsonObject();
        new TreeMap<>(Map.copyOf(bookmarks)).forEach((book, marks) -> {
            if (!marks.isEmpty()) {
                JsonArray list = new JsonArray();
                marks.forEach(mark -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("entry", mark.entry());
                    object.addProperty("block", mark.block());
                    object.addProperty("text", mark.text());
                    list.add(object);
                });
                marked.add(book.toString(), list);
            }
        });
        JsonObject root = new JsonObject();
        root.add(OPENED, books);
        root.add(BOOKMARKS, marked);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            GottschBooks.LOGGER.warn("couldn't write {}", file, e);
        }
    }
}
