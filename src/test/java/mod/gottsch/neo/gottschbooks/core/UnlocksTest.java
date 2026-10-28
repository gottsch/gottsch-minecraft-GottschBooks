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

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Unlocks}: which entries a set of newly done advancements unlocks.
 *
 * @author Mark Gottschling on 10/28/2026
 */
class UnlocksTest {
    private static final ResourceLocation GUIDE = ResourceLocation.parse("example:field_guide");
    private static final ResourceLocation OTHER = ResourceLocation.parse("example:other_manual");
    private static final ResourceLocation DIAMOND = ResourceLocation.parse("minecraft:story/mine_diamond");
    private static final ResourceLocation IRON = ResourceLocation.parse("minecraft:story/smelt_iron");
    private static final ResourceLocation NETHER = ResourceLocation.parse("minecraft:story/enter_the_nether");

    private static final List<Unlocks.Lock> LOCKS = List.of(
            new Unlocks.Lock(GUIDE, "diamonds", DIAMOND),
            new Unlocks.Lock(GUIDE, "iron", IRON),
            new Unlocks.Lock(GUIDE, "deep_mining", DIAMOND),
            new Unlocks.Lock(OTHER, "gems", DIAMOND),
            new Unlocks.Lock(OTHER, "nether", NETHER));

    @Test
    void newlyDoneIsWhatIsDoneNowAndWasNotBefore() {
        assertEquals(Set.of(DIAMOND), Unlocks.newlyDone(Set.of(IRON), Set.of(IRON, DIAMOND)));
        assertEquals(Set.of(), Unlocks.newlyDone(Set.of(IRON, DIAMOND), Set.of(IRON, DIAMOND)), "nothing changed");
        assertEquals(Set.of(), Unlocks.newlyDone(Set.of(IRON, DIAMOND), Set.of(IRON)), "a revoked one opens nothing");
        assertEquals(Set.of(NETHER), Unlocks.newlyDone(Set.of(IRON), Set.of(NETHER)));
    }

    @Test
    void nothingNewlyDoneUnlocksNothing() {
        assertTrue(Unlocks.unlocked(LOCKS, Set.of(), book -> true).isEmpty());
    }

    @Test
    void anAdvancementUnlocksEveryEntryLockedBehindIt() {
        Map<ResourceLocation, List<String>> unlocked = Unlocks.unlocked(LOCKS, Set.of(DIAMOND), book -> true);
        assertEquals(List.of(GUIDE, OTHER), List.copyOf(unlocked.keySet()), "books in the order of the locks");
        assertEquals(List.of("diamonds", "deep_mining"), unlocked.get(GUIDE), "entries in contents order");
        assertEquals(List.of("gems"), unlocked.get(OTHER));
    }

    @Test
    void severalAdvancementsAtOnceGroupByBook() {
        Map<ResourceLocation, List<String>> unlocked = Unlocks.unlocked(LOCKS, Set.of(IRON, NETHER), book -> true);
        assertEquals(Map.of(GUIDE, List.of("iron"), OTHER, List.of("nether")), unlocked);
    }

    @Test
    void booksThePlayerCannotReadAreLeftOut() {
        Map<ResourceLocation, List<String>> unlocked = Unlocks.unlocked(LOCKS, Set.of(DIAMOND), GUIDE::equals);
        assertEquals(Map.of(GUIDE, List.of("diamonds", "deep_mining")), unlocked);
        assertTrue(Unlocks.unlocked(LOCKS, Set.of(DIAMOND), book -> false).isEmpty());
    }

    @Test
    void anAdvancementNoEntryNeedsUnlocksNothing() {
        assertTrue(Unlocks.unlocked(LOCKS, Set.of(ResourceLocation.parse("minecraft:story/root")), book -> true).isEmpty());
    }

    @Test
    void anEntryListedTwiceIsUnlockedOnce() {
        List<Unlocks.Lock> twice = List.of(new Unlocks.Lock(GUIDE, "diamonds", DIAMOND), new Unlocks.Lock(GUIDE, "diamonds", DIAMOND));
        assertEquals(Map.of(GUIDE, List.of("diamonds")), Unlocks.unlocked(twice, Set.of(DIAMOND), book -> true));
    }
}
