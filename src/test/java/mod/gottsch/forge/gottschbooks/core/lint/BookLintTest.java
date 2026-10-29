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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BookLint} and {@link LintBook}, over books in memory, in a temp folder, and the example pack's
 * {@code example:mistakes}, which is wrong on purpose.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class BookLintTest {
    private static final String GUIDE = "assets/mymod/books/guide/";
    private static final String MANUAL = manual("");
    private static final String INDEX = "---\ntitle: The Guide\n---\nWelcome.\n";

    /** a manual's book.md, with {@code front} (whole lines) in its front matter */
    private static String manual(String front) {
        return "---\nmode: manual\n" + front + "---\n";
    }

    /** every problem in the books {@code pathsAndSources} make, file problems included, as "file: message" */
    private static List<String> lint(String... pathsAndSources) {
        Map<String, String> files = new TreeMap<>();
        for (int i = 0; i + 1 < pathsAndSources.length; i += 2) {
            files.put(pathsAndSources[i], pathsAndSources[i + 1]);
        }
        List<BookLint.Problem> problems = new ArrayList<>();
        LintBook.read(files, problems).values().forEach(book -> problems.addAll(BookLint.check(book)));
        return problems.stream().map(p -> p.file() + ": " + p.message()).toList();
    }

    private static void assertHas(List<String> problems, String file, String message) {
        assertTrue(problems.stream().anyMatch(p -> p.startsWith(file + ": ") && p.contains(message)),
                "no '" + message + "' in " + file + ": " + problems);
    }

    @Test
    void cleanManual() {
        assertEquals(List.of(), lint(
                GUIDE + "book.md", manual("lookup: always\nlookup_from: items\npages: single\n"),
                GUIDE + "en_us/_index.md", INDEX,
                GUIDE + "en_us/categories/basics.md", "---\ntitle: Basics\n---\n",
                GUIDE + "en_us/start.md", """
                        ---
                        title: Start
                        category: basics
                        icon: minecraft:torch
                        items: minecraft:torch, #minecraft:logs minecraft:potion{Potion:"minecraft:healing"}
                        ---
                        # Start {#top}
                        See [the widget](entry:machines/widget#use) and [the top](entry:start#top).

                        ```spotlight item:minecraft:torch
                        A torch.
                        ```

                        ```recipe minecraft:torch
                        ```

                        {page:spread}

                        | Item | Use |
                        |---|:---:|
                        | ![](item:minecraft:torch) Torch | light |
                        """,
                GUIDE + "en_us/machines/widget.md", "---\ntitle: Widget\n---\n## Using it {#use}\nPress it.\n",
                GUIDE + "de_de/machines/widget.md", "---\ntitle: Widget\n---\n## Benutzen {#use}\n"));
    }

    @Test
    void frontMatter() {
        List<String> problems = lint(
                GUIDE + "book.md", manual("pages: wide\nlookup: sometimes\nlookup_from: icons\n"),
                GUIDE + "en_us/_index.md", INDEX,
                GUIDE + "en_us/a.md", "---\npages: tall\nlookup_from: everything\n---\n",
                GUIDE + "en_us/b.md", "---\nlookup_from: Items\n---\n",
                GUIDE + "en_us/c.md", "---\nlookup_from: items\nitems: minecraft:stick\n---\n");
        assertHas(problems, GUIDE + "book.md", "pages 'wide' isn't single or spread");
        assertHas(problems, GUIDE + "book.md", "lookup 'sometimes' isn't carried, always or never");
        assertHas(problems, GUIDE + "book.md", "lookup_from 'icons' isn't all, items or none");
        assertHas(problems, GUIDE + "en_us/a.md", "pages 'tall'");
        assertHas(problems, GUIDE + "en_us/a.md", "lookup_from 'everything'");
        assertHas(problems, GUIDE + "en_us/b.md", "lookup_from: items, but no items:");
        assertEquals(6, problems.size(), problems.toString());
    }

    @Test
    void links() {
        List<String> problems = lint(
                GUIDE + "book.md", MANUAL,
                GUIDE + "en_us/_index.md", INDEX,
                GUIDE + "en_us/a.md", "# A {#top}\n[gone](entry:nowhere), [no anchor](entry:a#nope), [contents](entry:_index),"
                        + " [a category](entry:categories/x), [fine](entry:a#top)\n",
                GUIDE + "fr_fr/a.md", "# A\n[fine in English only](entry:a#top)\n");
        assertHas(problems, GUIDE + "en_us/a.md", "link to 'nowhere': no entry 'nowhere'");
        assertHas(problems, GUIDE + "en_us/a.md", "link to 'a#nope': entry 'a' has no heading {#nope}");
        assertHas(problems, GUIDE + "en_us/a.md", "link to '_index': no entry '_index'");
        assertHas(problems, GUIDE + "en_us/a.md", "link to 'categories/x': no entry 'categories/x'");
        // the French entry has no {#top}, and a link reads the entry in its own language
        assertHas(problems, GUIDE + "fr_fr/a.md", "link to 'a#top': entry 'a' has no heading {#top}");
        assertEquals(5, problems.size(), problems.toString());

        String lore = "assets/mymod/books/diary/";
        assertEquals(List.of(lore + "en_us/text.md: link to 'guide': no chapter 'guide' (a lore book links to its chapters,"
                        + " 01-name, and to headings in them or in text.md)"),
                lint(lore + "en_us/text.md", "Read [on](entry:text), [the guide](entry:guide).\n"));
    }

    @Test
    void cleanChapters() {
        String lore = "assets/mymod/books/diary/";
        assertEquals(List.of(), lint(
                lore + "en_us/01-arrival.md", "---\ntitle: The Log\n---\n# Arrival\nSee [the storm](entry:02-the-storm#wreck).\n",
                lore + "en_us/02-the-storm.md", "# The Storm\n## The wreck {#wreck}\nBack to [the start](entry:01-arrival).\n",
                lore + "ko_kr/01-arrival.md", "---\ntitle: 일지\n---\n# 도착\n",
                // with text.md, the title is text.md's
                "assets/mymod/books/log/en_us/text.md", "# The Log {#top}\n",
                "assets/mymod/books/log/en_us/1-a.md", "# A\n[top](entry:text#top)\n"));
    }

    @Test
    void chapterProblems() {
        String lore = "assets/mymod/books/diary/";
        List<String> problems = lint(
                lore + "en_us/text.md", "---\ntitle: The Log\n---\n",
                lore + "en_us/01-arrival.md", "# Arrival\n[gone](entry:03-nowhere), [no heading](entry:02-storm#nope)\n",
                lore + "en_us/1-again.md", "# Again\n",
                lore + "en_us/02-storm.md", "Wind, and no heading.\n",
                lore + "en_us/arrival.md", "# Not a chapter\n",
                lore + "en_us/chapters/03-calm.md", "# In a folder\n",
                lore + "de_de/02-sturm.md", "# Der Sturm\n");
        assertHas(problems, lore + "en_us/01-arrival.md", "link to '03-nowhere': no chapter '03-nowhere'");
        assertHas(problems, lore + "en_us/01-arrival.md", "link to '02-storm#nope': entry '02-storm' has no heading {#nope}");
        assertHas(problems, lore + "en_us/1-again.md", "chapter 1 again (01-arrival.md has that number too)");
        assertHas(problems, lore + "en_us/02-storm.md", "chapter has no heading, so its title is made from its file name");
        assertHas(problems, lore + "en_us/arrival.md", "isn't text.md or a chapter");
        assertHas(problems, lore + "en_us/chapters/03-calm.md", "isn't text.md or a chapter");
        assertHas(problems, lore + "de_de/02-sturm.md", "no en_us/02-sturm.md: chapters are matched across languages by file name");
        assertEquals(7, problems.size(), problems.toString());
    }

    @Test
    void chaptersNeedATitleWithoutText() {
        String lore = "assets/mymod/books/diary/";
        assertEquals(List.of(lore + "en_us/01-a.md: no title: (with no text.md, the book's front matter is its first chapter's),"
                        + " so the title page shows the book's id"),
                lint(lore + "en_us/01-a.md", "# A\n", lore + "en_us/02-b.md", "---\ntitle: Too late\n---\n# B\n"));
    }

    @Test
    void categories() {
        List<String> problems = lint(
                GUIDE + "book.md", MANUAL,
                GUIDE + "en_us/_index.md", INDEX,
                GUIDE + "en_us/categories/basics.md", "",
                GUIDE + "en_us/a.md", "---\ncategory: basics\n---\n",
                GUIDE + "en_us/b.md", "---\ncategory: gems\n---\n");
        assertEquals(List.of(GUIDE + "en_us/b.md: category 'gems' has no categories/gems.md, so the entry is listed after the categories"),
                problems);
    }

    @Test
    void fencesAndTables() {
        List<String> problems = lint(GUIDE + "book.md", MANUAL, GUIDE + "en_us/_index.md", INDEX, GUIDE + "en_us/a.md", """
                ```chart
                not a block the reader knows
                ```

                ```
                a plain fence is fine
                ```

                ```recipe
                ```

                ```image Not/A/Texture.png 32 32
                ```

                | Ore | Depth |
                |---|---|
                | Coal | y 96 | extra |
                | Iron ![](item:minecraft:iron_ore) |

                | | |
                |---|---|
                """);
        String a = GUIDE + "en_us/a.md";
        assertHas(problems, a, "unknown block '```chart', shown as plain lines");
        assertHas(problems, a, "recipe '' isn't a resource location");
        assertHas(problems, a, "image 'Not/A/Texture.png' isn't a resource location");
        assertHas(problems, a, "table: row 1 has 3 cells but the table has 2; the extra ones are left out");
        assertHas(problems, a, "table: row 2 has 1 cells but the table has 2; the missing ones are left empty");
        assertHas(problems, a, "only icons at the start of a cell are drawn");
        assertHas(problems, a, "table has nothing in it");
        assertEquals(7, problems.size(), problems.toString());
    }

    @Test
    void pageStyle() {
        assertEquals(List.of("assets/mymod/books/diary/en_us/text.md: {page:wide} isn't single or spread, so it only breaks the page"),
                lint("assets/mymod/books/diary/en_us/text.md", "One.\n\n{page:wide}\n\nTwo.\n\n{page:single}\n"));
    }

    @Test
    void missingIndexAndText() {
        assertEquals(List.of(GUIDE + "book.md: manual has no _index.md, so its title is its id and it has no landing text"),
                lint(GUIDE + "book.md", MANUAL, GUIDE + "en_us/a.md", "Text.\n"));
        String lore = "assets/mymod/books/diary/";
        assertEquals(List.of(lore + "book.md: lore book has no text.md and no chapters (01-name.md); files: [chapter]"),
                lint(lore + "en_us/chapter.md", "Text.\n"));
    }

    @Test
    void itemReferences() {
        List<String> problems = lint(GUIDE + "book.md", MANUAL, GUIDE + "en_us/_index.md", INDEX, GUIDE + "en_us/a.md", """
                ---
                icon: Not An Item
                items: minecraft:stick, mymod:Bad, #Not:A:Tag, #minecraft:logs
                advancement: mymod:Story/Root
                ---
                ```spotlight
                ```

                ```entity Minecraft:Zombie
                ```

                ```entity minecraft:enderman 0.8
                ```

                ```entity minecraft:enderman 2
                ```
                """);
        String a = GUIDE + "en_us/a.md";
        assertHas(problems, a, "icon: 'Not An Item' isn't an item or a picture");
        assertHas(problems, a, "items: 'mymod:Bad' isn't an item or a picture");
        assertHas(problems, a, "items: '#Not:A:Tag' isn't an item tag");
        assertHas(problems, a, "advancement 'mymod:Story/Root' isn't a resource location");
        assertHas(problems, a, "spotlight: '' isn't an item or a picture");
        assertHas(problems, a, "mob 'Minecraft:Zombie' isn't a resource location");
        assertHas(problems, a, "mob 'minecraft:enderman': scale '2' isn't a number above 0 and at most 1, such as 0.7");
        assertEquals(7, problems.size(), problems.toString());
    }

    @Test
    void files() {
        List<String> problems = lint(
                GUIDE + "book.md", MANUAL,
                GUIDE + "en_us/_index.md", INDEX,
                GUIDE + "en_us/Widgets.md", "Text.\n",
                GUIDE + "notes.md", "Text.\n",
                "assets/mymod/books/empty/book.md", MANUAL,
                "assets/mymod/textures/readme.md", "not a book file");
        assertHas(problems, GUIDE + "en_us/Widgets.md", "isn't lowercase");
        assertHas(problems, GUIDE + "notes.md", "ignored: expected books/<book>/book.md");
        assertHas(problems, "assets/mymod/books/empty/book.md", "book has a book.md but no text");
        assertEquals(3, problems.size(), problems.toString());
    }

    @Test
    void runPrintsEachProblemAndASummary(@TempDir Path root) throws IOException {
        Path entry = root.resolve(GUIDE + "en_us/a.md");
        Files.createDirectories(entry.getParent());
        Files.writeString(root.resolve(GUIDE + "book.md"), manual("pages: wide\n"));
        Files.writeString(entry, "[gone](entry:nowhere)\n");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertFalse(BookLint.run(List.of(root), new PrintStream(bytes, true, StandardCharsets.UTF_8)));
        List<String> lines = bytes.toString(StandardCharsets.UTF_8).lines().toList();
        assertEquals(List.of(
                GUIDE + "book.md: pages 'wide' isn't single or spread, so the book shows two pages",
                GUIDE + "book.md: manual has no _index.md, so its title is its id and it has no landing text",
                GUIDE + "en_us/a.md: link to 'nowhere': no entry 'nowhere'",
                "Books: mymod:guide",
                "1 book(s), 3 problem(s)"), lines);

        // a later root replaces an earlier one's file, as a resource pack does
        Path fixes = root.resolve("fixes");
        Files.createDirectories(fixes.resolve(GUIDE + "en_us"));
        Files.writeString(fixes.resolve(GUIDE + "book.md"), MANUAL);
        Files.writeString(fixes.resolve(GUIDE + "en_us/_index.md"), INDEX);
        Files.writeString(fixes.resolve(GUIDE + "en_us/a.md"), "Fixed.\n");
        bytes.reset();
        assertTrue(BookLint.run(List.of(root, fixes), new PrintStream(bytes, true, StandardCharsets.UTF_8)), bytes.toString());

        assertThrows(IOException.class, () -> BookLint.run(List.of(root.resolve("nowhere")), System.out));
    }

    /** the example pack's book of mistakes: every mistake the parser can see is found */
    @Test
    void mistakesBook() throws IOException {
        Path resources = Path.of("").toAbsolutePath();
        while (resources != null && !Files.isDirectory(resources.resolve("src/example/resources"))) {
            resources = resources.getParent();
        }
        assertNotNull(resources, "no src/example/resources above " + Path.of("").toAbsolutePath());
        Map<String, String> files = new TreeMap<>(LintBook.files(resources.resolve("src/example/resources")));
        files.keySet().removeIf(file -> !file.startsWith("assets/example/books/mistakes/"));
        List<BookLint.Problem> problems = new ArrayList<>();
        LintBook.read(files, problems).values().forEach(book -> problems.addAll(BookLint.check(book)));
        List<String> found = problems.stream().map(p -> p.file() + ": " + p.message()).toList();

        String book = "assets/example/books/mistakes/book.md", errors = "assets/example/books/mistakes/en_us/errors.md";
        assertHas(found, book, "lookup 'sometimes' isn't carried, always or never");
        assertHas(found, errors, "category 'nowhere' has no categories/nowhere.md");
        assertHas(found, errors, "pages 'wide' isn't single or spread");
        assertHas(found, errors, "lookup_from 'icons' isn't all, items or none");
        assertHas(found, errors, "unknown block '```chart'");
        assertHas(found, errors, "link to 'nowhere': no entry 'nowhere'");
        assertHas(found, errors, "link to 'errors#nope': entry 'errors' has no heading {#nope}");
        assertHas(found, errors, "{page:wide} isn't single or spread");
        assertHas(found, errors, "table: row 2 has 3 cells but the table has 2");
        assertHas(found, errors, "table: row 3 has 1 cells but the table has 2");
        assertHas(found, errors, "only icons at the start of a cell are drawn");
        assertHas(found, errors, "mob 'minecraft:zombie': scale 'huge' isn't a number");
        assertEquals(12, found.size(), found.toString());
    }
}
