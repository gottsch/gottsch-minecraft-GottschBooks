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
import mod.gottsch.forge.gottschbooks.client.LoadedBook;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    /** Opens {@code book} with a plain book on its title page. */
    public static boolean open(ResourceLocation book) {
        return open(book, new ItemStack(Items.BOOK));
    }

    /**
     * Opens {@code book}, in the player's language when it has that translation.
     *
     * @param icon drawn on the title page; usually the stack the player is holding
     * @return false, after telling the player, when the book has no text loaded
     */
    public static boolean open(ResourceLocation book, ItemStack icon) {
        Minecraft minecraft = Minecraft.getInstance();
        String locale = minecraft.getLanguageManager().getSelected();
        Optional<LoadedBook> loaded = BookLoader.get(book);
        Optional<BookText> text = loaded.flatMap(b -> b.entry(LoadedBook.LORE_ENTRY, locale));
        if (text.isEmpty()) {
            GottschBooks.LOGGER.warn("no text for book {} (looked for assets/{}/books/{}/<locale>/{}.md)",
                    book, book.getNamespace(), book.getPath(), LoadedBook.LORE_ENTRY);
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("gottschbooks.book.missing", book.toString()), true);
            }
            return false;
        }
        minecraft.setScreen(new BookScreen(loaded.get(), text.get(), icon.copyWithCount(1)));
        return true;
    }
}
