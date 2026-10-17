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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A book the server knows about: an entry in the {@code gottschbooks:book} datapack registry, at
 * {@code data/<namespace>/gottschbooks/book/<book>.json}. The words and art are client assets
 * ({@code assets/<namespace>/books/<book>/}); this entry only exists so the server can count books for the
 * {@code gottschbooks:book_read} advancement trigger and so books can be grouped by tag
 * ({@code data/<namespace>/tags/gottschbooks/book/<tag>.json}). Every field is optional: {@code {}} is a whole entry.
 *
 * @param disabled a pack can switch a book off; it then no longer counts toward "every book" conditions
 *
 * @author Mark Gottschling on 10/17/2026
 */
public record BookDef(boolean disabled) {

    public static final Codec<BookDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("disabled", false).forGetter(BookDef::disabled)
    ).apply(instance, BookDef::new));
}
