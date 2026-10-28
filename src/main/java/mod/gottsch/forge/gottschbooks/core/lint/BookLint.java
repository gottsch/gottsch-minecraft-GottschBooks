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
package mod.gottsch.forge.gottschbooks.core.lint;

import mod.gottsch.forge.gottschbooks.core.text.Block;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import mod.gottsch.forge.gottschbooks.core.text.Inline;
import mod.gottsch.forge.gottschbooks.core.text.ItemReference;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The part of {@code /gottschbooks check} that needs no game: what the parser alone can see wrong in a book's files,
 * for a mod's build or CI. It finds unknown {@code pages}, {@code lookup} and {@code lookup_from} values,
 * {@code lookup_from: items} without {@code items:}, {@code entry:} links to entries or anchors that aren't there and
 * entry links in lore books, categories with no {@code categories/<id>.md}, fenced blocks the reader doesn't know,
 * table problems, a manual with no {@code _index.md} or a lore book with no {@code text.md}, and item references
 * that don't parse. Whether an item, recipe, image, mob or font exists needs the game, so those are left to the
 * in-game check. Messages read as the in-game check's do.
 * <p>
 * {@link #main} checks every book under one or more resource roots (folders holding {@code assets/}), prints each
 * problem as {@code file: message}, as the in-game check logs them, and exits with 1 when there are any.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class BookLint {
    /** the fenced blocks the reader draws; any other kind shows its lines as plain text */
    private static final Set<String> FENCES = Set.of("spotlight", "recipe", "image", "entity");
    /** an icon left in a table cell's text, after the text began */
    private static final Pattern LATE_ICON = Pattern.compile("!\\[[^\\]]*]\\([a-z0-9_]+:[a-z0-9_.-]+:");
    private static final String INDEX = "_index", CATEGORIES = "categories/", LORE_ENTRY = "text";
    private static final String PAGES = "pages", ONE_PAGE = "single", TWO_PAGES = "spread";
    private static final String LOOKUP = "lookup", ITEMS = "items", LOOKUP_FROM = "lookup_from";
    private static final List<String> LOOKUPS = List.of("carried", "always", "never");
    private static final String FROM_ITEMS = "items";
    private static final List<String> LOOKUP_FROMS = List.of("all", FROM_ITEMS, "none");

    /** one thing wrong, and the file it is in ({@code assets/<ns>/books/<book>/<locale>/<entry>.md}) */
    public record Problem(String file, String message) {}

    private final LintBook book;
    private final List<Problem> problems = new ArrayList<>();
    private String file;

    private BookLint(LintBook book) {
        this.book = book;
    }

    /** checks every book under each of the resource roots given, printing what is wrong; exits with 1 if anything is */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: BookLint <resource root>... (a folder holding assets/, such as src/main/resources)");
            System.exit(2);
        }
        try {
            System.exit(run(List.of(args).stream().map(Path::of).toList(), System.out) ? 0 : 1);
        } catch (IOException e) {
            System.err.println("couldn't read the books: " + e);
            System.exit(2);
        }
    }

    /**
     * Checks every book under {@code roots}, a later root's files replacing an earlier one's at the same path (as a
     * resource pack does), and prints each problem, then a summary, to {@code out}. True when nothing is wrong.
     */
    public static boolean run(List<Path> roots, PrintStream out) throws IOException {
        Map<String, String> files = new TreeMap<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                throw new IOException("no folder " + root.toAbsolutePath());
            }
            files.putAll(LintBook.files(root));
        }
        List<Problem> problems = new ArrayList<>();
        Map<String, LintBook> books = LintBook.read(files, problems);
        books.values().forEach(book -> problems.addAll(check(book)));
        problems.sort(Comparator.comparing(Problem::file));
        problems.forEach(problem -> out.println(problem.file() + ": " + problem.message()));
        out.println("Books: " + String.join(", ", books.keySet()));
        out.println(books.size() + " book(s), " + problems.size() + " problem(s)");
        return problems.isEmpty();
    }

    /** what is wrong in {@code book}, file by file */
    public static List<Problem> check(LintBook book) {
        BookLint lint = new BookLint(book);
        lint.run();
        return lint.problems;
    }

    private void run() {
        file = book.folder() + "/book.md";
        book.meta().get("skin").filter(skin -> ResourceLocation.tryParse(skin) == null)
                .ifPresent(skin -> problem("skin '" + skin + "' isn't a resource location"));
        pages(book.meta());
        book.meta().get(LOOKUP).map(String::strip)
                .filter(value -> !LOOKUPS.contains(value.toLowerCase(Locale.ROOT)))
                .ifPresent(value -> problem("lookup '" + value + "' isn't carried, always or never, so the manual answers item lookups only when carried"));
        lookupFrom(book.meta());
        if (book.isManual()) {
            if (!book.entries().containsKey(INDEX)) {
                problem("manual has no " + INDEX + ".md, so its title is its id and it has no landing text");
            }
        } else if (!book.entries().containsKey(LORE_ENTRY)) {
            problem("lore book has no " + LORE_ENTRY + ".md; files: " + book.entries().keySet());
        }

        Set<String> categories = book.entries().keySet().stream().filter(name -> name.startsWith(CATEGORIES))
                .map(name -> name.substring(CATEGORIES.length())).collect(Collectors.toSet());
        book.entries().forEach((entry, byLocale) -> byLocale.forEach((locale, text) -> {
            file = book.folder() + "/" + locale + "/" + entry + ".md";
            entry(entry, locale, text, categories);
        }));
    }

    /** {@code pages:} is {@code single} or {@code spread} */
    private void pages(BookText text) {
        text.get(PAGES).filter(style -> !style.strip().equalsIgnoreCase(ONE_PAGE)
                && !style.strip().equalsIgnoreCase(TWO_PAGES)).ifPresent(style ->
                problem("pages '" + style + "' isn't single or spread, so the book shows two pages"));
    }

    private void entry(String entry, String locale, BookText text, Set<String> categories) {
        text.get("icon").flatMap(BookLint::reference).ifPresent(why -> problem("icon: " + why));
        pages(text);
        if (book.isManual() && !entry.equals(INDEX) && !entry.startsWith(CATEGORIES)) {
            text.get("category").filter(category -> !categories.contains(category)).ifPresent(category ->
                    problem("category '" + category + "' has no categories/" + category + ".md, so the entry is listed after the categories"));
            text.get("advancement").filter(id -> ResourceLocation.tryParse(id) == null).ifPresent(id ->
                    problem("advancement '" + id + "' isn't a resource location, so the entry is never hidden"));
            text.get(ITEMS).ifPresent(this::items);
            lookupFrom(text);
            if (text.get(LOOKUP_FROM).map(value -> value.strip().equalsIgnoreCase(FROM_ITEMS)).orElse(false)
                    && text.get(ITEMS).isEmpty()) {
                problem("lookup_from: items, but no items:, so the entry answers no item lookups (lookup_from: none says so plainly)");
            }
        }
        blocks(text.blocks(), locale);
    }

    /** {@code lookup_from:} is {@code all}, {@code items} or {@code none} */
    private void lookupFrom(BookText text) {
        text.get(LOOKUP_FROM).map(String::strip)
                .filter(value -> !LOOKUP_FROMS.contains(value.toLowerCase(Locale.ROOT)))
                .ifPresent(value -> problem("lookup_from '" + value + "' isn't all, items or none, so it is taken as all"));
    }

    /** each of {@code items:}'s references parses, and each {@code #tag} is a resource location */
    private void items(String items) {
        for (String token : ItemReference.split(items)) {
            if (token.startsWith("#")) {
                if (ResourceLocation.tryParse(token.substring(1)) == null) {
                    problem("items: '" + token + "' isn't an item tag (#ns:path)");
                }
            } else {
                reference(token).ifPresent(why -> problem("items: " + why));
            }
        }
    }

    /** why {@code reference} doesn't parse as an item or a picture; empty when it does */
    private static Optional<String> reference(String reference) {
        return ItemReference.parse(reference).isPresent() ? Optional.empty()
                : Optional.of("'" + reference + "' isn't an item or a picture (ns:id or scheme:ns:id)");
    }

    private void blocks(List<Block> blocks, String locale) {
        for (Block block : blocks) {
            if (block instanceof Block.Paragraph p) {
                text(p.text(), locale);
            } else if (block instanceof Block.Heading h) {
                text(h.text(), locale);
            } else if (block instanceof Block.ListItem item) {
                text(item.text(), locale);
            } else if (block instanceof Block.Fence f) {
                fence(f);
                blocks(f.body(), locale);
            } else if (block instanceof Block.Table t) {
                table(t, locale);
            } else if (block instanceof Block.PageBreak b) {
                if (!b.style().isEmpty() && !b.style().equals(ONE_PAGE) && !b.style().equals(TWO_PAGES)) {
                    problem("{page:" + b.style() + "} isn't single or spread, so it only breaks the page");
                }
            }
            // an illustration's reference always parses (the parser only reads one that does), and an ornament is
            // just an ornament
        }
    }

    /** a fence's kind is one the reader knows, and its argument names something */
    private void fence(Block.Fence fence) {
        String argument = fence.argument().strip();
        switch (fence.kind()) {
            case "spotlight" -> reference(argument).ifPresent(why -> problem("spotlight: " + why));
            case "recipe" -> {
                if (ResourceLocation.tryParse(argument) == null || argument.isEmpty()) {
                    problem("recipe '" + argument + "' isn't a resource location");
                }
            }
            case "image" -> {
                String texture = argument.split("\\s+")[0];
                if (ResourceLocation.tryParse(texture) == null || texture.isEmpty()) {
                    problem("image '" + texture + "' isn't a resource location (the full path, such as mymod:textures/gui/map.png)");
                }
            }
            case "entity" -> {
                if (ResourceLocation.tryParse(argument) == null || argument.isEmpty()) {
                    problem("mob '" + argument + "' isn't a resource location");
                }
            }
            default -> {
                if (!fence.kind().isEmpty() && !FENCES.contains(fence.kind())) {
                    problem("unknown block '```" + fence.kind() + "', shown as plain lines; the reader knows " + FENCES);
                }
            }
        }
    }

    /** a table's rows, cells and text */
    private void table(Block.Table table, String locale) {
        table.problems().forEach(why -> problem("table: " + why));
        List<List<Block.Table.Cell>> rows = new ArrayList<>(table.rows());
        rows.add(table.header());
        if (rows.stream().flatMap(List::stream).allMatch(Block.Table.Cell::isEmpty)) {
            problem("table has nothing in it");
        }
        for (List<Block.Table.Cell> row : rows) {
            for (Block.Table.Cell cell : row) {
                if (LATE_ICON.matcher(cell.text().getString()).find()) {
                    problem("table cell '" + cell.text().getString() + "': only icons at the start of a cell are drawn; this one shows as text");
                }
                text(cell.text(), locale);
            }
        }
    }

    /** a run of text's {@code entry:} links */
    private void text(Component text, String locale) {
        text.visit((style, run) -> {
            link(style, locale);
            return Optional.empty();
        }, Style.EMPTY);
    }

    private void link(Style style, String locale) {
        ClickEvent click = style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.CHANGE_PAGE || !click.getValue().startsWith(Inline.ENTRY_LINK)) {
            return;
        }
        String target = click.getValue().substring(Inline.ENTRY_LINK.length());
        if (!book.isManual()) {
            problem("link to '" + target + "': links between entries only work in manuals");
            return;
        }
        String[] parts = target.split("#", 2);
        String entry = parts[0];
        if (entry.equals(INDEX) || entry.startsWith(CATEGORIES) || !book.entries().containsKey(entry)) {
            problem("link to '" + target + "': no entry '" + entry + "'");
            return;
        }
        if (parts.length > 1) {
            String anchor = parts[1];
            boolean found = book.entry(entry, locale).map(text -> anchors(text.blocks()).contains(anchor)).orElse(false);
            if (!found) {
                problem("link to '" + target + "': entry '" + entry + "' has no heading {#" + anchor + "}");
            }
        }
    }

    private static Set<String> anchors(List<Block> blocks) {
        Set<String> anchors = new HashSet<>();
        for (Block block : blocks) {
            if (block instanceof Block.Heading h && !h.anchor().isEmpty()) {
                anchors.add(h.anchor());
            } else if (block instanceof Block.Fence f) {
                anchors.addAll(anchors(f.body()));
            }
        }
        return anchors;
    }

    private void problem(String message) {
        problems.add(new Problem(file, message));
    }
}
