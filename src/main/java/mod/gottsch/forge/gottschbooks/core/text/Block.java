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

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One block of a parsed book file: the page-independent tree the reader flows onto pages.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public sealed interface Block {

    /** a paragraph; {@code quote} for a {@code >} paragraph (an editor's note: italic, grey) */
    record Paragraph(Component text, boolean quote) implements Block {}

    /**
     * {@code #} to {@code ######}; {@code level} 1 is the entry's title. {@code anchor} is the id from a trailing
     * {@code {#id}}, which {@code [text](entry:<entry>#id)} links jump to; empty without one.
     */
    record Heading(int level, Component text, String anchor) implements Block {}

    /** {@code ---}: an ornament between sections */
    record Ornament() implements Block {}

    /** {@code ![caption](scheme:namespace:path)}; an empty caption means "the item's own name" */
    record Illustration(String scheme, ResourceLocation id, String caption) implements Block {}

    /** {@code <!-- page -->}: the next block starts a new page */
    record PageBreak() implements Block {}

    /** {@code - item} or {@code 1. item}: one item of a one-level list; {@code number} is 0 for a bullet */
    record ListItem(int number, Component text) implements Block {}

    /**
     * A fenced block: {@code ```kind argument}, its lines, then {@code ```}. The reader knows {@code spotlight}
     * ({@code scheme:ns:id} or {@code ns:id}), {@code recipe} ({@code ns:id}), {@code image}
     * ({@code ns:textures/....png [width height]}) and {@code entity} ({@code ns:id}); the body is ordinary book text
     * drawn under it. Any other kind shows its body as plain lines.
     */
    record Fence(String kind, String argument, List<Block> body) implements Block {}
}
