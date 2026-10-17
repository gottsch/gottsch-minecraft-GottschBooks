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
package mod.gottsch.neo.gottschbooks.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Turns the id in an illustration ({@code ![caption](<scheme>:<namespace>:<path>)}) into the stack to draw.
 * Registered with {@link Illustrations#register}.
 *
 * @author Mark Gottschling on 10/17/2026
 */
@FunctionalInterface
public interface IllustrationProvider {

    /**
     * @param id the id after the scheme ({@code magictreasures:ring_of_life_death} in
     *           {@code preset:magictreasures:ring_of_life_death})
     * @return the stack to draw, or empty when there is no such thing (the reader then leaves the picture out)
     */
    Optional<ItemStack> get(ResourceLocation id);
}
