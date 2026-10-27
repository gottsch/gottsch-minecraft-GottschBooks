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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.api.Illustrations;
import mod.gottsch.forge.gottschbooks.core.text.ItemReference;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns an item reference in a book ({@code icon:} front matter, a spotlight, an illustration) into a stack:
 * {@code ns:id} is an item, {@code scheme:ns:id} goes through an illustration provider ({@code item:}, or a mod's
 * own, such as Magic Treasures' {@code preset:}). An item may carry {@code /give}-style NBT.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class Icons {
    /** references already warned about, so a resize (which lays the book out again) doesn't repeat the warning */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private Icons() {}

    public static Optional<ItemStack> stack(String reference) {
        return ItemReference.parse(reference).flatMap(Icons::stack);
    }

    public static Optional<ItemStack> stack(ItemReference ref) {
        if (ref.nbt().isEmpty()) {
            return Illustrations.get(ref.scheme(), ref.id());
        }
        if (!ref.scheme().equals(Illustrations.ITEM)) {
            warn(ref, "NBT only works on item references; drawing it without");
            return Illustrations.get(ref.scheme(), ref.id());
        }
        StringReader reader = new StringReader(ref.id() + ref.nbt());
        try {
            ItemParser.ItemResult result = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), reader);
            if (reader.canRead()) {
                warn(ref, "unexpected text after the NBT: '" + reader.getRemaining() + "'");
                return Optional.empty();
            }
            ItemStack stack = new ItemStack(result.item().value());
            if (result.nbt() != null) {
                stack.setTag(result.nbt());
            }
            return Optional.of(stack).filter(s -> !s.isEmpty());
        } catch (CommandSyntaxException e) {
            warn(ref, e.getMessage());
            return Optional.empty();
        }
    }

    private static void warn(ItemReference ref, String why) {
        if (WARNED.add(ref.text())) {
            GottschBooks.LOGGER.warn("item '{}': {}", ref.text(), why);
        }
    }
}
