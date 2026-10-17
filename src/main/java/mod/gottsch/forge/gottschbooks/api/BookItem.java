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

import mod.gottsch.forge.gottschbooks.api.client.GottschBooksClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * A base for a mod's book item, for mods that require Gottsch Books (an optional-manual mod uses a plain
 * {@link Item} instead; see the README). Which book a stack is comes from the stack.s {@code gottschbooks:book}
 * tag, or {@link #bookId(ItemStack)} for an item that is always the same book. Right-click opens the reader on
 * the client and, on the server, records the read (for the {@code gottschbooks:book_read} trigger) when the book has
 * a {@code gottschbooks:book} entry.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public class BookItem extends Item {

    public BookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** which book {@code stack} is; by default its {@code gottschbooks:book} tag */
    public Optional<ResourceLocation> bookId(ItemStack stack) {
        return Books.getId(stack);
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
        } else if (player instanceof ServerPlayer serverPlayer && Books.getDef(id.get(), level.registryAccess()).isPresent()) {
            Books.markRead(serverPlayer, id.get());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
