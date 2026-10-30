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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BookExport}, over the example books in {@code src/example/resources}.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class BookExportTest {
    private static final Pattern HREF = Pattern.compile("href=\"([^\"]+)\"");
    private static final Pattern MD_LINK = Pattern.compile("]\\(([^)\\s]+)\\)");
    private static final Pattern MD_HEADING = Pattern.compile("(?m)^#{1,6} (.*)$");

    /** the example books' folder: set by the build, else found from the project directory */
    private static Path examples() {
        String set = System.getProperty("gottschbooks.exampleBooks");
        return Path.of(set != null ? set : "src/example/resources/assets/example/books");
    }

    private static Map<String, String> pages(String book, BookExport.Format format) throws IOException {
        return BookExport.pages(BookExport.load(examples().resolve(book)), "en_us", format);
    }

    @Test
    void loadsTheFieldGuideAsAManual() throws IOException {
        LoadedBook book = BookExport.load(examples().resolve("field_guide"));
        assertEquals("example:field_guide", book.id().toString());
        assertTrue(book.isManual());
        assertTrue(book.entries().containsKey("categories/basics"));
        assertTrue(book.entries().containsKey("first_night"));
    }

    @Test
    void fieldGuideHasAnIndexAndAPagePerEntry() throws IOException {
        Map<String, String> pages = pages("field_guide", BookExport.Format.HTML);
        assertEquals(Set.of("index.html", "creatures.html", "curios.html", "diamonds.html", "first_night.html",
                "ores.html", "paintings.html", "tools.html", "workshop.html"), pages.keySet());
        assertEquals("index.html", pages.keySet().iterator().next());

        String index = pages.get("index.html");
        assertTrue(index.contains("<h1>A Field Guide to the Overworld</h1>"), index);
        assertTrue(index.contains("Everything a new traveller should know"), index);
        // categories in sort order, each with its description and its entries in sort order
        int basics = index.indexOf(">Basics</h2>"), nature = index.indexOf(">Nature</h2>");
        assertTrue(basics > 0 && nature > basics, index);
        assertTrue(index.indexOf("Surviving, building and digging.") > basics);
        int night = index.indexOf("<li><a href=\"first_night.html\""), tools = index.indexOf("<li><a href=\"tools.html\"");
        assertTrue(basics < night && night < tools && tools < nature, index);
        assertTrue(index.indexOf("<li><a href=\"creatures.html\"") > nature, index);
        // no loose entries in the field guide
        assertFalse(index.contains("Other entries"));
    }

    @Test
    void everyHtmlLinkLeadsToAPageAndHeading() throws IOException {
        Map<String, String> pages = pages("field_guide", BookExport.Format.HTML);
        List<String> links = new ArrayList<>();
        pages.forEach((name, html) -> {
            Matcher m = HREF.matcher(html);
            while (m.find()) {
                String href = m.group(1);
                links.add(name + " -> " + href);
                String[] target = href.split("#", 2);
                assertTrue(pages.containsKey(target[0]), name + " links to a missing page: " + href);
                if (target.length > 1) {
                    assertTrue(pages.get(target[0]).contains("id=\"" + target[1] + "\""), name + " links to a missing heading: " + href);
                }
            }
        });
        assertTrue(links.contains("index.html -> first_night.html"), links.toString());
        assertTrue(links.contains("first_night.html -> tools.html#diamond"), links.toString());
        assertTrue(links.contains("workshop.html -> tools.html#diamond"), links.toString());
        assertTrue(links.contains("ores.html -> diamonds.html"), links.toString());
        assertTrue(links.contains("tools.html -> index.html"), links.toString());
    }

    @Test
    void htmlKeepsHeadingsStylesAndTables() throws IOException {
        Map<String, String> pages = pages("field_guide", BookExport.Format.HTML);
        String night = pages.get("first_night.html");
        assertTrue(night.contains("<h1>Your First Night</h1>"), night);
        assertTrue(night.contains("<h2 id=\"torch\">The Torch</h2>"), night);
        assertTrue(night.contains("<span style=\"color:#aa0000\">zombies</span>"), night);
        assertTrue(night.contains("<span style=\"font-weight:bold\">ten minutes</span>"), night);
        assertTrue(night.contains("<span style=\"font-style:italic\">Stay inside.</span>"), night);
        assertTrue(night.contains("<ol>\n<li><span style=\"font-weight:bold\">Wood</span>"), night);
        assertTrue(night.contains("<p class=\"note\">Tip: a bed skips the night"), night);
        assertTrue(night.contains("<ul>\n<li>Make better <a href=\"tools.html\">tools</a>.</li>"), night);

        String ores = pages.get("ores.html");
        // the first table has a header and a centred column; the dye grid has neither header nor page breaks
        assertEquals(1, count(ores, "<thead>"));
        assertEquals(2, count(ores, "<table>"));
        assertTrue(ores.contains("<th style=\"text-align:center\">Most common at</th>"), ores);
        assertTrue(ores.contains("<td><code>minecraft:coal_ore</code> Coal</td>"), ores);
        assertTrue(ores.contains("<td style=\"text-align:center\">→</td>"), ores);
        assertFalse(ores.contains("{page"), ores);

        // a font run is plain text, without the white it is coloured for its pictures
        String curios = pages.get("curios.html");
        assertTrue(curios.contains("d e g r"), curios);
        assertFalse(curios.contains("color:#ffffff"), curios);
    }

    @Test
    void htmlPlaceholdersAndLocks() throws IOException {
        Map<String, String> pages = pages("field_guide", BookExport.Format.HTML);
        String tools = pages.get("tools.html");
        assertTrue(tools.contains("<div class=\"label\">Recipe: <code>minecraft:crafting_table</code></div>"), tools);
        assertTrue(tools.contains("<div class=\"label\">Item: <code>minecraft:diamond_pickaxe</code></div>"), tools);
        assertTrue(tools.contains("Four planks of any wood."), tools);
        assertTrue(pages.get("creatures.html").contains("Mob: <code>minecraft:fox</code>"));
        assertTrue(pages.get("paintings.html").contains("Image: <code>minecraft:textures/painting/sunset.png 32 16</code>"));
        String curios = pages.get("curios.html");
        assertTrue(curios.contains("Picture: <code>minecraft:potion{Potion:&quot;minecraft:healing&quot;}</code>"), curios);
        assertTrue(curios.contains("<figcaption>A Potion of Healing</figcaption>"), curios);

        String diamonds = pages.get("diamonds.html");
        assertTrue(diamonds.contains("class=\"locked\""), diamonds);
        assertTrue(diamonds.contains("<code>minecraft:story/mine_diamond</code>"), diamonds);
        assertTrue(pages.get("index.html").contains("Finding Diamonds</a> <span class=\"tag\">(locked)</span>"));
        assertFalse(pages.get("tools.html").contains("class=\"locked\""));
    }

    @Test
    void journalIsOnePage() throws IOException {
        Map<String, String> pages = pages("journal", BookExport.Format.HTML);
        assertEquals(Set.of("index.html"), pages.keySet());
        String page = pages.get("index.html");
        assertTrue(page.contains("<h1>The Lighthouse Keeper&#39;s Journal</h1>"), page);
        assertTrue(page.contains("<p class=\"author\">by Old Maren</p>"), page);
        assertEquals(2, count(page, "<hr class=\"ornament\">"));
        assertTrue(page.contains("Picture: <code>minecraft:spyglass</code>"), page);
        assertTrue(page.contains("<th style=\"text-align:center\">High water</th>"), page);
        assertTrue(page.contains("Item: <code>minecraft:potion{Potion:&quot;minecraft:water_breathing&quot;}</code>"), page);
        assertFalse(page.contains("<nav><a"), page);
    }

    @Test
    void journalChapters() throws IOException {
        String page = pages("journal", BookExport.Format.HTML).get("index.html");
        // the chapter list, then text.md, then the chapters in order, each heading taking the chapter's name as its id
        assertTrue(page.contains("<nav>\n<ol class=\"contents\">\n<li><a href=\"#01-the-storms\">The Storms</a></li>\n"
                + "<li><a href=\"#02-the-visitor\">The Visitor</a></li>\n<li><a href=\"#03-the-calm\">The Calm</a></li>\n</ol>\n</nav>\n"
                + "<p class=\"note\">Found in the lamp room"), page);
        int storms = page.indexOf("<h1 id=\"01-the-storms\">The Storms</h1>");
        int visitor = page.indexOf("<h1 id=\"02-the-visitor\">The Visitor</h1>");
        int calm = page.indexOf("<h1 id=\"03-the-calm\">The Calm</h1>");
        assertTrue(storms > 0 && storms < visitor && visitor < calm, page);
        // a link to a chapter leads down the page
        assertTrue(page.contains("<a href=\"#02-the-visitor\">to the door</a>"), page);
    }

    @Test
    void koreanJournal() throws IOException {
        LoadedBook book = BookExport.load(examples().resolve("journal"));
        String page = BookExport.pages(book, "ko_kr", BookExport.Format.HTML).get("index.html");
        assertTrue(page.contains("<html lang=\"ko-kr\">"), page);
        assertTrue(page.contains("<h1>등대지기의 일지</h1>"), page);
        assertTrue(page.contains("<p class=\"author\">by 늙은 마렌</p>"), page);
        assertTrue(page.contains("<li><a href=\"#01-the-storms\">폭풍</a></li>"), page);
        assertTrue(page.contains("<h1 id=\"03-the-calm\">고요</h1>"), page);
        assertFalse(page.contains("The Storms"), page);
    }

    @Test
    void markdownPagesAndLinks() throws IOException {
        Map<String, String> pages = pages("field_guide", BookExport.Format.MARKDOWN);
        assertEquals("Home.md", pages.keySet().iterator().next());
        assertTrue(pages.containsKey("first_night.md"));

        String home = pages.get("Home.md");
        assertTrue(home.startsWith("# A Field Guide to the Overworld\n"), home);
        assertTrue(home.contains("Start with [Your First Night](first_night), or pick a chapter below."), home);
        assertTrue(home.contains("## Basics\n\nSurviving, building and digging.\n\n- [Your First Night](first_night)\n"), home);
        assertTrue(home.contains("- [Finding Diamonds](diamonds) *(locked)*"), home);

        // a link to {#anchor} uses the id GitHub gives that heading
        String night = pages.get("first_night.md");
        assertTrue(night.contains("[diamonds](tools#the-diamond-pickaxe)"), night);
        assertTrue(night.contains("## The Torch\n"), night);
        assertTrue(night.contains("1. **Wood**, from any tree. Punch the trunk.\n2. **A shelter.**"), night);
        assertTrue(night.contains("> Tip: a bed skips the night"), night);
        assertTrue(night.contains("> **Item:** `minecraft:torch`\n>\n> Light stops monsters spawning nearby. Place them **everywhere**."), night);
        // colours are left out (GitHub drops inline styles), the words stay
        assertTrue(night.contains("When night falls, zombies and skeletons come out. *Stay inside.*"), night);

        String ores = pages.get("ores.md");
        assertTrue(ores.contains("| Ore | Found | Most common at | Mine with |\n|---|---|:---:|---|\n"), ores);
        assertTrue(ores.contains("| `minecraft:coal_ore` Coal | almost everywhere above y 0 | y 96 |"), ores);
        assertTrue(ores.contains("|   |   |   |\n|---|:---:|---|\n"), ores);
        assertTrue(pages.get("diamonds.md").contains("> **Locked:**"));

        pages.forEach((name, md) -> {
            Matcher m = MD_LINK.matcher(md);
            while (m.find()) {
                String[] target = m.group(1).split("#", 2);
                assertTrue(pages.containsKey(target[0] + ".md"), name + " links to a missing page: " + m.group(1));
                if (target.length > 1) {
                    assertTrue(slugs(pages.get(target[0] + ".md")).contains(target[1]), name + " links to a missing heading: " + m.group(1));
                }
            }
        });
    }

    @Test
    void markdownJournal() throws IOException {
        Map<String, String> pages = pages("journal", BookExport.Format.MARKDOWN);
        assertEquals(Set.of("Home.md"), pages.keySet());
        String page = pages.get("Home.md");
        assertTrue(page.startsWith("# The Lighthouse Keeper's Journal\n\n*by Old Maren*\n\n## Contents\n\n"
                + "1. [The Storms](#the-storms)\n2. [The Visitor](#the-visitor)\n3. [The Calm](#the-calm)\n\n"
                + "> Found in the lamp room"), page);
        assertTrue(page.contains("# The Storms\n\n*First night of the autumn storms.*"), page);
        // a link to a chapter uses the id GitHub gives its heading
        assertTrue(page.contains("[to the door](#the-visitor)"), page);
        assertTrue(page.contains("| Day | High water | Low water |\n|---|:---:|:---:|\n"), page);
        assertTrue(page.contains("> **Picture:** `minecraft:prismarine_shard`\n>\n> A gift from the deep"), page);
    }

    @Test
    void exportWritesTheFiles(@TempDir Path out) throws IOException {
        List<Path> written = BookExport.export(examples().resolve("field_guide"), "en_us", out.resolve("site"), BookExport.Format.HTML);
        assertEquals(9, written.size());
        assertTrue(Files.readString(out.resolve("site/index.html")).startsWith("<!DOCTYPE html>"));
        assertTrue(Files.isRegularFile(out.resolve("site/workshop.html")));
    }

    @Test
    void slugsMatchGitHub() {
        MarkdownPages.Slugs slugs = new MarkdownPages.Slugs();
        assertEquals("the-diamond-pickaxe", slugs.slug("The Diamond Pickaxe"));
        assertEquals("potions-and-curios", slugs.slug("Potions and Curios"));
        assertEquals("whats-new-v13", slugs.slug("What's New: v1.3"));
        assertEquals("the-diamond-pickaxe-1", slugs.slug("The Diamond Pickaxe"));
    }

    @Test
    void entryPagesInFoldersSitTogether() {
        assertEquals("machines--smelter", WebPages.page("machines/smelter"));
        assertEquals(BookExport.Format.MARKDOWN, BookExport.Format.of("Markdown"));
        assertThrows(IllegalArgumentException.class, () -> BookExport.Format.of("pdf"));
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + part.length())) {
            n++;
        }
        return n;
    }

    /** the ids GitHub gives a Markdown page's headings */
    private static Set<String> slugs(String markdown) {
        MarkdownPages.Slugs slugs = new MarkdownPages.Slugs();
        Set<String> ids = new java.util.HashSet<>();
        Matcher m = MD_HEADING.matcher(markdown);
        while (m.find()) {
            ids.add(slugs.slug(m.group(1).replace("\\", "").replace("*", "")));
        }
        return ids;
    }
}
