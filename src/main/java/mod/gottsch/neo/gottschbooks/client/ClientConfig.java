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

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The client config, {@code config/gottschbooks-client.toml}. It touches no client classes, so the mod can register
 * it on either side.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    /** whether item tooltips get the item lookup line; the lookup key works either way */
    public static final ModConfigSpec.BooleanValue LOOKUP_HINT;
    /** whether the item lookup line goes away for an item once its entry has been opened */
    public static final ModConfigSpec.BooleanValue LOOKUP_HINT_UNTIL_READ;
    /** whether earning an advancement that unlocks manual entries shows a toast */
    public static final ModConfigSpec.BooleanValue UNLOCK_TOASTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        LOOKUP_HINT = builder
                .comment("Show the item lookup line (\"[B] Manual\") in item tooltips.",
                        "The lookup key still opens the manual when this is off. Also set with /gottschbooks lookup hint on|off.")
                .define("lookupHint", true);
        LOOKUP_HINT_UNTIL_READ = builder
                .comment("Stop showing the item lookup line for an item once you have opened the manual entry that explains it.",
                        "The lookup key still opens the entry.")
                .define("lookupHintUntilRead", true);
        UNLOCK_TOASTS = builder
                .comment("Show a toast when an advancement you earn unlocks entries in a manual you carry.",
                        "Pressing the lookup key while it shows opens the manual there.")
                .define("unlockToasts", true);
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
