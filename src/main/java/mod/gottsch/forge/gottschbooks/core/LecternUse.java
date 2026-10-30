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
package mod.gottsch.forge.gottschbooks.core;

/**
 * What a right-click on a vanilla lectern does with a Gottsch Books book, following vanilla's rules for when a click
 * uses a block (a sneaking player uses one only with both hands empty). Game-free, so it is tested without the game.
 * <ul>
 *     <li>an empty lectern, a book in the clicking hand, not sneaking: {@link #PLACE} the book;</li>
 *     <li>a lectern holding a Gottsch Books book, not sneaking: {@link #READ} it;</li>
 *     <li>the same, sneaking with both hands empty: {@link #TAKE} it, as vanilla's "Take Book" button does;</li>
 *     <li>anything else: {@link #NONE}, and vanilla handles the click.</li>
 * </ul>
 *
 * @author Mark Gottschling on 10/28/2026
 */
public enum LecternUse {
    NONE, PLACE, READ, TAKE;

    /**
     * @param lecternHasBook whether the lectern holds a book (any book)
     * @param lecternBookIsOurs whether that book is a Gottsch Books book
     * @param heldIsOurs whether the clicking hand holds a Gottsch Books book
     * @param mainHand whether the click is for the main hand (a click tries the main hand, then the off hand)
     * @param sneaking whether the player is sneaking
     * @param handsEmpty whether both of the player's hands are empty
     */
    public static LecternUse of(boolean lecternHasBook, boolean lecternBookIsOurs, boolean heldIsOurs,
                                boolean mainHand, boolean sneaking, boolean handsEmpty) {
        if (!lecternHasBook) {
            // sneaking with something in hand skips the block, so the book in hand opens instead, as a written book does
            return heldIsOurs && !sneaking ? PLACE : NONE;
        }
        if (!lecternBookIsOurs || !mainHand) {
            return NONE;
        }
        if (sneaking) {
            return handsEmpty ? TAKE : NONE;
        }
        return READ;
    }
}
