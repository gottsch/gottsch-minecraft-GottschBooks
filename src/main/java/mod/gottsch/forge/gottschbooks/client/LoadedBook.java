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

import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import mod.gottsch.forge.gottschbooks.core.text.LoreText;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * A book's client assets, from {@code assets/<namespace>/books/<book>/}: the optional {@code book.md} (front matter
 * for the whole book: {@code skin}) and every {@code <locale>/<entry>.md}.
 *
 * @param id      the book ({@code magictreasures:tidewalkers_log})
 * @param meta    {@code book.md}; empty when the book has none
 * @param entries entry name to locale to text
 *
 * @author Mark Gottschling on 10/17/2026
 */
public record LoadedBook(ResourceLocation id, BookText meta, Map<String, Map<String, BookText>> entries) {
    public static final String FALLBACK_LOCALE = "en_us";
    /** a lore book's opening (or only) entry: {@code <locale>/text.md}; chapters may follow it ({@link LoreText}) */
    public static final String LORE_ENTRY = LoreText.TEXT;
    public static final ResourceLocation DEFAULT_SKIN =
            new ResourceLocation(GottschBooks.MOD_ID, "textures/gui/book");

    /** {@code entry} in {@code locale}, else in {@code en_us}, else in whatever locale has it */
    public Optional<BookText> entry(String entry, String locale) {
        Map<String, BookText> byLocale = entries.get(entry);
        if (byLocale == null || byLocale.isEmpty()) {
            return Optional.empty();
        }
        BookText text = byLocale.get(locale);
        if (text == null) {
            text = byLocale.get(FALLBACK_LOCALE);
        }
        return Optional.ofNullable(text != null ? text : byLocale.values().iterator().next());
    }

    /**
     * a lore book's text in {@code locale}: {@code text.md}, then its chapters, each file in {@code locale}, else in
     * {@code en_us}, else in whatever locale has it; empty when the book has neither
     */
    public Optional<LoreText> lore(String locale) {
        return LoreText.of(meta, entries.keySet(), name -> entry(name, locale));
    }

    /** a manual ({@code mode: manual} in book.md), rather than a lore book */
    public boolean isManual() {
        return meta.get("mode").map(mode -> mode.equalsIgnoreCase("manual")).orElse(false);
    }

    /** the folder holding the art ({@code cover.png}, {@code page.png}, {@code ornament.png}); {@code skin:} in book.md */
    public ResourceLocation skin() {
        return meta.get("skin").map(ResourceLocation::tryParse).orElse(DEFAULT_SKIN);
    }
}
