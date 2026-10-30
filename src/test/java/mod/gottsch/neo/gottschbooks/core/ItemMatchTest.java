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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ItemMatch}. They run in a loaded game (the build's unit-test setup), since items and components
 * need the registries.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class ItemMatchTest {

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

    private static ItemStack map(CompoundTag tag) {
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static DataComponentPatch customData(CompoundTag tag) {
        return DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build();
    }

    @Test
    void noComponentsMatchAnyStack() {
        assertTrue(ItemMatch.matches(DataComponentPatch.EMPTY, new ItemStack(Items.FILLED_MAP)));
        assertTrue(ItemMatch.matches(DataComponentPatch.EMPTY, map(dungeonMap("a"))));
    }

    @Test
    void customDataMatchesAsASubset() {
        CompoundTag wanted = new CompoundTag();
        wanted.put("D2DungeonMap", new CompoundTag());
        DataComponentPatch any = customData(wanted);
        assertTrue(ItemMatch.matches(any, map(dungeonMap("a", "crypt", "vault"))));
        assertFalse(ItemMatch.matches(any, new ItemStack(Items.FILLED_MAP)), "no custom_data at all");
        CompoundTag other = new CompoundTag();
        other.putInt("other", 7);
        assertFalse(ItemMatch.matches(any, map(other)), "custom_data without the tag");

        // nested values must agree; a list matches when the stack's holds each of its elements
        assertTrue(ItemMatch.matches(customData(dungeonMap("a", "vault")), map(dungeonMap("a", "crypt", "vault"))));
        assertFalse(ItemMatch.matches(customData(dungeonMap("b")), map(dungeonMap("a", "crypt"))));
        assertFalse(ItemMatch.matches(customData(dungeonMap("a", "tomb")), map(dungeonMap("a", "crypt"))));
    }

    @Test
    void otherComponentsMustBeEqual() {
        DataComponentPatch named = DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Atlas")).build();
        ItemStack atlas = new ItemStack(Items.FILLED_MAP);
        atlas.set(DataComponents.CUSTOM_NAME, Component.literal("Atlas"));
        ItemStack other = new ItemStack(Items.FILLED_MAP);
        other.set(DataComponents.CUSTOM_NAME, Component.literal("Chart"));
        assertTrue(ItemMatch.matches(named, atlas));
        assertFalse(ItemMatch.matches(named, other));
        assertFalse(ItemMatch.matches(named, new ItemStack(Items.FILLED_MAP)));
    }

    @Test
    void removedComponentsMustBeMissing() {
        DataComponentPatch unnamed = DataComponentPatch.builder().remove(DataComponents.CUSTOM_NAME).build();
        ItemStack named = new ItemStack(Items.FILLED_MAP);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Atlas"));
        assertTrue(ItemMatch.matches(unnamed, new ItemStack(Items.FILLED_MAP)));
        assertFalse(ItemMatch.matches(unnamed, named));
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
