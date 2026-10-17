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
package mod.gottsch.forge.gottschbooks.client;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;

/**
 * Entry locks: an entry with {@code advancement: ns:id} stays hidden until the player has that advancement. Reads the
 * client's advancement progress, which the access transformer opens up ({@code ClientAdvancements.progress},
 * {@code f_104390_}).
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class Locks {

    private Locks() {}

    public static boolean has(ResourceLocation advancement) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        ClientAdvancements advancements = connection.getAdvancements();
        Advancement holder = advancements.getAdvancements().get(advancement);
        if (holder == null) {
            return false;
        }
        AdvancementProgress progress = advancements.progress.get(holder);
        return progress != null && progress.isDone();
    }
}
