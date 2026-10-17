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
package mod.gottsch.neo.gottschbooks.client;

import mod.gottsch.neo.gottschbooks.api.client.GottschBooksClient;
import mod.gottsch.neo.gottschbooks.core.network.OpenBookPayload;

/**
 * Client handlers for the library's payloads (they run on the main thread).
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class ClientPayloads {

    private ClientPayloads() {}

    public static void open(OpenBookPayload payload) {
        if (payload.entry().isEmpty()) {
            GottschBooksClient.open(payload.book());
        } else {
            GottschBooksClient.open(payload.book(), payload.entry());
        }
    }
}
