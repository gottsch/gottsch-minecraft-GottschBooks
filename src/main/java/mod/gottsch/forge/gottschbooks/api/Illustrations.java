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
package mod.gottsch.forge.gottschbooks.api;

import mod.gottsch.forge.gottschbooks.GottschBooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The illustration schemes a book can use in {@code ![caption](<scheme>:<namespace>:<path>)}. The library
 * provides {@code item} (the item's default stack); a mod adds its own, e.g. Magic Treasures' {@code preset} for
 * named jewelry pieces. Register from the mod constructor or common setup; registering is thread-safe.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class Illustrations {
    public static final String ITEM = "item";

    private static final Pattern SCHEME = Pattern.compile("[a-z0-9_]+");
    private static final Map<String, IllustrationProvider> PROVIDERS = new ConcurrentHashMap<>();

    static {
        PROVIDERS.put(ITEM, id -> BuiltInRegistries.ITEM.getOptional(id).map(Item::getDefaultInstance));
    }

    private Illustrations() {}

    /**
     * Adds a scheme.
     *
     * @throws IllegalArgumentException if {@code scheme} isn't lowercase letters, digits and underscores
     * @throws IllegalStateException    if another provider already has {@code scheme}
     */
    public static void register(String scheme, IllustrationProvider provider) {
        if (!SCHEME.matcher(scheme).matches()) {
            throw new IllegalArgumentException("illustration scheme must match [a-z0-9_]+: " + scheme);
        }
        if (PROVIDERS.putIfAbsent(scheme, provider) != null) {
            throw new IllegalStateException("illustration scheme already registered: " + scheme);
        }
    }

    public static boolean has(String scheme) {
        return PROVIDERS.containsKey(scheme);
    }

    /** the stack for {@code scheme:id}; empty when the scheme is unknown, or its provider has nothing or fails */
    public static Optional<ItemStack> get(String scheme, ResourceLocation id) {
        IllustrationProvider provider = PROVIDERS.get(scheme);
        if (provider == null) {
            return Optional.empty();
        }
        try {
            return provider.get(id).filter(stack -> !stack.isEmpty());
        } catch (RuntimeException e) {
            // another mod's provider must not take the reader down with it
            GottschBooks.LOGGER.warn("illustration provider '{}' failed for {}", scheme, id, e);
            return Optional.empty();
        }
    }
}
