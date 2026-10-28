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
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Gives each player the books marked {@code "give_on_first_join": true}, once each: the first time they join after
 * the book's entry exists, so a book added to a running world reaches existing players too. Books marked
 * {@code "announce_on_first_join": true} are announced the same way instead: one chat line each, with a [Get it]
 * link ({@code /gottschbooks get}), unless the server config turns announcements off. Which books a player has been
 * given ({@code gottschbooks:given_books}) and told about ({@code gottschbooks:announced_books}) are lists in the
 * player's persisted data (under {@link Player#PERSISTED_NBT_TAG}), which Forge keeps through death.
 * <p>
 * A book's mod can word its line with the lang key {@code book.<namespace>.<path>.announce}, where {@code %s} is the
 * book's name: "%s is in the Functional Blocks tab, or craft one from a book and a bone".
 *
 * @author Mark Gottschling on 10/17/2026
 */
@Mod.EventBusSubscriber(modid = GottschBooks.MOD_ID)
public final class BookGiving {
    private static final String GIVEN = GottschBooks.MOD_ID + ":given_books";
    private static final String ANNOUNCED = GottschBooks.MOD_ID + ":announced_books";

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
        giveNew(player, books);
        if (ServerConfig.ANNOUNCE_BOOKS.get()) {
            announceNew(player, books);
        }
    }

    private static void giveNew(ServerPlayer player, Registry<BookDef> books) {
        Set<ResourceLocation> given = ids(player, GIVEN);
        boolean changed = false;
        for (Holder.Reference<BookDef> book : books.holders().toList()) {
            BookDef def = book.value();
            ResourceLocation id = book.key().location();
            if (!def.giveOnFirstJoin() || def.disabled() || given.contains(id)) {
                continue;
            }
            give(player, def, id);
            given.add(id);
            changed = true;
        }
        if (changed) {
            save(player, GIVEN, given);
        }
    }

    private static void announceNew(ServerPlayer player, Registry<BookDef> books) {
        Set<ResourceLocation> announced = ids(player, ANNOUNCED);
        boolean changed = false;
        for (Holder.Reference<BookDef> book : books.holders().toList()) {
            BookDef def = book.value();
            ResourceLocation id = book.key().location();
            if (!def.announceOnFirstJoin() || def.giveOnFirstJoin() || def.disabled() || announced.contains(id)) {
                continue;
            }
            // a player who already has the book needs no telling
            if (!carries(player.getInventory(), id)) {
                player.sendSystemMessage(announcement(stack(def, id), id));
            }
            announced.add(id);
            changed = true;
        }
        if (changed) {
            save(player, ANNOUNCED, announced);
        }
    }

    /** the ids listed under {@code key} in the player's persisted data */
    private static Set<ResourceLocation> ids(Player player, String key) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (Tag tag : player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(key, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static void save(Player player, String key, Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(key, list);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** "<book> is available." (or the book's own {@code .announce} line), then the [Get it] link */
    private static Component announcement(ItemStack stack, ResourceLocation id) {
        Component name = name(stack, id).copy().withStyle(ChatFormatting.YELLOW);
        // the client shows the book's own line when its lang has one, else the default (%2$s); both have the name as %s
        Component line = Component.translatableWithFallback(BookItem.nameKey(id) + ".announce", "%2$s",
                name, Component.translatable("gottschbooks.announce", name));
        Component link = Component.translatable("gottschbooks.announce.get").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + GottschBooks.MOD_ID + " get " + id))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("gottschbooks.announce.get.hover"))));
        return Component.empty().append(line).append(" ").append(link);
    }

    /**
     * The book's name, translated on the player's client: its {@code book.<namespace>.<path>} lang line, else the
     * item's name (the server's language has neither). Hovering shows the item.
     */
    public static Component name(ItemStack stack, ResourceLocation id) {
        return Component.translatableWithFallback(BookItem.nameKey(id), "%s", stack.getItem().getDescription())
                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack))));
    }

    /** whether the inventory holds book {@code id} */
    public static boolean carries(Inventory inventory, ResourceLocation id) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            Optional<ResourceLocation> book = stack.getItem() instanceof BookItem item ? item.bookId(stack) : Books.getId(stack);
            if (book.filter(id::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** puts the book in the player's inventory, or drops it at their feet when it's full; a copy of the stack given */
    public static ItemStack give(ServerPlayer player, BookDef def, ResourceLocation id) {
        ItemStack stack = stack(def, id);
        ItemStack given = stack.copy();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return given;
    }

    /** the book as a stack of its {@code item} (a {@link BookItem}; default {@code gottschbooks:book}) */
    public static ItemStack stack(BookDef def, ResourceLocation id) {
        Item item = def.item().flatMap(BuiltInRegistries.ITEM::getOptional).orElse(GottschBooksItems.BOOK.get());
        if (!(item instanceof BookItem)) {
            GottschBooks.LOGGER.warn("book {}: item {} is not a book item; giving gottschbooks:book instead",
                    id, BuiltInRegistries.ITEM.getKey(item));
            item = GottschBooksItems.BOOK.get();
        }
        return Books.of(item, id);
    }
}
