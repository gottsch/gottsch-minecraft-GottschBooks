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
package mod.gottsch.neo.gottschbooks.core.advancement;

import mod.gottsch.neo.gottschbooks.GottschBooks;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The library's advancement triggers.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksCriteria {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, GottschBooks.MOD_ID);

    /** a book read; checks what the player has read so far (see {@link BookReadTrigger}) */
    public static final DeferredHolder<CriterionTrigger<?>, BookReadTrigger> BOOK_READ =
            TRIGGERS.register("book_read", BookReadTrigger::new);

    private GottschBooksCriteria() {}

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }
}
