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
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * A book the server knows about: an entry in the {@code gottschbooks:book} datapack registry, at
 * {@code data/<namespace>/gottschbooks/book/<book>.json}. The words and art are client assets
 * ({@code assets/<namespace>/books/<book>/}); this entry is what the server needs: read tracking for the
 * {@code gottschbooks:book_read} advancement trigger, book tags ({@code data/<namespace>/tags/gottschbooks/book/}),
 * and giving the book to new players. Every field is optional: {@code {}} is a whole entry.
 *
 * @param disabled        a pack can switch a book off: it is no longer tracked, counted or given
 * @param trackReading    reading it is recorded and fires {@code book_read}, and it counts toward "every book"
 *                        conditions; set false for a manual, so it doesn't count with the lore books
 * @param giveOnFirstJoin each player gets the book once, the first time they join after the entry exists
 * @param item            the item given: a {@code BookItem}, default {@code gottschbooks:book}
 *
 * @author Mark Gottschling on 10/17/2026
 */
public record BookDef(boolean disabled, boolean trackReading, boolean giveOnFirstJoin, Optional<ResourceLocation> item) {

    public static final Codec<BookDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("disabled", false).forGetter(BookDef::disabled),
            Codec.BOOL.optionalFieldOf("track_reading", true).forGetter(BookDef::trackReading),
            Codec.BOOL.optionalFieldOf("give_on_first_join", false).forGetter(BookDef::giveOnFirstJoin),
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(BookDef::item)
    ).apply(instance, BookDef::new));

    /** whether reading it is recorded and counted */
    public boolean tracked() {
        return !disabled && trackReading;
    }
}
