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
package mod.gottsch.forge.gottschbooks.client;

import mod.gottsch.forge.gottschbooks.core.text.Block;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import mod.gottsch.forge.gottschbooks.core.text.Inline;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A manual's table of contents, in one language. The files, under {@code assets/<ns>/books/<book>/<locale>/}:
 * <ul>
 *     <li>{@code _index.md}: {@code title} (the manual's name) and the landing text shown above the contents;</li>
 *     <li>{@code categories/<id>.md}: {@code title}, {@code icon}, {@code sort}, and a short description;</li>
 *     <li>{@code <entry>.md} (any other path): {@code title}, {@code category} (a category id), {@code icon},
 *     {@code sort}, and optionally {@code advancement}, which hides the entry until the player has it.</li>
 * </ul>
 * Each file falls back to {@code en_us} on its own, so a translation can cover only some of them. Categories and
 * entries are ordered by {@code sort}, then title.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class Manual {
    public static final String INDEX = "_index";
    public static final String CATEGORIES = "categories/";

    public record Category(String id, Component title, String icon, List<Block> description) {}

    public record Entry(String id, Component title, String category, String icon, Optional<ResourceLocation> advancement,
                        BookText text) {
        public boolean unlocked() {
            return advancement.map(Locks::has).orElse(true);
        }
    }

    private final Component title;
    private final List<Block> landing;
    private final List<Category> categories;
    private final Map<String, Entry> entries;

    private Manual(Component title, List<Block> landing, List<Category> categories, Map<String, Entry> entries) {
        this.title = title;
        this.landing = landing;
        this.categories = categories;
        this.entries = entries;
    }

    public static Manual of(LoadedBook book, String locale) {
        Optional<BookText> index = book.entry(INDEX, locale);
        Component title = index.flatMap(text -> text.get("title")).<Component>map(Inline::parse)
                .orElseGet(() -> Component.literal(book.id().toString()));
        List<Block> landing = index.map(BookText::blocks).orElse(List.of());

        List<Sortable<Category>> categories = new ArrayList<>();
        List<Sortable<Entry>> entries = new ArrayList<>();
        for (String name : book.entries().keySet()) {
            if (name.equals(INDEX)) {
                continue;
            }
            BookText text = book.entry(name, locale).orElseThrow();
            Component entryTitle = text.get("title").<Component>map(Inline::parse)
                    .orElseGet(() -> Component.literal(name));
            int sort = text.get("sort").map(Manual::number).orElse(Integer.MAX_VALUE);
            if (name.startsWith(CATEGORIES)) {
                String id = name.substring(CATEGORIES.length());
                categories.add(new Sortable<>(sort, entryTitle.getString(),
                        new Category(id, entryTitle, text.get("icon").orElse(""), text.blocks())));
            } else {
                entries.add(new Sortable<>(sort, entryTitle.getString(), new Entry(name, entryTitle,
                        text.get("category").orElse(""), text.get("icon").orElse(""),
                        text.get("advancement").map(ResourceLocation::tryParse), text)));
            }
        }
        Map<String, Entry> byId = new LinkedHashMap<>();
        sorted(entries).forEach(entry -> byId.put(entry.id(), entry));
        return new Manual(title, landing, sorted(categories), byId);
    }

    public Component title() {
        return title;
    }

    public List<Block> landing() {
        return landing;
    }

    public List<Category> categories() {
        return categories;
    }

    /** an entry the player can read: it exists and isn't locked */
    public Optional<Entry> entry(String id) {
        return Optional.ofNullable(entries.get(id)).filter(Entry::unlocked);
    }

    /** the readable entries of {@code category} ("" for those in none, or in a category that doesn't exist) */
    public List<Entry> entriesIn(String category) {
        List<Entry> in = new ArrayList<>();
        for (Entry entry : entries.values()) {
            boolean known = categories.stream().anyMatch(c -> c.id().equals(entry.category()));
            String of = known ? entry.category() : "";
            if (of.equals(category) && entry.unlocked()) {
                in.add(entry);
            }
        }
        return in;
    }

    private record Sortable<T>(int sort, String title, T value) {}

    private static <T> List<T> sorted(List<Sortable<T>> list) {
        return list.stream().sorted(Comparator.<Sortable<T>>comparingInt(Sortable::sort).thenComparing(Sortable::title))
                .map(Sortable::value).toList();
    }

    private static int number(String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
