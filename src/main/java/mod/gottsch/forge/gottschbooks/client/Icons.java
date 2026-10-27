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

import javax.annotation.Nullable;
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
        Resolved resolved = resolve(ref);
        if (resolved.why() != null) {
            warn(ref, resolved.why());
        }
        return resolved.stack();
    }

    /** what is wrong with {@code reference} as written, for {@code /gottschbooks check}; empty when nothing is */
    public static Optional<String> problem(String reference) {
        Optional<ItemReference> ref = ItemReference.parse(reference);
        if (ref.isEmpty()) {
            return Optional.of("'" + reference + "' isn't an item or a picture (ns:id or scheme:ns:id)");
        }
        Resolved resolved = resolve(ref.get());
        if (resolved.why() != null) {
            return Optional.of("item '" + ref.get().text() + "': " + resolved.why());
        }
        return resolved.stack().isPresent() ? Optional.empty()
                : Optional.of("no item or picture '" + ref.get().text() + "'");
    }

    /** the stack, if any, and why it isn't drawn as written; a missing item alone has no reason */
    private record Resolved(Optional<ItemStack> stack, @Nullable String why) {}

    private static Resolved resolve(ItemReference ref) {
        if (ref.nbt().isEmpty()) {
            return new Resolved(Illustrations.get(ref.scheme(), ref.id()), null);
        }
        if (!ref.scheme().equals(Illustrations.ITEM)) {
            return new Resolved(Illustrations.get(ref.scheme(), ref.id()), "NBT only works on item references; drawing it without");
        }
        StringReader reader = new StringReader(ref.id() + ref.nbt());
        try {
            ItemParser.ItemResult result = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), reader);
            if (reader.canRead()) {
                return new Resolved(Optional.empty(), "unexpected text after the NBT: '" + reader.getRemaining() + "'");
            }
            ItemStack stack = new ItemStack(result.item().value());
            if (result.nbt() != null) {
                stack.setTag(result.nbt());
            }
            return new Resolved(Optional.of(stack).filter(s -> !s.isEmpty()), null);
        } catch (CommandSyntaxException e) {
            return new Resolved(Optional.empty(), e.getMessage());
        }
    }

    private static void warn(ItemReference ref, String why) {
        if (WARNED.add(ref.text())) {
            GottschBooks.LOGGER.warn("item '{}': {}", ref.text(), why);
        }
    }
}
