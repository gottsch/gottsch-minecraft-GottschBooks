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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Item lookup rules that need no client: whether a stack's NBT holds what a book wrote for an item (so a manual can
 * explain one kind of {@code filled_map}, one with {@code {D2DungeonMap:{}}}, apart from maps in general), and what an
 * entry answers lookups for.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class ItemMatch {
    /**
     * an entry's (or book.md's, for every entry) key for what the entry answers item lookups for: {@code all} it shows
     * (spotlights, recipes, icon and {@code items:}), only its {@code items}, or {@code none}
     */
    public static final String LOOKUP_FROM = "lookup_from", FROM_ALL = "all", FROM_ITEMS = "items", FROM_NONE = "none";

    private ItemMatch() {}

    /** an entry's {@code lookup_from:}, else its book's, else {@code all}; a value that isn't one counts as {@code all} */
    public static String lookupFrom(Optional<String> entry, Optional<String> book) {
        return entry.or(() -> book).map(value -> value.strip().toLowerCase(Locale.ROOT))
                .filter(value -> List.of(FROM_ALL, FROM_ITEMS, FROM_NONE).contains(value))
                .orElse(FROM_ALL);
    }

    /**
     * Whether a stack's NBT ({@code stack}, null for none) holds {@code nbt}: each tag written, with the same value,
     * any others ignored (nested compounds the same way, and a list matches when the stack's list holds each of its
     * elements, as {@link NbtUtils#compareNbt} with partial matching). No NBT written (null or empty) matches any
     * stack. The item itself isn't compared.
     */
    public static boolean matches(@Nullable CompoundTag nbt, @Nullable CompoundTag stack) {
        return nbt == null || nbt.isEmpty() || NbtUtils.compareNbt(nbt, stack, true);
    }
}
