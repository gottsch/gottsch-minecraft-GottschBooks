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
package mod.gottsch.neo.gottschbooks.client;

import mod.gottsch.neo.gottschbooks.core.text.Block;
import mod.gottsch.neo.gottschbooks.core.text.TextSearch;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A manual's search: the text of each entry the player can read (its title, its text, and the names of the items it
 * spotlights or illustrates), and the entries a query finds. An entry is found when every word of the query occurs
 * in it, ignoring case; entries whose title has every word come first, then those that mention the words most, then
 * contents order.
 *
 * @author Mark Gottschling on 10/28/2026
 */
final class ManualSearch {
    private static final String ELLIPSIS = "…";

    /** an entry found, and one line of its text around the first word found (empty when only its title matched) */
    record Result(Manual.Entry entry, Optional<Component> snippet) {}

    private record Indexed(Manual.Entry entry, String title, List<String> pieces) {}

    private final List<Indexed> entries = new ArrayList<>();

    ManualSearch(Manual manual) {
        for (Manual.Entry entry : manual.allEntries()) {
            if (entry.unlocked()) {
                List<String> pieces = new ArrayList<>(TextSearch.pieces(entry.text().blocks()));
                itemNames(entry.text().blocks(), pieces);
                entries.add(new Indexed(entry, entry.title().getString(), pieces));
            }
        }
    }

    /** the names of the items a spotlight or illustration shows, which the text often doesn't repeat */
    private static void itemNames(List<Block> blocks, List<String> names) {
        for (Block block : blocks) {
            if (block instanceof Block.Fence f) {
                if (f.kind().equals("spotlight")) {
                    Icons.stack(f.argument()).ifPresent(item -> names.add(item.getHoverName().getString()));
                }
                itemNames(f.body(), names);
            } else if (block instanceof Block.Illustration i) {
                Icons.stack(i.item()).ifPresent(item -> names.add(item.getHoverName().getString()));
            }
        }
    }

    /** the entries {@code query} finds, best first, each with a snippet that fits {@code width} */
    List<Result> find(String query, Font font, int width) {
        List<String> terms = TextSearch.terms(query);
        record Scored(Indexed indexed, boolean title, int count) {}
        List<Scored> found = new ArrayList<>();
        for (Indexed indexed : entries) {
            if (TextSearch.matches(indexed.title(), indexed.pieces(), terms)) {
                found.add(new Scored(indexed, TextSearch.inTitle(indexed.title(), terms),
                        TextSearch.count(indexed.pieces(), terms)));
            }
        }
        // a stable sort, so ties keep contents order
        found.sort(Comparator.comparing((Scored s) -> !s.title()).thenComparing(Scored::count, Comparator.reverseOrder()));
        List<Result> results = new ArrayList<>();
        for (Scored scored : found) {
            List<String> pieces = scored.indexed().pieces();
            Optional<Component> snippet = TextSearch.first(pieces, terms)
                    .map(hit -> snippet(pieces.get(hit.piece()), hit, font, width));
            results.add(new Result(scored.indexed().entry(), snippet));
        }
        return results;
    }

    /**
     * One line of {@code piece} around {@code hit}, the word in bold: a little of what comes before it (from a word's
     * start), the rest after it, each cut with "…" where it doesn't fit.
     */
    static Component snippet(String piece, TextSearch.Hit hit, Font font, int width) {
        String before = piece.substring(0, hit.start());
        String word = piece.substring(hit.start(), hit.end());
        String after = piece.substring(hit.end());
        Component bold = Component.literal(word).withStyle(ChatFormatting.BOLD);
        int wordW = font.width(bold);

        // up to a third of the room before the word
        int beforeW = Math.max(0, (width - wordW) / 3);
        if (font.width(before) > beforeW) {
            String tail = font.plainSubstrByWidth(before, Math.max(0, beforeW - font.width(ELLIPSIS)), true);
            int space = tail.indexOf(' ');
            if (space >= 0 && space < tail.length() - 1) {
                tail = tail.substring(space + 1);
            }
            before = ELLIPSIS + tail;
        }
        int afterW = width - font.width(before) - wordW;
        if (font.width(after) > afterW) {
            after = font.plainSubstrByWidth(after, Math.max(0, afterW - font.width(ELLIPSIS))).stripTrailing() + ELLIPSIS;
        }
        return Component.literal(before).append(bold).append(after);
    }
}
