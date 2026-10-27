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

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * An item reference in a book (an illustration, a spotlight, an {@code icon:}): {@code ns:id} or
 * {@code scheme:ns:id}, optionally followed by {@code /give}-style components, e.g.
 * {@code minecraft:potion[potion_contents={potion:"minecraft:healing"}]}. The components are kept as written
 * (brackets included, or empty) for the client to parse against the world's registries.
 *
 * @author Mark Gottschling on 10/26/2026
 */
public record ItemReference(String scheme, ResourceLocation id, String components) {
    /** the scheme of a reference with none: {@code ns:id} is an item */
    public static final String ITEM = "item";

    /** {@code reference} split into its parts; empty when blank or the id isn't a valid resource location */
    public static Optional<ItemReference> parse(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        String ref = reference.strip();
        // the components hold colons of their own, so only the part before them names the scheme
        int bracket = ref.indexOf('[');
        String head = bracket < 0 ? ref : ref.substring(0, bracket);
        String components = bracket < 0 ? "" : ref.substring(bracket);
        int first = head.indexOf(':');
        int second = first < 0 ? -1 : head.indexOf(':', first + 1);
        String scheme = second > 0 ? head.substring(0, first) : ITEM;
        ResourceLocation id = ResourceLocation.tryParse(second > 0 ? head.substring(first + 1) : head);
        return id == null ? Optional.empty() : Optional.of(new ItemReference(scheme, id, components));
    }

    /** the reference as a book would write it, with the scheme spelled out */
    public String text() {
        return scheme + ":" + id + components;
    }
}
