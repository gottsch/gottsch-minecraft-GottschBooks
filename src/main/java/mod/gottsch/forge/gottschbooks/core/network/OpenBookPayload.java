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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: open {@code book}, at {@code entry} when it isn't empty (a manual's entry, {@code "gems#ruby"}).
 * Sent by {@code /gottschbooks open}.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public record OpenBookPayload(ResourceLocation book, String entry) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(book);
        buf.writeUtf(entry);
    }

    public static OpenBookPayload decode(FriendlyByteBuf buf) {
        return new OpenBookPayload(buf.readResourceLocation(), buf.readUtf());
    }
}
