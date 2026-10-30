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

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server config, {@code serverconfig/gottschbooks-server.toml} in each world.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    /** whether books marked {@code announce_on_first_join} are announced in chat; giving is never affected */
    public static final ModConfigSpec.BooleanValue ANNOUNCE_BOOKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ANNOUNCE_BOOKS = builder
                .comment("Tell each player once, in chat, about the books whose entry says \"announce_on_first_join\": true,",
                        "with a [Get it] link. Off silences every announcement; books given on first join are still given.")
                .define("announceBooks", true);
        SPEC = builder.build();
    }

    private ServerConfig() {}
}
