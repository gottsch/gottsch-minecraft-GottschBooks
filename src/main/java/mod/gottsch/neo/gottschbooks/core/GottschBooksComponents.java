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
package mod.gottsch.neo.gottschbooks.core;

import mod.gottsch.neo.gottschbooks.GottschBooks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The library's data components. Go through {@link mod.gottsch.neo.gottschbooks.api.Books} rather than these
 * holders, so the stack accessors stay one API across loaders.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GottschBooks.MOD_ID);

    /** which book a stack is: its id ({@code assets/<namespace>/books/<path>/}, and its {@code gottschbooks:book} entry) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> BOOK =
            DATA_COMPONENTS.registerComponentType("book", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    private GottschBooksComponents() {}

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
