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
package mod.gottsch.neo.gottschbooks.core.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
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
        assertEquals(new Block.Illustration(item("item", "magictreasures:white_pearl", ""), ""), b.get(0));
        assertEquals("First line joined here.", ((Block.Paragraph) b.get(1)).text().getString());
        assertEquals("Second paragraph.", ((Block.Paragraph) b.get(2)).text().getString());
        assertInstanceOf(Block.Ornament.class, b.get(3));
        Block.Paragraph note = (Block.Paragraph) b.get(4);
        assertTrue(note.quote());
        assertEquals("[An editor's note.]", note.text().getString());
        assertEquals(new Block.Illustration(item("preset", "magictreasures:aqua_ring", ""), "The ring"), b.get(5));
        assertEquals(new Block.PageBreak(""), b.get(6));
        assertEquals(2, ((Block.Heading) b.get(7)).level());
        assertEquals("Part two", ((Block.Heading) b.get(7)).text().getString());
    }

    @Test
    void illustrationComponents() {
        List<Block> b = BookParser.parse("""
                ![Healing](item:minecraft:potion[potion_contents={potion:"minecraft:healing"}])
                ![](item:minecraft:player_head[custom_name='"Mark (the author) ]"'])
                ![A ring](preset:magictreasures:aqua_ring[dyed_color=1])
                ![no close](item:minecraft:potion[x=1)
                """).blocks();
        assertEquals(4, b.size(), b.toString());
        assertEquals(new Block.Illustration(item("item", "minecraft:potion",
                "[potion_contents={potion:\"minecraft:healing\"}]"), "Healing"), b.get(0));
        assertEquals(new Block.Illustration(item("item", "minecraft:player_head",
                "[custom_name='\"Mark (the author) ]\"']"), ""), b.get(1));
        assertEquals(new Block.Illustration(item("preset", "magictreasures:aqua_ring", "[dyed_color=1]"), "A ring"),
                b.get(2));
        // components without their closing bracket aren't an illustration
        assertInstanceOf(Block.Paragraph.class, b.get(3));
    }

    @Test
    void iconWithComponents() {
        BookText book = BookParser.parse("""
                ---
                icon: minecraft:leather_chestplate[custom_name='{"text":"Red","color":"#FF0000"}'] # red
                ---
                """);
        assertEquals("minecraft:leather_chestplate[custom_name='{\"text\":\"Red\",\"color\":\"#FF0000\"}']",
                book.front().get("icon"));
    }

    @Test
    void noFrontMatter() {
        BookText book = BookParser.parse("Just text.\n");
        assertTrue(book.front().isEmpty());
        assertEquals(1, book.blocks().size());
    }

    @Test
    void hardBreaks() {
        BookText book = BookParser.parse("one  \ntwo\\\nthree\n");
        assertEquals("one\ntwo\nthree", ((Block.Paragraph) book.blocks().get(0)).text().getString());
    }

    @Test
    void lists() {
        BookText book = BookParser.parse("Intro\n- a\n- b\n  continued\n\n1. one\n2) two\n\n\\- not a list\n");
        List<Block> b = book.blocks();
        assertEquals(6, b.size(), b.toString());
        assertEquals("Intro", ((Block.Paragraph) b.get(0)).text().getString());
        assertEquals(0, ((Block.ListItem) b.get(1)).number());
        assertEquals("a", ((Block.ListItem) b.get(1)).text().getString());
        assertEquals("b continued", ((Block.ListItem) b.get(2)).text().getString());
        assertEquals(1, ((Block.ListItem) b.get(3)).number());
        assertEquals(2, ((Block.ListItem) b.get(4)).number());
        assertEquals("two", ((Block.ListItem) b.get(4)).text().getString());
        assertEquals("- not a list", ((Block.Paragraph) b.get(5)).text().getString());
    }

    @Test
    void listItemWithBreak() {
        BookText book = BookParser.parse("- **Harm +** {dark_gray}level 2{}\\\n  3 hp to one mob.\n- **Next**\n");
        List<Block> b = book.blocks();
        assertEquals(2, b.size(), b.toString());
        assertEquals("Harm + level 2\n3 hp to one mob.", ((Block.ListItem) b.get(0)).text().getString());
    }

    @Test
    void fences() {
        BookText book = BookParser.parse("""
                ## Topaz {#topaz}
                ```spotlight item:magictreasures:topaz
                The **most** common gem.

                - Tier: 1
                ```
                ```recipe minecraft:stick
                ```
                after
                """);
        List<Block> b = book.blocks();
        assertEquals(4, b.size(), b.toString());
        Block.Heading h = (Block.Heading) b.get(0);
        assertEquals("Topaz", h.text().getString());
        assertEquals("topaz", h.anchor());
        Block.Fence spot = (Block.Fence) b.get(1);
        assertEquals("spotlight", spot.kind());
        assertEquals("item:magictreasures:topaz", spot.argument());
        assertEquals(2, spot.body().size());
        assertEquals("The most common gem.", ((Block.Paragraph) spot.body().get(0)).text().getString());
        assertInstanceOf(Block.ListItem.class, spot.body().get(1));
        Block.Fence recipe = (Block.Fence) b.get(2);
        assertEquals("recipe", recipe.kind());
        assertTrue(recipe.body().isEmpty());
        assertEquals("after", ((Block.Paragraph) b.get(3)).text().getString());
    }

    @Test
    void unclosedFenceRunsToTheEnd() {
        BookText book = BookParser.parse("```image ns:textures/a.png\ncaption\n");
        assertEquals(1, book.blocks().size());
        assertEquals(1, ((Block.Fence) book.blocks().get(0)).body().size());
    }

    @Test
    void headingWithoutAnchor() {
        assertEquals("", ((Block.Heading) BookParser.parse("# Title\n").blocks().get(0)).anchor());
    }

    @Test
    void links() {
        Component c = Inline.parse("See [the **gems**](entry:gems#ruby), [site](https://example.com/a) and [not a link] or [x](ftp://no).");
        assertEquals("See the gems, site and [not a link] or [x](ftp://no).", c.getString());
        List<ClickEvent> clicks = new ArrayList<>();
        List<String> bolds = new ArrayList<>();
        c.visit((style, text) -> {
            if (style.getClickEvent() != null) {
                clicks.add(style.getClickEvent());
                assertTrue(style.isUnderlined(), text);
            }
            if (style.isBold()) {
                bolds.add(text);
            }
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, "entry:gems#ruby"), clicks.get(0));
        assertEquals(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com/a"), clicks.get(clicks.size() - 1));
        assertEquals(List.of("gems"), bolds);
    }

    @Test
    void loreBracketsStayText() {
        assertEquals("[An editor's note.]", Inline.parse("[An editor's note.]").getString());
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

    /** each styled run of {@code c}, as "text|font|colour" */
    private static List<String> fonts(Component c) {
        List<String> out = new ArrayList<>();
        c.visit((style, text) -> {
            out.add(text + "|" + style.getFont() + "|" + (style.getColor() == null ? "" : style.getColor().serialize()));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    @Test
    void fontRuns() {
        assertEquals(List.of("a |minecraft:default|", "abc|mypack:icons|", " b|minecraft:default|"),
                fonts(Inline.parse("a {font:mypack:icons}abc{} b")));
        // a colour and a font together; one {} ends both
        assertEquals(List.of("abc|mypack:icons|white", "z|minecraft:default|"),
                fonts(Inline.parse("{white}{font:mypack:icons}abc{}z")));
        assertEquals(List.of("x|minecraft:alt|"), fonts(Inline.parse("{font:alt}x{}")));
        // never closed, or not a resource location: plain text
        assertEquals("{font:mypack:icons}abc", Inline.parse("{font:mypack:icons}abc").getString());
        assertEquals("{font:Bad Name}x{}", Inline.parse("{font:Bad Name}x{}").getString());
    }

    @Test
    void pageBreaks() {
        List<Block> b = BookParser.parse("""
                one
                {page}
                two
                {page:single}
                three
                { page : Spread }
                {page:wide}
                <!-- page -->
                a {page} in a line is text
                \\{page}
                """).blocks();
        assertEquals(new Block.PageBreak(""), b.get(1));
        assertEquals(new Block.PageBreak("single"), b.get(3));
        // "{ page" isn't the marker, so that line is text
        assertInstanceOf(Block.Paragraph.class, b.get(4));
        assertEquals(new Block.PageBreak("wide"), b.get(5), "an unknown style is kept for the check to report");
        assertEquals(new Block.PageBreak(""), b.get(6));
        assertEquals("a {page} in a line is text {page}", ((Block.Paragraph) b.get(7)).text().getString());
        assertEquals(8, b.size(), b.toString());
    }

    private static Block.Table table(String source) {
        List<Block> blocks = BookParser.parse(source).blocks();
        assertEquals(1, blocks.size(), blocks.toString());
        return assertInstanceOf(Block.Table.class, blocks.getFirst());
    }

    private static List<String> texts(List<Block.Table.Cell> row) {
        return row.stream().map(cell -> cell.text().getString()).toList();
    }

    @Test
    void tableBasics() {
        Block.Table t = table("""
                | Ore | Found at | Mine with |
                |---|:---:|--:|
                | Coal | 0 to 192 | **wood** |
                | Diamond | -64 to 16 | iron |
                """);
        assertEquals(List.of(Block.Table.Align.LEFT, Block.Table.Align.CENTER, Block.Table.Align.RIGHT), t.align());
        assertEquals(List.of("Ore", "Found at", "Mine with"), texts(t.header()));
        assertEquals(2, t.rows().size());
        assertEquals(List.of("Coal", "0 to 192", "wood"), texts(t.rows().getFirst()));
        assertEquals(List.of("wood|b||"), runs(t.rows().getFirst().get(2).text()));
        assertTrue(t.problems().isEmpty());
    }

    @Test
    void tableWithoutOuterPipesAndEscapes() {
        Block.Table t = table("""
                a | b
                --- | ---
                x \\| y | z
                """);
        assertEquals(List.of("a", "b"), texts(t.header()));
        assertEquals(List.of("x | y", "z"), texts(t.rows().getFirst()));
    }

    @Test
    void tableNeedsItsSeparator() {
        List<Block> blocks = BookParser.parse("| a | b |\n| c | d |").blocks();
        assertInstanceOf(Block.Paragraph.class, blocks.getFirst());
        // a separator with a different number of cells isn't one
        assertInstanceOf(Block.Paragraph.class, BookParser.parse("| a | b |\n|---|").blocks().getFirst());
        // a plain --- is still an ornament
        List<Block> ornament = BookParser.parse("text\n\n---\n\nmore").blocks();
        assertInstanceOf(Block.Ornament.class, ornament.get(1));
    }

    @Test
    void tableRaggedRows() {
        Block.Table t = table("""
                | a | b | c |
                |---|---|---|
                | 1 |
                | 1 | 2 | 3 | 4 |
                """);
        assertEquals(List.of("1", "", ""), texts(t.rows().get(0)));
        assertEquals(List.of("1", "2", "3"), texts(t.rows().get(1)));
        assertEquals(2, t.problems().size());
        assertTrue(t.problems().get(0).startsWith("row 1 has 1 cells"), t.problems().get(0));
    }

    @Test
    void tableIcons() {
        Block.Table t = table("""
                | | |
                |---|---|
                | ![](item:minecraft:diamond_ore) ![x](item:minecraft:deepslate_diamond_ore) Diamond | ![](item:minecraft:iron_pickaxe) |
                | ![](item:minecraft:potion[potion_contents={potion:"minecraft:healing"}]) Healing | text ![](item:minecraft:stone) |
                """);
        assertTrue(t.header().isEmpty(), "an all-blank header isn't drawn");
        Block.Table.Cell diamond = t.rows().get(0).get(0);
        assertEquals(List.of(item("item", "minecraft:diamond_ore", ""), item("item", "minecraft:deepslate_diamond_ore", "")),
                diamond.icons());
        assertEquals("Diamond", diamond.text().getString());
        Block.Table.Cell pick = t.rows().get(0).get(1);
        assertEquals(1, pick.icons().size());
        assertEquals("", pick.text().getString());
        assertEquals(item("item", "minecraft:potion", "[potion_contents={potion:\"minecraft:healing\"}]"),
                t.rows().get(1).get(0).icons().getFirst());
        // only leading icons are icons
        Block.Table.Cell late = t.rows().get(1).get(1);
        assertTrue(late.icons().isEmpty());
        assertEquals("text ![](item:minecraft:stone)", late.text().getString());
    }

    @Test
    void tableEndsAndNests() {
        List<Block> blocks = BookParser.parse("""
                | a |
                |---|
                | 1 |
                after

                ```spotlight minecraft:diamond
                | b |
                |---|
                | 2 |
                ```
                """).blocks();
        assertInstanceOf(Block.Table.class, blocks.get(0));
        assertEquals("after", ((Block.Paragraph) blocks.get(1)).text().getString());
        Block.Fence fence = assertInstanceOf(Block.Fence.class, blocks.get(2));
        assertInstanceOf(Block.Table.class, fence.body().getFirst());
        // a list still follows a table
        List<Block> list = BookParser.parse("| a |\n|---|\n| 1 |\n\n- item").blocks();
        assertInstanceOf(Block.ListItem.class, list.get(1));
    }

    private static ItemReference item(String scheme, String id, String components) {
        return new ItemReference(scheme, net.minecraft.resources.ResourceLocation.parse(id), components);
    }
}
