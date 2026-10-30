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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Unlock rules that need no client: which advancements a progress update newly finished, and which locked manual
 * entries those open, grouped by book, for the "new in a manual" toast.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class Unlocks {

    /** manual {@code book}'s {@code entry}, hidden until the player has {@code advancement} */
    public record Lock(ResourceLocation book, String entry, ResourceLocation advancement) {}

    private Unlocks() {}

    /** the advancements done {@code now} that weren't {@code before}; one revoked since counts for nothing */
    public static Set<ResourceLocation> newlyDone(Set<ResourceLocation> before, Set<ResourceLocation> now) {
        Set<ResourceLocation> newly = new HashSet<>(now);
        newly.removeAll(before);
        return newly;
    }

    /**
     * The entries of {@code locks} that {@code newlyDone} opens, in books the player can {@code read}: book to entries,
     * both in the order of {@code locks}.
     */
    public static Map<ResourceLocation, List<String>> unlocked(List<Lock> locks, Set<ResourceLocation> newlyDone,
                                                               Predicate<ResourceLocation> read) {
        Map<ResourceLocation, List<String>> unlocked = new LinkedHashMap<>();
        if (newlyDone.isEmpty()) {
            return unlocked;
        }
        for (Lock lock : locks) {
            if (newlyDone.contains(lock.advancement()) && read.test(lock.book())) {
                List<String> entries = unlocked.computeIfAbsent(lock.book(), k -> new ArrayList<>());
                if (!entries.contains(lock.entry())) {
                    entries.add(lock.entry());
                }
            }
        }
        return unlocked;
    }
}
