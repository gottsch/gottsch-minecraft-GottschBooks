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
package mod.gottsch.forge.gottschbooks.client;

import com.mojang.blaze3d.platform.InputConstants;
import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.api.BookItem;
import mod.gottsch.forge.gottschbooks.api.Books;
import mod.gottsch.forge.gottschbooks.api.client.GottschBooksClient;
import mod.gottsch.forge.gottschbooks.core.GottschBooksItems;
import mod.gottsch.forge.gottschbooks.core.Unlocks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unlock toasts: when the player earns an advancement that unlocks manual entries, a toast says what is new to read
 * ({@link UnlockToast}): one for the entry, or one for the book when several of its entries unlock at once. Only
 * manuals the player can read count: carried, or {@code lookup: always} in book.md, as for the item lookup. Pressing
 * the lookup key (B by default) with no screen open, while the toast shows, opens the manual at the entry (the first,
 * in contents order, when several unlocked). {@code unlockToasts = false} in the client config turns them off.
 * <p>
 * Each client tick compares which of the advancements that lock entries are done with the tick before. That needs no
 * mixin: Forge has no client event for advancement progress, and only a handful of advancements are checked. The
 * server's first advancement packet on joining a world (everything the player already has) only sets the starting
 * point, and so does a resource reload that changes the books, so neither shows toasts.
 *
 * @author Mark Gottschling on 10/28/2026
 */
@Mod.EventBusSubscriber(modid = GottschBooks.MOD_ID, value = Dist.CLIENT)
public final class UnlockToasts {

    // the locked entries of every manual, and what they were found in; found again when either changes
    private static List<Unlocks.Lock> locks = List.of();
    private static Map<ResourceLocation, Manual> manuals = Map.of();
    @Nullable
    private static Map<ResourceLocation, LoadedBook> lockedBooks;
    @Nullable
    private static String lockedLocale;

    /** the connection's advancements being watched, and which locking advancements were done at the last tick */
    @Nullable
    private static ClientAdvancements watching;
    private static Set<ResourceLocation> done = Set.of();

    /** the toasts shown that may still be on screen, oldest (topmost) first */
    private static final List<UnlockToast> toasts = new ArrayList<>();

    private UnlockToasts() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null) {
            watching = null;
            toasts.clear();
            return;
        }
        boolean found = findLocks(minecraft.getLanguageManager().getSelected());
        ClientAdvancements advancements = connection.getAdvancements();
        if (advancements != watching) {
            // a new connection: wait for the server's first packet, then take what is done as the starting point
            if (advancements.getAdvancements().getAllAdvancements().isEmpty()) {
                return;
            }
            watching = advancements;
            done = done();
            return;
        }
        Set<ResourceLocation> now = done();
        Set<ResourceLocation> newly = Unlocks.newlyDone(done, now);
        done = now;
        toasts.removeIf(UnlockToast::gone);
        if (found || newly.isEmpty() || !ClientConfig.UNLOCK_TOASTS.get()) {
            return;
        }
        Map<ResourceLocation, ItemStack> carried = carried(minecraft.player.getInventory());
        Unlocks.unlocked(locks, newly, book -> carried.containsKey(book) || ItemLookup.lookup(book).equals(ItemLookup.ALWAYS))
                .forEach((book, entries) -> show(minecraft, book, entries, carried.get(book)));
    }

    /** the lookup key, pressed in game while a toast shows, opens its entry */
    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        KeyMapping key = ItemLookup.KEY;
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null || minecraft.player == null
                || toasts.isEmpty() || key.isUnbound()
                || !key.getKey().equals(InputConstants.getKey(event.getKey(), event.getScanCode()))
                || !key.getKeyModifier().isActive(KeyConflictContext.IN_GAME)) {
            return;
        }
        toasts.stream().filter(UnlockToast::showing).findFirst().ifPresent(toast -> {
            toast.dismiss();
            GottschBooksClient.open(toast.book(), toast.entry());
        });
    }

    private static void show(Minecraft minecraft, ResourceLocation book, List<String> entries, @Nullable ItemStack carried) {
        Manual manual = manuals.get(book);
        Optional<Manual.Entry> first = manual == null ? Optional.empty()
                : manual.allEntries().stream().filter(entry -> entry.id().equals(entries.get(0))).findFirst();
        if (first.isEmpty()) {
            return;
        }
        Component detail = entries.size() == 1 ? first.get().title()
                : Component.translatable("gottschbooks.unlock.toast.entries", entries.size());
        Component hint = ItemLookup.KEY.isUnbound() ? null
                : Component.translatable("gottschbooks.unlock.toast.open", ItemLookup.KEY.getTranslatedKeyMessage());
        UnlockToast toast = new UnlockToast(book, first.get().id(), icon(book, carried),
                Component.translatable("gottschbooks.unlock.toast.title", manual.title()), detail, hint);
        toasts.add(toast);
        minecraft.getToasts().addToast(toast);
    }

    /** the book as the player carries it, else book.md's {@code icon}, else a Gottsch Books book */
    private static ItemStack icon(ResourceLocation book, @Nullable ItemStack carried) {
        if (carried != null) {
            return carried.copyWithCount(1);
        }
        return BookLoader.get(book).flatMap(b -> b.meta().get("icon")).flatMap(Icons::stack)
                .orElseGet(() -> Books.of(GottschBooksItems.BOOK.get(), book));
    }

    /** the books in {@code inventory}, each with the first stack that is it */
    private static Map<ResourceLocation, ItemStack> carried(Inventory inventory) {
        Map<ResourceLocation, ItemStack> books = new HashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            Optional<ResourceLocation> id = stack.getItem() instanceof BookItem item ? item.bookId(stack) : Books.getId(stack);
            id.ifPresent(book -> books.putIfAbsent(book, stack));
        }
        return books;
    }

    /** which of the locking advancements the player has */
    private static Set<ResourceLocation> done() {
        return locks.stream().map(Unlocks.Lock::advancement).filter(Locks::has).collect(Collectors.toSet());
    }

    /** finds every manual's locked entries again when the books or the language changed; true when it did */
    private static boolean findLocks(String locale) {
        Map<ResourceLocation, LoadedBook> books = BookLoader.all();
        if (books == lockedBooks && locale.equals(lockedLocale)) {
            return false;
        }
        lockedBooks = books;
        lockedLocale = locale;
        List<Unlocks.Lock> found = new ArrayList<>();
        Map<ResourceLocation, Manual> byBook = new HashMap<>();
        books.values().stream().filter(LoadedBook::isManual).sorted(Comparator.comparing(book -> book.id().toString()))
                .forEach(book -> {
                    Manual manual = Manual.of(book, locale);
                    for (Manual.Entry entry : manual.allEntries()) {
                        entry.advancement().ifPresent(advancement -> {
                            found.add(new Unlocks.Lock(book.id(), entry.id(), advancement));
                            byBook.put(book.id(), manual);
                        });
                    }
                });
        locks = found;
        manuals = byBook;
        done = done();
        return true;
    }
}
