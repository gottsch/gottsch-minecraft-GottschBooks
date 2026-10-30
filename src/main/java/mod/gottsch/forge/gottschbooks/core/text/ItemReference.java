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

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An item reference in a book (an illustration, a spotlight, an {@code icon:}): {@code ns:id} or
 * {@code scheme:ns:id}, optionally followed by {@code /give}-style NBT, e.g.
 * {@code minecraft:potion{Potion:"minecraft:healing"}}. The NBT is kept as written (braces included, or empty) for
 * the client to parse.
 *
 * @author Mark Gottschling on 10/26/2026
 */
public record ItemReference(String scheme, ResourceLocation id, String nbt) {
    /** the scheme of a reference with none: {@code ns:id} is an item */
    public static final String ITEM = "item";

    /** {@code reference} split into its parts; empty when blank or the id isn't a valid resource location */
    public static Optional<ItemReference> parse(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        String ref = reference.strip();
        // the NBT holds colons of its own, so only the part before it names the scheme
        int brace = ref.indexOf('{');
        String head = brace < 0 ? ref : ref.substring(0, brace);
        String nbt = brace < 0 ? "" : ref.substring(brace);
        int first = head.indexOf(':');
        int second = first < 0 ? -1 : head.indexOf(':', first + 1);
        String scheme = second > 0 ? head.substring(0, first) : ITEM;
        ResourceLocation id = ResourceLocation.tryParse(second > 0 ? head.substring(first + 1) : head);
        return id == null ? Optional.empty() : Optional.of(new ItemReference(scheme, id, nbt));
    }

    /**
     * A list of references (and {@code #tags}) separated by commas or spaces, as in {@code items:}. Separators inside
     * brackets, braces or quotes belong to a reference's components, so
     * {@code minecraft:filled_map[custom_data={a:1, b:2}], #minecraft:axes} is two tokens.
     */
    public static List<String> split(String list) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < list.length(); i++) {
            char c = list.charAt(i);
            if (quote != 0) {
                if (c == '\\' && i + 1 < list.length()) {
                    token.append(c);
                    c = list.charAt(++i);
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[' || c == '{') {
                depth++;
            } else if ((c == ']' || c == '}') && depth > 0) {
                depth--;
            } else if (depth == 0 && (c == ',' || Character.isWhitespace(c))) {
                if (!token.isEmpty()) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                continue;
            }
            token.append(c);
        }
        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    /** the reference as a book would write it, with the scheme spelled out */
    public String text() {
        return scheme + ":" + id + nbt;
    }
}
