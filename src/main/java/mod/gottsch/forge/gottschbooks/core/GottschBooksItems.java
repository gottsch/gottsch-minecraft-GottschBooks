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
import mod.gottsch.forge.gottschbooks.api.BookItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The library's one item: {@code gottschbooks:book}, a generic book that opens whatever book its stack names
 * ({@code /give @p gottschbooks:book{"gottschbooks:book":"mymod:guide"}}), so a datapack or modpack can hand out any
 * book without code. It is named by {@code book.<namespace>.<path>} in a language file, or the stack's display name.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GottschBooks.MOD_ID);

    public static final RegistryObject<Item> BOOK = ITEMS.register("book", () -> new BookItem(new Item.Properties()));

    private GottschBooksItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
