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

import java.util.function.IntUnaryOperator;

/**
 * A table's column widths, as a web page sizes an automatic table. Each column has a natural width (its widest cell on
 * one line) and a minimum (its widest word or icon strip):
 * <ol>
 *     <li>if the natural widths fit, they are used, and the table is no wider than it needs to be;</li>
 *     <li>else, if the minimums fit, each column gets its minimum, and the space left is shared out in proportion to
 *     how much more each column would like, so long text gets the room and short columns don't wrap;</li>
 *     <li>else the minimums are scaled down to fit, and the reader breaks words that are still too long.</li>
 * </ol>
 * Font-free, so it is tested without the game.
 *
 * @author Mark Gottschling on 10/27/2026
 */
public final class TableWidths {

    private TableWidths() {}

    /**
     * @param natural   each column's width with no wrapping
     * @param min       each column's narrowest width without breaking a word (clamped to {@code natural})
     * @param available the width the table may take, gaps included
     * @param gap       the space between two columns
     * @return each column's width; with the gaps, never more than {@code available}
     */
    public static int[] columns(int[] natural, int[] min, int available, int gap) {
        int n = natural.length;
        int[] widths = new int[n];
        if (n == 0) {
            return widths;
        }
        int space = Math.max(n, available - gap * (n - 1));
        int[] lo = new int[n];
        long naturalSum = 0, minSum = 0;
        for (int i = 0; i < n; i++) {
            lo[i] = Math.max(1, Math.min(min[i], natural[i]));
            naturalSum += Math.max(lo[i], natural[i]);
            minSum += lo[i];
        }
        if (naturalSum <= space) {
            for (int i = 0; i < n; i++) {
                widths[i] = Math.max(lo[i], natural[i]);
            }
            return widths;
        }
        if (minSum <= space) {
            // the minimums, then the rest by how much more each column wants
            long wantSum = naturalSum - minSum;
            int left = (int) (space - minSum);
            return share(lo, i -> Math.max(lo[i], natural[i]) - lo[i], wantSum, left);
        }
        // not even the minimums fit: scale them down
        int[] none = new int[n];
        return share(none, i -> lo[i], minSum, space);
    }

    /** {@code base} plus {@code amount} shared by weight, rounded so the shares add up to exactly {@code amount} */
    private static int[] share(int[] base, IntUnaryOperator weight, long weightSum, int amount) {
        int n = base.length;
        int[] out = base.clone();
        if (weightSum <= 0) {
            out[n - 1] += amount;
            return out;
        }
        int given = 0;
        double exact = 0;
        for (int i = 0; i < n; i++) {
            exact += (double) amount * weight.applyAsInt(i) / weightSum;
            int upTo = (int) Math.round(exact);
            out[i] += upTo - given;
            given = upTo;
        }
        return out;
    }
}
