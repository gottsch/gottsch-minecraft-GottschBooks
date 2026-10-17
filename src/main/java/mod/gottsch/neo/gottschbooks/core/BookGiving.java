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
import mod.gottsch.neo.gottschbooks.api.BookDef;
import mod.gottsch.neo.gottschbooks.api.BookItem;
import mod.gottsch.neo.gottschbooks.api.Books;
import mod.gottsch.neo.gottschbooks.api.GottschBooksRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Gives each player the books marked {@code "give_on_first_join": true}, once each: the first time they join after
 * the book's entry exists, so a book added to a running world reaches existing players too. Which books a player has
 * been given is kept on the player ({@code gottschbooks:given_books}), through death.
 *
 * @author Mark Gottschling on 10/17/2026
 */
@EventBusSubscriber(modid = GottschBooks.MOD_ID)
public final class BookGiving {
    private static final Codec<Set<ResourceLocation>> CODEC =
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf);

    public static final Supplier<AttachmentType<Set<ResourceLocation>>> GIVEN_BOOKS = BookReading.ATTACHMENT_TYPES.register(
            "given_books", () -> AttachmentType.<Set<ResourceLocation>>builder(() -> Set.of())
                    .serialize(CODEC)
                    .copyOnDeath()
                    .build());

    private BookGiving() {}

    /** loads the class, registering the attachment on {@link BookReading#ATTACHMENT_TYPES} */
    public static void init() {
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Registry<BookDef> books = player.registryAccess().registry(GottschBooksRegistries.BOOK).orElse(null);
        if (books == null) {
            return;
        }
        Set<ResourceLocation> given = new HashSet<>(player.getData(GIVEN_BOOKS));
        boolean changed = false;
        for (Holder.Reference<BookDef> book : books.holders().toList()) {
            BookDef def = book.value();
            ResourceLocation id = book.key().location();
            if (!def.giveOnFirstJoin() || def.disabled() || given.contains(id)) {
                continue;
            }
            ItemStack stack = stack(def, id);
            if (stack.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            given.add(id);
            changed = true;
        }
        if (changed) {
            player.setData(GIVEN_BOOKS, Set.copyOf(given));
        }
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
