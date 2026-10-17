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
package mod.gottsch.forge.gottschbooks.core.network;

import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.client.ClientPayloads;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The library's network channel ({@code gottschbooks:main}).
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GottschBooks.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private GottschBooksNetwork() {}

    /** call from the mod constructor */
    public static void register() {
        // the handler is a lambda, not a method reference, so a dedicated server never loads the client class
        CHANNEL.messageBuilder(OpenBookPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenBookPayload::encode)
                .decoder(OpenBookPayload::decode)
                .consumerMainThread((payload, context) -> ClientPayloads.open(payload))
                .add();
    }

    public static void send(ServerPlayer player, OpenBookPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
