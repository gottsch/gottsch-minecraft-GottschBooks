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
package mod.gottsch.forge.gottschbooks.core.text;

import java.util.Optional;

/**
 * An {@code entity} block's argument: the mob's id, then optionally a scale from just above 0 to 1
 * ({@code ```entity mymod:golem 0.7}). The reader fits the mob's model to the line; a scale draws it smaller, feet
 * kept on the line's floor.
 *
 * @param id the entity id as written
 * @param scale the scale, 1 when none is written or it is bad
 * @param problem why the scale is bad, if it is
 *
 * @author Mark Gottschling on 10/28/2026
 */
public record EntityArgument(String id, float scale, Optional<String> problem) {

    public static EntityArgument parse(String argument) {
        String[] parts = argument.strip().split("\\s+", 3);
        if (parts.length < 2) {
            return new EntityArgument(parts[0], 1F, Optional.empty());
        }
        if (parts.length > 2) {
            return new EntityArgument(parts[0], 1F, Optional.of("mob '" + parts[0] + "' takes one scale, not '"
                    + argument.strip().substring(parts[0].length()).strip() + "'"));
        }
        try {
            float scale = Float.parseFloat(parts[1]);
            if (scale > 0F && scale <= 1F) {
                return new EntityArgument(parts[0], scale, Optional.empty());
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        return new EntityArgument(parts[0], 1F, Optional.of("mob '" + parts[0] + "': scale '" + parts[1]
                + "' isn't a number above 0 and at most 1, such as 0.7"));
    }
}
