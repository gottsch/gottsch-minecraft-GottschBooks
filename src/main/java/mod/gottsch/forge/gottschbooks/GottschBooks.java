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
package mod.gottsch.forge.gottschbooks;

import mod.gottsch.forge.gottschbooks.api.BookDef;
import mod.gottsch.forge.gottschbooks.api.GottschBooksRegistries;
import mod.gottsch.forge.gottschbooks.core.GottschBooksItems;
import mod.gottsch.forge.gottschbooks.core.advancement.GottschBooksCriteria;
import mod.gottsch.forge.gottschbooks.core.network.GottschBooksNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gottsch Books: an in-game book library (lore books and manuals written in Markdown) that other mods
 * nest by Jar-in-Jar or declare as an optional dependency.
 *
 * @author Mark Gottschling on 10/17/2026
 */
@Mod(GottschBooks.MOD_ID)
public class GottschBooks {
    public static final String MOD_ID = "gottschbooks";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public GottschBooks() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        GottschBooksCriteria.init();
        GottschBooksItems.register(modEventBus);
        GottschBooksNetwork.register();
        modEventBus.addListener(GottschBooks::onNewDataPackRegistries);
    }

    private static void onNewDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // synced, so clients have the entries (and tags) too
        event.dataPackRegistry(GottschBooksRegistries.BOOK, BookDef.CODEC, BookDef.CODEC);
    }
}
