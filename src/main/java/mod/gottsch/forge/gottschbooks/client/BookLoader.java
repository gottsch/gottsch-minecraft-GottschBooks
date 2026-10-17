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
package mod.gottsch.forge.gottschbooks.client;

import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.core.text.BookParser;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads every book under {@code assets/<namespace>/books/} on resource reload, parsing each file once. Resource packs
 * can replace any file. A file that can't be read is logged and left out; it never stops the others loading.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public class BookLoader extends SimplePreparableReloadListener<Map<ResourceLocation, LoadedBook>> {
    private static final String ROOT = "books";
    private static final String BOOK_FILE = "book.md";
    private static final Pattern LOCALE = Pattern.compile("[a-z]{2,3}_[a-z0-9]{2,3}");
    private static final BookText NO_META = new BookText(Map.of(), List.of());

    private static volatile Map<ResourceLocation, LoadedBook> books = Map.of();

    public static Optional<LoadedBook> get(ResourceLocation id) {
        return Optional.ofNullable(books.get(id));
    }

    @Override
    protected Map<ResourceLocation, LoadedBook> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, BookText> metas = new HashMap<>();
        Map<ResourceLocation, Map<String, Map<String, BookText>>> entries = new HashMap<>();

        for (Map.Entry<ResourceLocation, Resource> file : manager.listResources(ROOT, rl -> rl.getPath().endsWith(".md")).entrySet()) {
            ResourceLocation location = file.getKey();
            // books/<book...>/book.md or books/<book...>/<locale>/<entry>.md
            List<String> parts = List.of(location.getPath().substring(ROOT.length() + 1).split("/"));
            try {
                if (parts.size() >= 2 && parts.get(parts.size() - 1).equals(BOOK_FILE)) {
                    metas.put(bookId(location, parts.subList(0, parts.size() - 1)), read(file.getValue()));
                } else if (parts.size() >= 3 && LOCALE.matcher(parts.get(parts.size() - 2)).matches()) {
                    String locale = parts.get(parts.size() - 2);
                    String fileName = parts.get(parts.size() - 1);
                    String entry = fileName.substring(0, fileName.length() - 3);
                    entries.computeIfAbsent(bookId(location, parts.subList(0, parts.size() - 2)), k -> new HashMap<>())
                            .computeIfAbsent(entry, k -> new HashMap<>())
                            .put(locale, read(file.getValue()));
                } else {
                    GottschBooks.LOGGER.warn("ignoring book file {}: expected books/<book>/book.md or books/<book>/<locale>/<entry>.md", location);
                }
            } catch (IOException | RuntimeException e) {
                GottschBooks.LOGGER.error("couldn't read book file {}", location, e);
            }
        }

        Map<ResourceLocation, LoadedBook> loaded = new HashMap<>();
        entries.forEach((id, byEntry) -> loaded.put(id, new LoadedBook(id, metas.getOrDefault(id, NO_META), Map.copyOf(byEntry))));
        metas.keySet().stream().filter(id -> !entries.containsKey(id))
                .forEach(id -> GottschBooks.LOGGER.warn("book {} has a book.md but no text", id));
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, LoadedBook> prepared, ResourceManager manager, ProfilerFiller profiler) {
        books = Map.copyOf(prepared);
        GottschBooks.LOGGER.debug("loaded {} books", books.size());
    }

    private static ResourceLocation bookId(ResourceLocation file, List<String> path) {
        return new ResourceLocation(file.getNamespace(), String.join("/", path));
    }

    private static BookText read(Resource resource) throws IOException {
        try (BufferedReader reader = resource.openAsReader()) {
            return BookParser.parse(reader.lines().collect(Collectors.joining("\n")));
        }
    }
}
