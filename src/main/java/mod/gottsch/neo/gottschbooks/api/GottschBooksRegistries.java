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

import mod.gottsch.neo.gottschbooks.GottschBooks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * The library's registry keys.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksRegistries {
    /** the books (see {@link BookDef}); a datapack registry, synced to clients */
    public static final ResourceKey<Registry<BookDef>> BOOK =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(GottschBooks.MOD_ID, "book"));

    private GottschBooksRegistries() {}

    /** a book tag, e.g. {@code magictreasures:books/selene} */
    public static TagKey<BookDef> bookTag(ResourceLocation id) {
        return TagKey.create(BOOK, id);
    }
}
