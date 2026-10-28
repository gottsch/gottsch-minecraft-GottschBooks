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
package mod.gottsch.forge.gottschbooks.export;

import mod.gottsch.forge.gottschbooks.client.LoadedBook;
import mod.gottsch.forge.gottschbooks.core.text.BookParser;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Exports a book folder ({@code assets/<ns>/books/<book>/}) as static web pages, so the same text can be published as
 * a wiki or a mod page: for a manual, an index page and a page per entry; for a lore book, one page. Runs outside the
 * game, on the book parser alone; what only the game can draw (spotlights, recipes, images, mobs, illustrations and
 * table icons) becomes a labelled placeholder showing its id.
 * <p>
 * {@code java ... mod.gottsch.forge.gottschbooks.export.BookExport <book folder> <locale> <output folder> [html|markdown]}
 * (or {@code gradlew exportBook}).
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class BookExport {
    private static final String BOOK_FILE = "book.md";
    private static final String BOOKS = "books";
    private static final Pattern LOCALE = Pattern.compile("[a-z]{2,3}_[a-z0-9]{2,3}");
    private static final BookText NO_META = new BookText(Map.of(), List.of());

    /** what the pages are written as */
    public enum Format {
        /** standalone HTML pages ({@code index.html}, {@code <entry>.html}) */
        HTML,
        /** GitHub-wiki-flavoured Markdown ({@code Home.md}, {@code <entry>.md}, linked without the extension) */
        MARKDOWN;

        public static Format of(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "html" -> HTML;
                case "markdown", "md", "wiki" -> MARKDOWN;
                default -> throw new IllegalArgumentException("unknown format '" + name + "': use html or markdown");
            };
        }
    }

    private BookExport() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 3 || args.length > 4) {
            System.err.println("usage: BookExport <book folder> <locale> <output folder> [html|markdown]");
            System.err.println("  e.g. BookExport src/main/resources/assets/mymod/books/guide en_us build/wiki markdown");
            System.exit(2);
        }
        Format format = args.length == 4 ? Format.of(args[3]) : Format.HTML;
        List<Path> written = export(Path.of(args[0]), args[1], Path.of(args[2]), format);
        System.out.println("wrote " + written.size() + " pages to " + Path.of(args[2]).toAbsolutePath());
    }

    /** writes the book's pages into {@code out} (made if missing); returns the files written */
    public static List<Path> export(Path folder, String locale, Path out, Format format) throws IOException {
        Files.createDirectories(out);
        List<Path> written = new ArrayList<>();
        for (Map.Entry<String, String> page : pages(load(folder), locale, format).entrySet()) {
            Path file = out.resolve(page.getKey());
            Files.writeString(file, page.getValue(), StandardCharsets.UTF_8);
            written.add(file);
        }
        return written;
    }

    /** the book's pages, file name to contents, index first */
    public static Map<String, String> pages(LoadedBook book, String locale, Format format) {
        WebPages pages = format == Format.HTML ? new HtmlPages(book, locale) : new MarkdownPages(book, locale);
        return pages.pages();
    }

    /**
     * Reads a book folder as the game would: {@code book.md}, and every {@code <locale>/<entry>.md} under it. The book's
     * id comes from the path ({@code assets/<ns>/books/<book>}), or is {@code book:<folder>} outside one.
     */
    public static LoadedBook load(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IOException("not a book folder: " + folder);
        }
        BookText meta = Files.isRegularFile(folder.resolve(BOOK_FILE)) ? read(folder.resolve(BOOK_FILE)) : NO_META;
        Map<String, Map<String, BookText>> entries = new HashMap<>();
        try (Stream<Path> locales = Files.list(folder)) {
            for (Path dir : locales.filter(Files::isDirectory).sorted().toList()) {
                String locale = dir.getFileName().toString();
                if (!LOCALE.matcher(locale).matches()) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(dir)) {
                    for (Path file : files.filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".md")).sorted().toList()) {
                        String path = dir.relativize(file).toString().replace('\\', '/');
                        entries.computeIfAbsent(path.substring(0, path.length() - 3), k -> new HashMap<>())
                                .put(locale, read(file));
                    }
                }
            }
        }
        if (entries.isEmpty()) {
            throw new IOException("no <locale>/<entry>.md files in " + folder);
        }
        return new LoadedBook(bookId(folder), meta, Map.copyOf(entries));
    }

    private static BookText read(Path file) throws IOException {
        return BookParser.parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    private static ResourceLocation bookId(Path folder) {
        Path absolute = folder.toAbsolutePath().normalize();
        List<String> path = new ArrayList<>();
        for (Path at = absolute; at != null && at.getFileName() != null; at = at.getParent()) {
            Path parent = at.getParent();
            path.add(0, at.getFileName().toString());
            if (parent != null && parent.getFileName() != null && parent.getFileName().toString().equals(BOOKS)
                    && parent.getParent() != null && parent.getParent().getFileName() != null) {
                ResourceLocation id = ResourceLocation.tryBuild(parent.getParent().getFileName().toString(), String.join("/", path));
                if (id != null) {
                    return id;
                }
            }
        }
        ResourceLocation id = ResourceLocation.tryBuild("book", absolute.getFileName().toString().toLowerCase(Locale.ROOT));
        return id != null ? id : new ResourceLocation("book", "book");
    }
}
