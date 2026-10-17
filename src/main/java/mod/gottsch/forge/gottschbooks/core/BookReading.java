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

import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.core.advancement.GottschBooksCriteria;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Which books a player has read: a list of ids in the player's persisted data ({@code gottschbooks:read_books} under
 * {@link Player#PERSISTED_NBT_TAG}), which Forge copies to the new player on death and respawn. The NeoForge build
 * keeps the same set in a data attachment.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class BookReading {
    private static final String KEY = GottschBooks.MOD_ID + ":read_books";

    private BookReading() {}

    public static Set<ResourceLocation> read(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        Set<ResourceLocation> read = new HashSet<>();
        for (Tag tag : persisted.getList(KEY, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            if (id != null) {
                read.add(id);
            }
        }
        return Set.copyOf(read);
    }

    /** Records that {@code player} read {@code book}, then lets the {@code book_read} advancements check. */
    public static void markRead(ServerPlayer player, ResourceLocation book) {
        Set<ResourceLocation> read = read(player);
        if (!read.contains(book)) {
            Set<ResourceLocation> updated = new HashSet<>(read);
            updated.add(book);
            read = Set.copyOf(updated);
            ListTag list = new ListTag();
            read.forEach(id -> list.add(StringTag.valueOf(id.toString())));
            CompoundTag data = player.getPersistentData();
            CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
            persisted.put(KEY, list);
            data.put(Player.PERSISTED_NBT_TAG, persisted);
        }
        GottschBooksCriteria.BOOK_READ.trigger(player, read);
    }
}
