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
package mod.gottsch.forge.gottschbooks.core.advancement;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.api.BookDef;
import mod.gottsch.forge.gottschbooks.api.GottschBooksRegistries;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code gottschbooks:book_read}: fires each time a player reads a book, and checks what they've read so far.
 * Conditions (all optional; with none, any book read matches):
 * <ul>
 *     <li>{@code book}: they've read this book;</li>
 *     <li>{@code books}: they've read every book in this tag ({@code "#magictreasures:books/selene"});</li>
 *     <li>{@code all_in}: they've read every book in this namespace ({@code "magictreasures"}).</li>
 * </ul>
 * "Every" counts the enabled entries of the {@code gottschbooks:book} registry, so a pack's books count too, and is
 * never true of no books, so an empty tag can't grant an advancement.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public class BookReadTrigger extends SimpleCriterionTrigger<BookReadTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation(GottschBooks.MOD_ID, "book_read");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        Optional<TagKey<BookDef>> books = Optional.empty();
        if (json.has("books")) {
            String tag = GsonHelper.getAsString(json, "books");
            if (!tag.startsWith("#")) {
                throw new JsonSyntaxException("\"books\" must be a tag, starting with #: " + tag);
            }
            books = Optional.of(GottschBooksRegistries.bookTag(new ResourceLocation(tag.substring(1))));
        }
        return new TriggerInstance(player,
                json.has("book") ? Optional.of(new ResourceLocation(GsonHelper.getAsString(json, "book"))) : Optional.empty(),
                books,
                json.has("all_in") ? Optional.of(GsonHelper.getAsString(json, "all_in")) : Optional.empty());
    }

    public void trigger(ServerPlayer player, Set<ResourceLocation> read) {
        Optional<Registry<BookDef>> books = player.level().registryAccess().registry(GottschBooksRegistries.BOOK);
        this.trigger(player, instance -> instance.matches(read, books));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final Optional<ResourceLocation> book;
        private final Optional<TagKey<BookDef>> books;
        private final Optional<String> allIn;

        public TriggerInstance(ContextAwarePredicate player, Optional<ResourceLocation> book,
                               Optional<TagKey<BookDef>> books, Optional<String> allIn) {
            super(ID, player);
            this.book = book;
            this.books = books;
            this.allIn = allIn;
        }

        public boolean matches(Set<ResourceLocation> read, Optional<Registry<BookDef>> registry) {
            if (book.isPresent() && !read.contains(book.get())) {
                return false;
            }
            if (books.isPresent() && !readEvery(read, registry.flatMap(r -> r.getTag(books.get()))
                    .map(tag -> tag.stream()).orElseGet(Stream::empty))) {
                return false;
            }
            return allIn.isEmpty() || readEvery(read, registry.map(r -> r.holders()
                    .filter(h -> h.key().location().getNamespace().equals(allIn.get()))
                    .<Holder<BookDef>>map(h -> h)).orElseGet(Stream::empty));
        }

        /** every enabled book of {@code books} read; false when there are none */
        private static boolean readEvery(Set<ResourceLocation> read, Stream<Holder<BookDef>> books) {
            List<ResourceLocation> ids = books.filter(h -> !h.value().disabled())
                    .flatMap(h -> h.unwrapKey().stream()).map(ResourceKey::location).toList();
            return !ids.isEmpty() && read.containsAll(ids);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            book.ifPresent(id -> json.addProperty("book", id.toString()));
            books.ifPresent(tag -> json.addProperty("books", "#" + tag.location()));
            allIn.ifPresent(namespace -> json.addProperty("all_in", namespace));
            return json;
        }
    }
}
