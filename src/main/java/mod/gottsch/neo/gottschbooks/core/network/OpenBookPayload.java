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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: open {@code book}, at {@code entry} when it isn't empty (a manual's entry, {@code "gems#ruby"}).
 * Sent by {@code /gottschbooks open}.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public record OpenBookPayload(ResourceLocation book, String entry) implements CustomPacketPayload {
    public static final Type<OpenBookPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GottschBooks.MOD_ID, "open_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBookPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, OpenBookPayload::book,
            ByteBufCodecs.STRING_UTF8, OpenBookPayload::entry,
            OpenBookPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
