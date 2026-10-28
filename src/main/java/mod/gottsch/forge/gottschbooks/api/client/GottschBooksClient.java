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
package mod.gottsch.forge.gottschbooks.api.client;

import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.client.BookLoader;
import mod.gottsch.forge.gottschbooks.client.BookScreen;
import mod.gottsch.forge.gottschbooks.client.Icons;
import mod.gottsch.forge.gottschbooks.client.LoadedBook;
import mod.gottsch.forge.gottschbooks.client.Manual;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Opens books on the client. <b>Client only</b>: call it from client code (a screen, a keybind, the client side of
 * {@code Item#use}), never from common code a dedicated server runs.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksClient {

    private GottschBooksClient() {}

    /** Opens {@code book}; a manual opens where it was left (else its contents), with its {@code icon:} on the title. */
    public static boolean open(ResourceLocation book) {
        return open(book, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Opens {@code book}, in the player's language when it has that translation.
     *
     * @param icon drawn on the title page; usually the stack the player is holding
     * @return false, after telling the player, when the book has no text loaded
     */
    public static boolean open(ResourceLocation book, ItemStack icon) {
        return open(book, Optional.of(icon), Optional.empty(), Optional.empty());
    }

    /**
     * Opens manual {@code book} at {@code entry} ({@code "gems"}, or {@code "gems#ruby"} for a heading's anchor). An
     * entry that doesn't exist, or is still locked, opens the contents instead.
     */
    public static boolean open(ResourceLocation book, String entry) {
        return open(book, Optional.empty(), Optional.of(entry), Optional.empty());
    }

    /**
     * Opens manual {@code book} at {@code entry}, on the page that shows {@code item} (its spotlight, else a recipe
     * that makes it), else the entry's first page. Closing the book goes back to the screen that was open, such as
     * the inventory the item was in.
     */
    public static boolean open(ResourceLocation book, String entry, Item item) {
        return open(book, Optional.empty(), Optional.of(entry), Optional.of(item));
    }

    private static boolean open(ResourceLocation book, Optional<ItemStack> icon, Optional<String> entry, Optional<Item> item) {
        Minecraft minecraft = Minecraft.getInstance();
        String locale = minecraft.getLanguageManager().getSelected();
        Optional<LoadedBook> loaded = BookLoader.get(book);
        if (loaded.isPresent() && loaded.get().isManual()) {
            LoadedBook manual = loaded.get();
            ItemStack shown = icon.orElseGet(() -> manual.meta().get("icon").flatMap(Icons::stack)
                    .orElseGet(() -> new ItemStack(Items.BOOK)));
            BookScreen screen = new BookScreen(manual, Manual.of(manual, locale), shown.copyWithCount(1), entry);
            item.ifPresent(it -> screen.showing(it, minecraft.screen));
            minecraft.setScreen(screen);
            return true;
        }
        Optional<BookText> text = loaded.flatMap(b -> b.entry(LoadedBook.LORE_ENTRY, locale));
        if (text.isEmpty()) {
            GottschBooks.LOGGER.warn("no text for book {} (looked for assets/{}/books/{}/<locale>/{}.md)",
                    book, book.getNamespace(), book.getPath(), LoadedBook.LORE_ENTRY);
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("gottschbooks.book.missing", book.toString()), true);
            }
            return false;
        }
        minecraft.setScreen(new BookScreen(loaded.get(), text.get(), icon.orElseGet(() -> new ItemStack(Items.BOOK)).copyWithCount(1)));
        return true;
    }
}
