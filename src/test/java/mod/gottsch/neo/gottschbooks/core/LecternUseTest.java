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
package mod.gottsch.neo.gottschbooks.core;

import org.junit.jupiter.api.Test;

import static mod.gottsch.neo.gottschbooks.core.LecternUse.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LecternUse}.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class LecternUseTest {

    @Test
    void ourBookGoesOnAnEmptyLectern() {
        assertEquals(PLACE, of(false, false, true, true, false, false));
        assertEquals(PLACE, of(false, false, true, false, false, false), "from the off hand too");
    }

    @Test
    void sneakingWithABookDoesNotPlaceIt() {
        // vanilla skips the block, so the book in hand opens, as a written book does
        assertEquals(NONE, of(false, false, true, true, true, false));
    }

    @Test
    void otherItemsOnAnEmptyLecternAreVanillas() {
        assertEquals(NONE, of(false, false, false, true, false, false));
        assertEquals(NONE, of(false, false, false, true, false, true));
    }

    @Test
    void ourBookOnALecternIsRead() {
        assertEquals(READ, of(true, true, false, true, false, true));
        assertEquals(READ, of(true, true, false, true, false, false), "whatever the hand holds");
        assertEquals(READ, of(true, true, true, true, false, false), "even another of our books");
    }

    @Test
    void readOnceForTheMainHand() {
        assertEquals(NONE, of(true, true, false, false, false, false));
    }

    @Test
    void sneakingWithEmptyHandsTakesTheBook() {
        assertEquals(TAKE, of(true, true, false, true, true, true));
    }

    @Test
    void sneakingWithSomethingInHandDoesNothing() {
        // vanilla skips the block when sneaking with an item in either hand
        assertEquals(NONE, of(true, true, false, true, true, false));
        assertEquals(NONE, of(true, true, true, true, true, false));
    }

    @Test
    void aVanillaBookOnALecternIsVanillas() {
        assertEquals(NONE, of(true, false, false, true, false, true));
        assertEquals(NONE, of(true, false, true, true, false, false));
        assertEquals(NONE, of(true, false, false, true, true, true));
    }
}
