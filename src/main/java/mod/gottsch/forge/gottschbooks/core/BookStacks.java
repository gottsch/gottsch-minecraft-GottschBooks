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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Which book a stack is, as stack NBT: the string {@code "gottschbooks:book"}, named after the NeoForge build's data
 * component. Go through {@link mod.gottsch.forge.gottschbooks.api.Books}, which is the same API on both loaders.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class BookStacks {
    public static final String KEY = GottschBooks.MOD_ID + ":book";

    private BookStacks() {}

    public static Optional<ResourceLocation> get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(tag.getString(KEY)));
    }

    public static void set(ItemStack stack, ResourceLocation id) {
        stack.getOrCreateTag().putString(KEY, id.toString());
    }
}
