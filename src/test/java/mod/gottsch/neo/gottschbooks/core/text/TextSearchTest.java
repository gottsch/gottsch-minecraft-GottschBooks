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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TextSearch}.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class TextSearchTest {

    @Test
    void termsAreLowerCaseDistinctWords() {
        assertEquals(List.of("iron", "ore"), TextSearch.terms("  Iron   ORE iron "));
        assertEquals(List.of(), TextSearch.terms("   "));
    }

    @Test
    void piecesCoverTextTablesListsAndFenceBodies() {
        BookText text = BookParser.parse("""
                # Mining
                Dig **deep** for diamonds.

                - bring a *torch*

                | Ore | Depth |
                |---|---|
                | Gold | low |

                ```spotlight minecraft:diamond
                Shiny.
                ```
                ![A cave](item:minecraft:stone)
                """);
        List<String> pieces = TextSearch.pieces(text.blocks());
        assertTrue(pieces.contains("Mining"));
        assertTrue(pieces.contains("Dig deep for diamonds."), pieces.toString());
        assertTrue(pieces.contains("bring a torch"));
        assertTrue(pieces.containsAll(List.of("Ore", "Depth", "Gold", "low")));
        assertTrue(pieces.contains("Shiny."));
        assertTrue(pieces.contains("A cave"));
    }

    @Test
    void everyTermMustOccurSomewhere() {
        List<String> pieces = List.of("Smelt raw iron in a furnace.", "A blast furnace is faster.");
        assertTrue(TextSearch.matches("Furnaces", pieces, TextSearch.terms("IRON blast")));
        // one word in the title, one in the text
        assertTrue(TextSearch.matches("Furnaces", pieces, TextSearch.terms("furnaces raw")));
        assertFalse(TextSearch.matches("Furnaces", pieces, TextSearch.terms("iron gold")));
        assertFalse(TextSearch.matches("Furnaces", pieces, List.of()));
    }

    @Test
    void titleAndCount() {
        assertTrue(TextSearch.inTitle("Furnaces and Workstations", TextSearch.terms("work furn")));
        assertFalse(TextSearch.inTitle("Furnaces", TextSearch.terms("furnace iron")));
        assertEquals(3, TextSearch.count(List.of("Iron, iron.", "more IRON"), List.of("iron")));
    }

    @Test
    void firstHitTriesTermsInOrder() {
        List<String> pieces = List.of("Coal burns.", "Iron ore, then more iron.");
        assertEquals(Optional.of(new TextSearch.Hit(1, 0, 4)), TextSearch.first(pieces, List.of("iron", "coal")));
        // a term found nowhere is skipped for the next
        assertEquals(Optional.of(new TextSearch.Hit(0, 0, 4)), TextSearch.first(pieces, List.of("gold", "coal")));
        assertEquals(Optional.empty(), TextSearch.first(pieces, List.of("gold")));
    }

    @Test
    void indexOfIgnoresCaseAndKeepsIndices() {
        assertEquals(4, TextSearch.indexOf("Raw IRON", "iron", 0));
        assertEquals(-1, TextSearch.indexOf("Raw IRON", "iron", 5));
        assertEquals(-1, TextSearch.indexOf("Raw", "", 0));
        // "İ" lower-cases to two chars; matching must not shift what follows it
        assertEquals(2, TextSearch.indexOf("İ ore", "ore", 0));
    }
}
