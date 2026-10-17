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

import com.mojang.serialization.Codec;
import mod.gottsch.neo.gottschbooks.GottschBooks;
import mod.gottsch.neo.gottschbooks.core.advancement.GottschBooksCriteria;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Which books a player has read: a data attachment on the player ({@code gottschbooks:read_books}), saved with them
 * and kept through death. Generalised from Magic Treasures' {@code LoreBookReading}.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class BookReading {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GottschBooks.MOD_ID);

    /** the ids of the books read; stored as a list, held as an unmodifiable set */
    private static final Codec<Set<ResourceLocation>> CODEC =
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf);

    public static final Supplier<AttachmentType<Set<ResourceLocation>>> READ_BOOKS = ATTACHMENT_TYPES.register(
            "read_books", () -> AttachmentType.<Set<ResourceLocation>>builder(() -> Set.of())
                    .serialize(CODEC)
                    .copyOnDeath()
                    .build());

    private BookReading() {}

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }

    public static Set<ResourceLocation> read(Player player) {
        return player.getData(READ_BOOKS);
    }

    /** Records that {@code player} read {@code book}, then lets the {@code book_read} advancements check. */
    public static void markRead(ServerPlayer player, ResourceLocation book) {
        Set<ResourceLocation> read = read(player);
        if (!read.contains(book)) {
            Set<ResourceLocation> updated = new HashSet<>(read);
            updated.add(book);
            read = Set.copyOf(updated);
            player.setData(READ_BOOKS, read);
        }
        GottschBooksCriteria.BOOK_READ.get().trigger(player, read);
    }
}
