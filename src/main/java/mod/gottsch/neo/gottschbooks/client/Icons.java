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
package mod.gottsch.neo.gottschbooks.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mod.gottsch.neo.gottschbooks.GottschBooks;
import mod.gottsch.neo.gottschbooks.api.Illustrations;
import mod.gottsch.neo.gottschbooks.core.text.ItemReference;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns an item reference in a book ({@code icon:} front matter, a spotlight, an illustration) into a stack:
 * {@code ns:id} is an item, {@code scheme:ns:id} goes through an illustration provider ({@code item:}, or a mod's
 * own, such as Magic Treasures' {@code preset:}). An item may carry {@code /give}-style components, parsed against
 * the world's registries.
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
        if (ref.components().isEmpty()) {
            return Illustrations.get(ref.scheme(), ref.id());
        }
        if (!ref.scheme().equals(Illustrations.ITEM)) {
            warn(ref, "components only work on item references; drawing it without them");
            return Illustrations.get(ref.scheme(), ref.id());
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            // enchantments and the like are world registries; without a world, the plain item will do
            return Illustrations.get(Illustrations.ITEM, ref.id());
        }
        StringReader reader = new StringReader(ref.id() + ref.components());
        try {
            ItemParser.ItemResult result = new ItemParser(level.registryAccess()).parse(reader);
            if (reader.canRead()) {
                warn(ref, "unexpected text after the components: '" + reader.getRemaining() + "'");
                return Optional.empty();
            }
            return Optional.of(new ItemStack(result.item(), 1, result.components())).filter(stack -> !stack.isEmpty());
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
