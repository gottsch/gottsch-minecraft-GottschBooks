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
package mod.gottsch.neo.gottschbooks.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EntityArgument}.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class EntityArgumentTest {

    @Test
    void idAlone() {
        EntityArgument entity = EntityArgument.parse(" minecraft:enderman ");
        assertEquals("minecraft:enderman", entity.id());
        assertEquals(1F, entity.scale());
        assertTrue(entity.problem().isEmpty());
    }

    @Test
    void idAndScale() {
        EntityArgument entity = EntityArgument.parse("mymod:golem   0.7");
        assertEquals("mymod:golem", entity.id());
        assertEquals(0.7F, entity.scale());
        assertTrue(entity.problem().isEmpty());
        assertEquals(1F, EntityArgument.parse("mymod:golem 1").scale());
    }

    @Test
    void badScales() {
        for (String scale : new String[] {"0", "1.5", "-0.5", "big", "NaN"}) {
            EntityArgument entity = EntityArgument.parse("mymod:golem " + scale);
            assertEquals("mymod:golem", entity.id());
            assertEquals(1F, entity.scale(), scale);
            assertTrue(entity.problem().isPresent(), scale);
        }
        assertTrue(EntityArgument.parse("mymod:golem 0.5 0.5").problem().isPresent());
    }
}
