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
package mod.gottsch.neo.gottschbooks.core.lint;

import mod.gottsch.neo.gottschbooks.core.text.BookParser;
import mod.gottsch.neo.gottschbooks.core.text.BookText;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A book read from files on disk (or from memory) rather than from the game's resources, for {@link BookLint}: the
 * same layout the client's book loader reads, {@code assets/<ns>/books/<book>/book.md} and
 * {@code assets/<ns>/books/<book>/<locale>/<entry>.md}, the first folder that looks like a locale ending the book's id.
 *
 * @param id      the book ({@code mymod:guide})
 * @param meta    {@code book.md}; empty when the book has none
 * @param entries entry name to locale to text
 *
 * @author Mark Gottschling on 10/28/2026
 */
public record LintBook(String id, BookText meta, Map<String, Map<String, BookText>> entries) {
    private static final String ASSETS = "assets/", BOOKS = "books";
    private static final String BOOK_FILE = "book.md";
    private static final Pattern LOCALE = Pattern.compile("[a-z]{2,3}_[a-z0-9]{2,3}");
    private static final BookText NO_META = new BookText(Map.of(), List.of());
    public static final String FALLBACK_LOCALE = "en_us";

    /**
     * Every book file under {@code root}'s {@code assets/<ns>/books/}, by its path from the root
     * ({@code assets/mymod/books/guide/en_us/basics.md}) with {@code /} separators, to its text.
     */
    public static Map<String, String> files(Path root) throws IOException {
        Map<String, String> files = new TreeMap<>();
        Path assets = root.resolve("assets");
        if (!Files.isDirectory(assets)) {
            return files;
        }
        try (Stream<Path> namespaces = Files.list(assets)) {
            for (Path books : namespaces.map(ns -> ns.resolve(BOOKS)).filter(Files::isDirectory).toList()) {
                try (Stream<Path> walk = Files.walk(books)) {
                    for (Path file : walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".md")).toList()) {
                        String path = root.relativize(file).toString().replace('\\', '/');
                        files.put(path, Files.readString(file, StandardCharsets.UTF_8));
                    }
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
            }
        }
        return files;
    }

    /**
     * The books in {@code files} (path from a resource root to text, as {@link #files} gives), parsed. A file the game
     * wouldn't load (outside the layout, or with a name that isn't a resource location) goes to {@code problems}
     * instead, as does a book.md with no text beside it.
     */
    public static Map<String, LintBook> read(Map<String, String> files, List<BookLint.Problem> problems) {
        Map<String, BookText> metas = new TreeMap<>();
        Map<String, Map<String, Map<String, BookText>>> entries = new TreeMap<>();
        files.forEach((file, source) -> {
            if (!file.startsWith(ASSETS) || !file.endsWith(".md")) {
                return;
            }
            // assets/<ns>/books/<book...>/...
            List<String> parts = List.of(file.substring(ASSETS.length()).split("/"));
            if (parts.size() < 4 || !parts.get(1).equals(BOOKS)) {
                return;
            }
            String namespace = parts.get(0);
            String path = String.join("/", parts.subList(2, parts.size()));
            if (!ResourceLocation.isValidNamespace(namespace) || !ResourceLocation.isValidPath(path)) {
                problems.add(new BookLint.Problem(file, "file name isn't lowercase a-z, 0-9, _, -, . and /, so the game never loads it"));
                return;
            }
            List<String> book = parts.subList(2, parts.size());
            int locale = -1;
            for (int i = 1; i < book.size() - 1 && locale < 0; i++) {
                if (LOCALE.matcher(book.get(i)).matches()) {
                    locale = i;
                }
            }
            if (locale < 0 && book.size() >= 2 && book.get(book.size() - 1).equals(BOOK_FILE)) {
                metas.put(namespace + ":" + String.join("/", book.subList(0, book.size() - 1)), BookParser.parse(source));
            } else if (locale > 0) {
                String entry = String.join("/", book.subList(locale + 1, book.size()));
                entries.computeIfAbsent(namespace + ":" + String.join("/", book.subList(0, locale)), k -> new TreeMap<>())
                        .computeIfAbsent(entry.substring(0, entry.length() - 3), k -> new TreeMap<>())
                        .put(book.get(locale), BookParser.parse(source));
            } else {
                problems.add(new BookLint.Problem(file, "ignored: expected books/<book>/book.md or books/<book>/<locale>/<entry>.md"));
            }
        });

        Map<String, LintBook> books = new TreeMap<>();
        entries.forEach((id, byEntry) -> books.put(id, new LintBook(id, metas.getOrDefault(id, NO_META), byEntry)));
        metas.keySet().stream().filter(id -> !entries.containsKey(id)).forEach(id ->
                problems.add(new BookLint.Problem(folder(id) + "/" + BOOK_FILE, "book has a book.md but no text, so the game leaves it out")));
        return books;
    }

    /** {@code assets/<ns>/books/<book>}, the folder the book's files are in */
    public String folder() {
        return folder(id);
    }

    private static String folder(String id) {
        int colon = id.indexOf(':');
        return ASSETS + id.substring(0, colon) + "/" + BOOKS + "/" + id.substring(colon + 1);
    }

    /** {@code entry} in {@code locale}, else in {@code en_us}, else in whatever locale has it */
    public Optional<BookText> entry(String entry, String locale) {
        Map<String, BookText> byLocale = entries.get(entry);
        if (byLocale == null || byLocale.isEmpty()) {
            return Optional.empty();
        }
        BookText text = byLocale.get(locale);
        if (text == null) {
            text = byLocale.get(FALLBACK_LOCALE);
        }
        return Optional.ofNullable(text != null ? text : byLocale.values().iterator().next());
    }

    /** a manual ({@code mode: manual} in book.md), rather than a lore book */
    public boolean isManual() {
        return meta.get("mode").map(mode -> mode.equalsIgnoreCase("manual")).orElse(false);
    }
}
