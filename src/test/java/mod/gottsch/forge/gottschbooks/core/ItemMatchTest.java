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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ItemMatch}.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class ItemMatchTest {

    /** a stack's NBT: {D2DungeonMap:{id, rooms:[...]}, other:7} */
    private static CompoundTag dungeonMap(String id, String... rooms) {
        CompoundTag map = new CompoundTag();
        map.putString("id", id);
        ListTag list = new ListTag();
        for (String room : rooms) {
            list.add(StringTag.valueOf(room));
        }
        map.put("rooms", list);
        CompoundTag tag = new CompoundTag();
        tag.put("D2DungeonMap", map);
        tag.putInt("other", 7);
        return tag;
    }

    @Test
    void noNbtMatchesAnyStack() {
        assertTrue(ItemMatch.matches(null, null));
        assertTrue(ItemMatch.matches(new CompoundTag(), dungeonMap("a")));
    }

    @Test
    void nbtMatchesAsASubset() {
        CompoundTag any = new CompoundTag();
        any.put("D2DungeonMap", new CompoundTag());
        assertTrue(ItemMatch.matches(any, dungeonMap("a", "crypt", "vault")));
        assertFalse(ItemMatch.matches(any, null), "no NBT at all");
        CompoundTag other = new CompoundTag();
        other.putInt("other", 7);
        assertFalse(ItemMatch.matches(any, other), "NBT without the tag");

        // nested values must agree; a list matches when the stack's holds each of its elements
        CompoundTag vault = dungeonMap("a", "vault");
        vault.remove("other");
        assertTrue(ItemMatch.matches(vault, dungeonMap("a", "crypt", "vault")));
        assertFalse(ItemMatch.matches(dungeonMap("b"), dungeonMap("a", "crypt")));
        assertFalse(ItemMatch.matches(dungeonMap("a", "tomb"), dungeonMap("a", "crypt")));
    }

    @Test
    void lookupFromIsTheEntrysElseTheBooksElseAll() {
        assertEquals(ItemMatch.FROM_ALL, ItemMatch.lookupFrom(Optional.empty(), Optional.empty()));
        assertEquals(ItemMatch.FROM_ITEMS, ItemMatch.lookupFrom(Optional.empty(), Optional.of(" Items ")));
        assertEquals(ItemMatch.FROM_NONE, ItemMatch.lookupFrom(Optional.of("none"), Optional.of("items")));
        assertEquals(ItemMatch.FROM_ALL, ItemMatch.lookupFrom(Optional.of("all"), Optional.of("items")));
        // a value that isn't one is everything, as /gottschbooks check reports
        assertEquals(ItemMatch.FROM_ALL, ItemMatch.lookupFrom(Optional.of("icons"), Optional.of("items")));
    }
}
