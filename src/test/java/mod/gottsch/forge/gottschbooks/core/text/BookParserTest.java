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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BookParser} and {@link Inline}.
 *
 * @author Mark Gottschling on 10/17/2026
 */
class BookParserTest {

    /** each styled run of {@code c}, as "text|b|i|colour" */
    private static List<String> runs(Component c) {
        List<String> out = new ArrayList<>();
        c.visit((style, text) -> {
            out.add(text + "|" + (style.isBold() ? "b" : "") + "|" + (style.isItalic() ? "i" : "") + "|"
                    + (style.getColor() == null ? "" : style.getColor().serialize()));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    @Test
    void frontMatterAndBlocks() {
        BookText book = BookParser.parse("""
                ---
                title: The Tidewalker's Log
                author: "Selene  # not a comment"
                sort: 13          # a comment
                ---
                ![](item:magictreasures:white_pearl)
                First line
                joined here.

                Second paragraph.
                ---
                > [An editor's
                > note.]
                ![The ring](preset:magictreasures:aqua_ring)
                <!-- page -->
                ## Part two
                """);
        assertEquals("The Tidewalker's Log", book.front().get("title"));
        assertEquals("Selene  # not a comment", book.front().get("author"));
        assertEquals("13", book.front().get("sort"));

        List<Block> b = book.blocks();
        assertEquals(8, b.size(), b.toString());
        assertEquals(new Block.Illustration("item", rl("magictreasures:white_pearl"), ""), b.get(0));
        assertEquals("First line joined here.", ((Block.Paragraph) b.get(1)).text().getString());
        assertEquals("Second paragraph.", ((Block.Paragraph) b.get(2)).text().getString());
        assertInstanceOf(Block.Ornament.class, b.get(3));
        Block.Paragraph note = (Block.Paragraph) b.get(4);
        assertTrue(note.quote());
        assertEquals("[An editor's note.]", note.text().getString());
        assertEquals(new Block.Illustration("preset", rl("magictreasures:aqua_ring"), "The ring"), b.get(5));
        assertInstanceOf(Block.PageBreak.class, b.get(6));
        assertEquals(2, ((Block.Heading) b.get(7)).level());
        assertEquals("Part two", ((Block.Heading) b.get(7)).text().getString());
    }

    @Test
    void noFrontMatter() {
        BookText book = BookParser.parse("Just text.\n");
        assertTrue(book.front().isEmpty());
        assertEquals(1, book.blocks().size());
    }

    @Test
    void hardBreaksAndListItems() {
        BookText book = BookParser.parse("one  \ntwo\\\nthree\n- a\n- b\n");
        assertEquals("one\ntwo\nthree\n- a\n- b", book.blocks().get(0) instanceof Block.Paragraph p ? p.text().getString() : "");
    }

    @Test
    void unknownSyntaxIsText() {
        BookText book = BookParser.parse("| a | b |\n<div>x</div>\n![bad](nope)\n");
        assertEquals(1, book.blocks().size());
        assertEquals("| a | b | <div>x</div> ![bad](nope)", ((Block.Paragraph) book.blocks().get(0)).text().getString());
    }

    @Test
    void emphasis() {
        assertEquals(List.of("a |||", "bold|b||", " and |||", "it|b|i|", " end|||"),
                runs(Inline.parse("a **bold** and ***it*** end")));
        assertEquals(List.of("plain |||", "it||i|"), runs(Inline.parse("plain *it*")));
    }

    @Test
    void unclosedMarkersArePlain() {
        assertEquals("2 * 3 = 6, **wow", Inline.parse("2 * 3 = 6, **wow").getString());
        assertEquals(List.of("2 * 3 = 6, **wow|||"), runs(Inline.parse("2 * 3 = 6, **wow")));
        assertEquals("{gold} never closed", Inline.parse("{gold} never closed").getString());
        assertEquals("{nope}x{}", Inline.parse("{nope}x{}").getString());
    }

    @Test
    void escapes() {
        assertEquals(List.of("*not italic*|||"), runs(Inline.parse("\\*not italic\\*")));
    }

    @Test
    void colours() {
        String gold = TextColor.fromLegacyFormat(ChatFormatting.GOLD).serialize();
        assertEquals(List.of("a |||", "gold|||" + gold, " b|||"), runs(Inline.parse("a {gold}gold{} b")));
        assertEquals(List.of("hex|||#D4A017"), runs(Inline.parse("{#d4a017}hex{}")));
        // a second colour replaces the first; one {} ends it
        assertEquals(List.of("x|||" + gold, "y|||#00FF00", "z|||"), runs(Inline.parse("{gold}x{#00ff00}y{}z")));
    }

    private static net.minecraft.resources.ResourceLocation rl(String id) {
        return new net.minecraft.resources.ResourceLocation(id);
    }
}
