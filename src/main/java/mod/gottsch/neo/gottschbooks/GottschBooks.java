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
package mod.gottsch.neo.gottschbooks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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

    public GottschBooks(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Gottsch Books loading");
    }
}
