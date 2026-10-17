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
package mod.gottsch.neo.gottschbooks.core.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.gottsch.neo.gottschbooks.api.BookDef;
import mod.gottsch.neo.gottschbooks.api.GottschBooksRegistries;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;

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

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Set<ResourceLocation> read) {
        Optional<Registry<BookDef>> books = player.registryAccess().registry(GottschBooksRegistries.BOOK);
        this.trigger(player, instance -> instance.matches(read, books));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> book,
                                  Optional<TagKey<BookDef>> books, Optional<String> allIn)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ResourceLocation.CODEC.optionalFieldOf("book").forGetter(TriggerInstance::book),
                TagKey.hashedCodec(GottschBooksRegistries.BOOK).optionalFieldOf("books").forGetter(TriggerInstance::books),
                Codec.STRING.optionalFieldOf("all_in").forGetter(TriggerInstance::allIn)
        ).apply(instance, TriggerInstance::new));

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
    }
}
