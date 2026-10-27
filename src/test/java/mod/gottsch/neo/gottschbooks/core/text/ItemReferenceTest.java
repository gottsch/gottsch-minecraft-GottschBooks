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

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ItemReference}.
 *
 * @author Mark Gottschling on 10/26/2026
 */
class ItemReferenceTest {

    private static ItemReference ref(String scheme, String id, String components) {
        return new ItemReference(scheme, ResourceLocation.parse(id), components);
    }

    @Test
    void bareIdIsAnItem() {
        assertEquals(Optional.of(ref("item", "minecraft:diamond", "")), ItemReference.parse("minecraft:diamond"));
        assertEquals(Optional.of(ref("item", "minecraft:diamond", "")), ItemReference.parse("  minecraft:diamond "));
    }

    @Test
    void scheme() {
        assertEquals(Optional.of(ref("item", "minecraft:diamond", "")), ItemReference.parse("item:minecraft:diamond"));
        assertEquals(Optional.of(ref("preset", "magictreasures:aqua_ring", "")),
                ItemReference.parse("preset:magictreasures:aqua_ring"));
    }

    @Test
    void componentColonsDontMakeAScheme() {
        String potion = "[potion_contents={potion:\"minecraft:healing\"}]";
        assertEquals(Optional.of(ref("item", "minecraft:potion", potion)), ItemReference.parse("minecraft:potion" + potion));
        assertEquals(Optional.of(ref("item", "minecraft:potion", potion)),
                ItemReference.parse("item:minecraft:potion" + potion));
    }

    @Test
    void componentsKeepTheirText() {
        String head = "[profile={name:\"gottsch\"},custom_name='\"Mark (the author) ]\"']";
        assertEquals(Optional.of(ref("item", "minecraft:player_head", head)),
                ItemReference.parse("minecraft:player_head" + head));
    }

    @Test
    void schemeWithComponents() {
        assertEquals(Optional.of(ref("preset", "magictreasures:aqua_ring", "[dyed_color=1]")),
                ItemReference.parse("preset:magictreasures:aqua_ring[dyed_color=1]"));
    }

    @Test
    void invalid() {
        assertTrue(ItemReference.parse(null).isEmpty());
        assertTrue(ItemReference.parse("   ").isEmpty());
        assertTrue(ItemReference.parse("Not An Id").isEmpty());
    }

    @Test
    void text() {
        assertEquals("item:minecraft:diamond", ItemReference.parse("minecraft:diamond").orElseThrow().text());
        assertEquals("item:minecraft:potion[x=1]", ItemReference.parse("minecraft:potion[x=1]").orElseThrow().text());
    }
}
