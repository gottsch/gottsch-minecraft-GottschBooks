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
package mod.gottsch.forge.gottschbooks.core.text;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LoreText}: which files are chapters, their order, how they join into one text, and each file's
 * fallback to {@code en_us}.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class LoreTextTest {
    private static final BookText NO_META = new BookText(Map.of(), List.of());

    /** entry name to locale to source, read as the book loader does: the locale's file, else en_us's */
    private static Optional<LoreText> lore(String locale, BookText meta, String... entryLocaleSource) {
        Map<String, Map<String, BookText>> entries = new HashMap<>();
        for (int i = 0; i + 2 < entryLocaleSource.length + 1; i += 3) {
            entries.computeIfAbsent(entryLocaleSource[i], k -> new HashMap<>())
                    .put(entryLocaleSource[i + 1], BookParser.parse(entryLocaleSource[i + 2]));
        }
        return LoreText.of(meta, entries.keySet(), name -> {
            Map<String, BookText> byLocale = entries.get(name);
            return Optional.ofNullable(byLocale.getOrDefault(locale, byLocale.get("en_us")));
        });
    }

    private static List<String> headings(LoreText lore) {
        return lore.text().blocks().stream().filter(Block.Heading.class::isInstance).map(Block.Heading.class::cast)
                .map(h -> h.text().getString() + "{#" + h.anchor() + "}").toList();
    }

    @Test
    void chapterNames() {
        assertTrue(LoreText.isChapter("01-arrival"));
        assertTrue(LoreText.isChapter("2-the_storm"));
        assertTrue(LoreText.isChapter("10-a-b-c"));
        assertFalse(LoreText.isChapter("text"));
        assertFalse(LoreText.isChapter("arrival"));
        assertFalse(LoreText.isChapter("01_arrival"));
        assertFalse(LoreText.isChapter("01-"));
        assertFalse(LoreText.isChapter("01"));
        assertFalse(LoreText.isChapter("chapters/01-arrival"));
        assertFalse(LoreText.isChapter("01-Arrival"));
        assertEquals(2, LoreText.number("02-the-storm"));
        assertEquals(-1, LoreText.number("text"));
    }

    @Test
    void chaptersAreInNumberOrder() {
        // by number, so 2 comes before 10 whether or not it is padded; the same number falls back to name order
        assertEquals(List.of("1-start", "02-b", "2-a", "10-end"),
                LoreText.chapters(List.of("10-end", "text", "2-a", "notes", "02-b", "1-start")));
    }

    @Test
    void textAloneIsUnchanged() {
        BookText text = BookParser.parse("---\ntitle: Diary\n---\n# Diary\nOne.\n\n---\n\nTwo.\n");
        LoreText lore = LoreText.of(NO_META, List.of("text", "stray"), name -> name.equals("text") ? Optional.of(text) : Optional.empty())
                .orElseThrow();
        assertSame(text, lore.text());
        assertEquals(List.of(), lore.chapters());
        assertTrue(LoreText.of(NO_META, List.of("stray"), name -> Optional.empty()).isEmpty());
    }

    @Test
    void chaptersJoinAfterText() {
        LoreText lore = lore("en_us", NO_META,
                "text", "en_us", "---\ntitle: The Log\nauthor: Maren\n---\nForeword.\n",
                "02-storm", "en_us", "# The Storm {#storm}\nWind.\n",
                "01-arrival", "en_us", "Before the heading.\n\n## Arriving\nSand.\n",
                "10-end", "en_us", "No heading here.\n").orElseThrow();
        assertEquals("The Log", lore.text().get("title").orElseThrow());
        assertEquals(List.of("01-arrival", "02-storm", "10-end"), lore.chapters().stream().map(LoreText.Chapter::name).toList());
        // a heading's own anchor stays; one without gets the chapter's name; a chapter without one gets one from its name
        assertEquals(List.of("Arriving{#01-arrival}", "The Storm{#storm}", "End{#10-end}"), headings(lore));
        assertEquals(List.of("Arriving", "The Storm", "End"), lore.chapters().stream().map(c -> c.title().getString()).toList());
        assertEquals(List.of("01-arrival", "storm", "10-end"), lore.chapters().stream().map(LoreText.Chapter::anchor).toList());

        // each chapter starts with a page break, in the book's style, at the block its Chapter names
        List<Block> blocks = lore.text().blocks();
        assertInstanceOf(Block.Paragraph.class, blocks.get(0));
        for (LoreText.Chapter chapter : lore.chapters()) {
            assertEquals(new Block.PageBreak("spread"), blocks.get(chapter.block()), chapter.name());
        }
        assertEquals(1, lore.chapters().get(0).block());
        assertEquals("Before the heading.", ((Block.Paragraph) blocks.get(2)).text().getString());
    }

    @Test
    void chaptersStartInTheBooksPageStyle() {
        LoreText lore = lore("en_us", BookParser.parse("---\npages: Single\n---\n"),
                "01-a", "en_us", "# A\n", "02-b", "en_us", "# B\n").orElseThrow();
        assertEquals(new Block.PageBreak("single"), lore.text().blocks().get(lore.chapters().get(1).block()));
    }

    @Test
    void frontMatterIsTheFirstChaptersWithoutText() {
        LoreText lore = lore("en_us", NO_META,
                "02-b", "en_us", "---\ntitle: Not this\n---\n# B\n",
                "01-a", "en_us", "---\ntitle: The Log\n---\n# A\n").orElseThrow();
        assertEquals("The Log", lore.text().get("title").orElseThrow());
        assertInstanceOf(Block.PageBreak.class, lore.text().blocks().get(0));
    }

    @Test
    void eachChapterFallsBackOnItsOwn() {
        String[] files = {
                "text", "en_us", "---\ntitle: The Log\n---\n",
                "text", "ko_kr", "---\ntitle: 일지\n---\n",
                "01-arrival", "en_us", "# Arrival\n",
                "01-arrival", "ko_kr", "# 도착\n",
                "02-storm", "en_us", "# The Storm\n",
                "03-calm", "en_us", "# The Calm\n",
                "03-calm", "ko_kr", "# 고요\n"};
        LoreText korean = lore("ko_kr", NO_META, files).orElseThrow();
        assertEquals("일지", korean.text().get("title").orElseThrow());
        // the chapter Korean lacks is read in English, in its place
        assertEquals(List.of("도착", "The Storm", "고요"), korean.chapters().stream().map(c -> c.title().getString()).toList());
        LoreText french = lore("fr_fr", NO_META, files).orElseThrow();
        assertEquals(List.of("Arrival", "The Storm", "The Calm"), french.chapters().stream().map(c -> c.title().getString()).toList());
    }

    @Test
    void titleFromName() {
        assertEquals("The storm", LoreText.title("02-the-storm"));
        assertEquals("Under the sea", LoreText.title("3-under_the-sea"));
    }
}
