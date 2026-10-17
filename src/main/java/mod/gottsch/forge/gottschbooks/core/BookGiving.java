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
import mod.gottsch.forge.gottschbooks.api.BookDef;
import mod.gottsch.forge.gottschbooks.api.BookItem;
import mod.gottsch.forge.gottschbooks.api.Books;
import mod.gottsch.forge.gottschbooks.api.GottschBooksRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Gives each player the books marked {@code "give_on_first_join": true}, once each: the first time they join after
 * the book's entry exists, so a book added to a running world reaches existing players too. Which books a player has
 * been given is a list in the player's persisted data ({@code gottschbooks:given_books} under
 * {@link Player#PERSISTED_NBT_TAG}), which Forge keeps through death.
 *
 * @author Mark Gottschling on 10/17/2026
 */
@Mod.EventBusSubscriber(modid = GottschBooks.MOD_ID)
public final class BookGiving {
    private static final String KEY = GottschBooks.MOD_ID + ":given_books";

    private BookGiving() {}

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Registry<BookDef> books = player.level().registryAccess().registry(GottschBooksRegistries.BOOK).orElse(null);
        if (books == null) {
            return;
        }
        Set<ResourceLocation> given = given(player);
        boolean changed = false;
        for (Holder.Reference<BookDef> book : books.holders().toList()) {
            BookDef def = book.value();
            ResourceLocation id = book.key().location();
            if (!def.giveOnFirstJoin() || def.disabled() || given.contains(id)) {
                continue;
            }
            ItemStack stack = stack(def, id);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            given.add(id);
            changed = true;
        }
        if (changed) {
            ListTag list = new ListTag();
            given.forEach(id -> list.add(StringTag.valueOf(id.toString())));
            CompoundTag data = player.getPersistentData();
            CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
            persisted.put(KEY, list);
            data.put(Player.PERSISTED_NBT_TAG, persisted);
        }
    }

    private static Set<ResourceLocation> given(Player player) {
        Set<ResourceLocation> given = new HashSet<>();
        for (Tag tag : player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(KEY, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            if (id != null) {
                given.add(id);
            }
        }
        return given;
    }

    /** the book as a stack of its {@code item} (a {@link BookItem}; default {@code gottschbooks:book}) */
    private static ItemStack stack(BookDef def, ResourceLocation id) {
        Item item = def.item().flatMap(BuiltInRegistries.ITEM::getOptional).orElse(GottschBooksItems.BOOK.get());
        if (!(item instanceof BookItem)) {
            GottschBooks.LOGGER.warn("book {}: item {} is not a book item; giving gottschbooks:book instead",
                    id, BuiltInRegistries.ITEM.getKey(item));
            item = GottschBooksItems.BOOK.get();
        }
        return Books.of(item, id);
    }
}
