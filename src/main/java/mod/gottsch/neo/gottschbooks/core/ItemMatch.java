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

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Item lookup rules that need no client: whether a stack has the components a book wrote for an item (so a manual can
 * explain one kind of {@code filled_map}, one with {@code custom_data={D2DungeonMap:{}}}, apart from maps in general),
 * and what an entry answers lookups for.
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
     * Whether {@code stack} has {@code components}: each one equal to the stack's, except {@code custom_data}, whose
     * tags need only be among the stack's (a subset: nested compounds match the same way, and a list matches when the
     * stack's list holds each of its elements, as {@link NbtUtils#compareNbt} with partial matching); a removed
     * component ({@code !name}) must be missing from the stack. No components match any stack. The item itself isn't
     * compared.
     */
    public static boolean matches(DataComponentPatch components, ItemStack stack) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> component : components.entrySet()) {
            Optional<?> wanted = component.getValue();
            if (wanted.isEmpty()) {
                if (stack.has(component.getKey())) {
                    return false;
                }
            } else if (wanted.get() instanceof CustomData data) {
                CustomData has = stack.get(DataComponents.CUSTOM_DATA);
                if (has == null || !NbtUtils.compareNbt(data.copyTag(), has.copyTag(), true)) {
                    return false;
                }
            } else if (!wanted.get().equals(stack.get(component.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
