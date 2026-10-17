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

import mod.gottsch.neo.gottschbooks.api.Illustrations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Turns an item reference in a book ({@code icon:} front matter, a spotlight) into a stack: {@code ns:id} is an item,
 * {@code scheme:ns:id} goes through an illustration provider ({@code item:}, or a mod's own, such as Magic
 * Treasures' {@code preset:}).
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class Icons {

    private Icons() {}

    public static Optional<ItemStack> stack(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        String ref = reference.strip();
        int first = ref.indexOf(':');
        int second = first < 0 ? -1 : ref.indexOf(':', first + 1);
        if (second > 0) {
            ResourceLocation id = ResourceLocation.tryParse(ref.substring(first + 1));
            return id == null ? Optional.empty() : Illustrations.get(ref.substring(0, first), id);
        }
        ResourceLocation id = ResourceLocation.tryParse(ref);
        return id == null ? Optional.empty() : Illustrations.get(Illustrations.ITEM, id);
    }
}
