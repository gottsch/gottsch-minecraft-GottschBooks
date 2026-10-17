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
package mod.gottsch.neo.gottschbooks.core.network;

import mod.gottsch.neo.gottschbooks.GottschBooks;
import mod.gottsch.neo.gottschbooks.client.ClientPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The library's payloads.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class GottschBooksNetwork {
    private static final String VERSION = "1";

    private GottschBooksNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(GottschBooksNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // a lambda, not a method reference, so a dedicated server never loads the client class
        event.registrar(GottschBooks.MOD_ID).versioned(VERSION)
                .playToClient(OpenBookPayload.TYPE, OpenBookPayload.STREAM_CODEC, (payload, context) -> ClientPayloads.open(payload));
    }
}
