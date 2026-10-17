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
package mod.gottsch.neo.gottschbooks.api;

import mod.gottsch.neo.gottschbooks.api.client.GottschBooksClient;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A base for a mod's book item, for mods that require Gottsch Books (an optional-manual mod uses a plain
 * {@link Item} instead; see the README). Which book a stack is comes from the stack's {@code gottschbooks:book}
 * component, or, for an item that is always the same book (a mod's manual), the book given to the constructor.
 * Right-click opens the reader on the client and, on the server, records the read (for the
 * {@code gottschbooks:book_read} trigger) when the book has a {@code gottschbooks:book} entry.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public class BookItem extends Item {
    @Nullable
    private final ResourceLocation book;

    /** an item whose stacks say which book they are (the {@code gottschbooks:book} component) */
    public BookItem(Properties properties) {
        this(properties, null);
    }

    /** an item that is always {@code book}, unless a stack's component says otherwise */
    public BookItem(Properties properties, @Nullable ResourceLocation book) {
        super(properties.stacksTo(1));
        this.book = book;
    }

    /** which book {@code stack} is: its {@code gottschbooks:book} component, else the item's own book */
    public Optional<ResourceLocation> bookId(ItemStack stack) {
        return Books.getId(stack).or(() -> Optional.ofNullable(book));
    }

    /**
     * The book's name, when a language file has {@code book.<namespace>.<path>} ({@code /} in the path becoming
     * {@code .}); else the item's own name. So a resource pack names a book with a lang line, no code. A stack's
     * {@code item_name} or {@code custom_name} component still wins, as for any item.
     */
    @Override
    public Component getName(ItemStack stack) {
        Optional<ResourceLocation> id = bookId(stack);
        if (id.isPresent()) {
            String key = nameKey(id.get());
            // the server's language only has vanilla's English, so this names books on clients (where names show)
            if (Language.getInstance().has(key)) {
                return Component.translatable(key);
            }
        }
        return super.getName(stack);
    }

    /** {@code book.<namespace>.<path>}: the lang key that names book {@code id} */
    public static String nameKey(ResourceLocation id) {
        return "book." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<ResourceLocation> id = bookId(stack);
        if (id.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            // only the client has a screen; this branch never runs on a dedicated server, so the class never loads there
            GottschBooksClient.open(id.get(), stack);
        } else if (player instanceof ServerPlayer serverPlayer && Books.getDef(id.get(), level.registryAccess()).filter(BookDef::tracked).isPresent()) {
            Books.markRead(serverPlayer, id.get());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
