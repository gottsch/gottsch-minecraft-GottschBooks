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

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TableWidths}.
 *
 * @author Mark Gottschling on 10/27/2026
 */
class TableWidthsTest {

    private static int total(int[] widths, int gap) {
        return Arrays.stream(widths).sum() + gap * (widths.length - 1);
    }

    @Test
    void naturalWidthsWhenTheyFit() {
        assertArrayEquals(new int[] {30, 50, 20}, TableWidths.columns(new int[] {30, 50, 20}, new int[] {10, 20, 20}, 200, 6));
    }

    @Test
    void extraSpaceGoesToColumnsThatWantIt() {
        // 10 + 100 + 20 natural, 10 + 30 + 20 minimum, 100 - 12 = 88 to share
        int[] widths = TableWidths.columns(new int[] {10, 100, 20}, new int[] {10, 30, 20}, 100, 6);
        assertEquals(100, total(widths, 6));
        assertEquals(10, widths[0], "a column already at its natural width doesn't wrap");
        assertEquals(20, widths[2]);
        assertEquals(58, widths[1]);
    }

    @Test
    void sharedInProportionToWhatEachWants() {
        int[] widths = TableWidths.columns(new int[] {100, 300}, new int[] {20, 20}, 206, 6);
        assertEquals(206, total(widths, 6));
        // 200 - 40 = 160 shared 80:280
        assertEquals(20 + 36, widths[0]);
        assertEquals(20 + 124, widths[1]);
    }

    @Test
    void minimumsScaledDownWhenEvenTheyDontFit() {
        int[] widths = TableWidths.columns(new int[] {80, 80, 80}, new int[] {60, 60, 30}, 100, 5);
        assertEquals(100, total(widths, 5));
        assertTrue(widths[0] > widths[2], Arrays.toString(widths));
    }

    @Test
    void edgeCases() {
        assertArrayEquals(new int[0], TableWidths.columns(new int[0], new int[0], 100, 6));
        assertArrayEquals(new int[] {100}, TableWidths.columns(new int[] {300}, new int[] {40}, 100, 6));
        // an empty column still gets a pixel, and rounding never loses one
        int[] widths = TableWidths.columns(new int[] {0, 333, 333, 333}, new int[] {0, 1, 1, 1}, 400, 6);
        assertEquals(400, total(widths, 6));
        assertTrue(widths[0] >= 1);
        // a minimum larger than the natural width is clamped to it
        assertArrayEquals(new int[] {10, 20}, TableWidths.columns(new int[] {10, 20}, new int[] {50, 50}, 200, 6));
    }
}
