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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The plain-text side of a manual's search: a query's words, a file's text as plain strings, and where the words
 * occur. Matching ignores case, and every word of the query must occur somewhere for a file to match.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class TextSearch {

    /** an occurrence: the piece it is in, and where in that piece */
    public record Hit(int piece, int start, int end) {}

    private TextSearch() {}

    /** the query's words, lower case, each once, in order */
    public static List<String> terms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        for (String word : query.strip().split("\\s+")) {
            if (!word.isEmpty()) {
                terms.add(word.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(terms);
    }

    /** the text of {@code blocks} as plain strings: each paragraph, heading, list item, table cell and caption */
    public static List<String> pieces(List<Block> blocks) {
        List<String> pieces = new ArrayList<>();
        collect(blocks, pieces);
        return pieces;
    }

    private static void collect(List<Block> blocks, List<String> pieces) {
        for (Block block : blocks) {
            switch (block) {
                case Block.Paragraph p -> pieces.add(p.text().getString());
                case Block.Heading h -> pieces.add(h.text().getString());
                case Block.ListItem item -> pieces.add(item.text().getString());
                case Block.Illustration i -> {
                    if (!i.caption().isEmpty()) {
                        pieces.add(i.caption());
                    }
                }
                case Block.Table t -> {
                    t.header().forEach(cell -> pieces.add(cell.text().getString()));
                    t.rows().forEach(row -> row.forEach(cell -> pieces.add(cell.text().getString())));
                }
                case Block.Fence f -> collect(f.body(), pieces);
                case Block.Ornament o -> { }
                case Block.PageBreak b -> { }
            }
        }
    }

    /** whether every term occurs in {@code title} or one of {@code pieces} */
    public static boolean matches(String title, List<String> pieces, List<String> terms) {
        if (terms.isEmpty()) {
            return false;
        }
        for (String term : terms) {
            if (indexOf(title, term, 0) < 0 && pieces.stream().noneMatch(piece -> indexOf(piece, term, 0) >= 0)) {
                return false;
            }
        }
        return true;
    }

    /** whether every term occurs in {@code title} */
    public static boolean inTitle(String title, List<String> terms) {
        return !terms.isEmpty() && terms.stream().allMatch(term -> indexOf(title, term, 0) >= 0);
    }

    /** how many times the terms occur in {@code pieces}, all told */
    public static int count(List<String> pieces, List<String> terms) {
        int count = 0;
        for (String piece : pieces) {
            for (String term : terms) {
                for (int at = indexOf(piece, term, 0); at >= 0; at = indexOf(piece, term, at + term.length())) {
                    count++;
                }
            }
        }
        return count;
    }

    /** the first occurrence of the first term that occurs in {@code pieces} at all */
    public static Optional<Hit> first(List<String> pieces, List<String> terms) {
        for (String term : terms) {
            for (int p = 0; p < pieces.size(); p++) {
                int at = indexOf(pieces.get(p), term, 0);
                if (at >= 0) {
                    return Optional.of(new Hit(p, at, at + term.length()));
                }
            }
        }
        return Optional.empty();
    }

    /** where {@code term} (lower case) first occurs in {@code text} from {@code from}, ignoring case; -1 if nowhere */
    public static int indexOf(String text, String term, int from) {
        if (term.isEmpty()) {
            return -1;
        }
        // regionMatches rather than lower-casing the text, which can change its length and so its indices
        for (int i = Math.max(0, from); i + term.length() <= text.length(); i++) {
            if (text.regionMatches(true, i, term, 0, term.length())) {
                return i;
            }
        }
        return -1;
    }
}
