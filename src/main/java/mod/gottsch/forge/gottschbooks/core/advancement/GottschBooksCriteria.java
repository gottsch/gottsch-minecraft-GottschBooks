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
package mod.gottsch.forge.gottschbooks.core.advancement;

import net.minecraft.advancements.CriteriaTriggers;

/**
 * The library's advancement triggers.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksCriteria {
    /** a book read; checks what the player has read so far (see {@link BookReadTrigger}) */
    public static final BookReadTrigger BOOK_READ = CriteriaTriggers.register(new BookReadTrigger());

    private GottschBooksCriteria() {}

    /** loads the class, registering the triggers; call from the mod constructor */
    public static void init() {
    }
}
