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
package mod.gottsch.forge.gottschbooks.api;

import mod.gottsch.forge.gottschbooks.core.BookReading;
import mod.gottsch.forge.gottschbooks.core.BookStacks;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

/**
 * The library's common API: which book a stack is, and which books a player has read.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class Books {

    private Books() {}

    /** a stack of {@code item} that is book {@code id} */
    public static ItemStack of(Item item, ResourceLocation id) {
        ItemStack stack = new ItemStack(item);
        setId(stack, id);
        return stack;
    }

    public static Optional<ResourceLocation> getId(ItemStack stack) {
        return BookStacks.get(stack);
    }

    public static void setId(ItemStack stack, ResourceLocation id) {
        BookStacks.set(stack, id);
    }

    /** the book's {@code gottschbooks:book} entry, from {@code holders} (the level's, or the creative tab's) */
    public static Optional<BookDef> getDef(ResourceLocation id, HolderLookup.Provider holders) {
        return holders.lookup(GottschBooksRegistries.BOOK)
                .flatMap(books -> books.get(ResourceKey.create(GottschBooksRegistries.BOOK, id)))
                .map(holder -> holder.value());
    }

    /** the ids of the books {@code player} has read (server side; the client copy is not synced) */
    public static Set<ResourceLocation> read(Player player) {
        return BookReading.read(player);
    }

    public static boolean hasRead(Player player, ResourceLocation book) {
        return read(player).contains(book);
    }

    /** Records that {@code player} read {@code book} and fires {@code gottschbooks:book_read}. */
    public static void markRead(ServerPlayer player, ResourceLocation book) {
        BookReading.markRead(player, book);
    }
}
