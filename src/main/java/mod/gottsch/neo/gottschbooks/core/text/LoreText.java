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

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lore book's whole text, as one file: {@code <locale>/text.md}, then its chapters, {@code <locale>/NN-name.md}
 * ({@code 01-arrival.md}, {@code 02-the-storm.md}), in the order of their numbers. A book with only {@code text.md}
 * reads exactly as it always has.
 * <p>
 * Each chapter starts on a new page, in the book's page style ({@code pages:} in book.md). Its first heading is its
 * title, and keeps its {@code {#anchor}}, or is given the chapter's name ({@code 02-the-storm}) as one, so links and
 * the contents can jump to it; a chapter with no heading gets one, from its name. The front matter (title, author) is
 * {@code text.md}'s, or the first chapter's in a book without one.
 * <p>
 * Every file falls back to {@code en_us} on its own, so the chapters are those of every language together: a
 * translation missing a chapter reads that chapter in English, where it belongs in the order. Chapters are matched
 * across languages by their file name, so a translation keeps {@code en_us}'s names.
 *
 * @param text     the text, every chapter in it
 * @param chapters the chapters, in order; empty for a book of {@code text.md} alone
 *
 * @author Mark Gottschling on 10/28/2026
 */
public record LoreText(BookText text, List<Chapter> chapters) {
    /** a lore book's opening (or only) file: {@code <locale>/text.md} */
    public static final String TEXT = "text";
    /** a chapter's file name, without {@code .md}: a number, a hyphen, then a name ({@code 01-arrival}) */
    public static final Pattern CHAPTER = Pattern.compile("(\\d{1,4})-([a-z0-9_-]+)");
    private static final String PAGES = "pages", ONE_PAGE = "single", TWO_PAGES = "spread";

    /**
     * A chapter: its file's name ({@code 02-the-storm}), the anchor of the heading it opens with, its title (that
     * heading's text), and the index in {@link #text}'s blocks of the page break it starts with.
     */
    public record Chapter(String name, String anchor, Component title, int block) {}

    /** whether {@code entry} ({@code 01-arrival}) names a chapter */
    public static boolean isChapter(String entry) {
        return CHAPTER.matcher(entry).matches();
    }

    /** a chapter's number: {@code 2} for {@code 02-the-storm} */
    public static int number(String chapter) {
        Matcher m = CHAPTER.matcher(chapter);
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** the chapters among {@code entries}, by number, then by name */
    public static List<String> chapters(Collection<String> entries) {
        return entries.stream().filter(LoreText::isChapter)
                .sorted(Comparator.comparingInt(LoreText::number).thenComparing(Comparator.naturalOrder())).toList();
    }

    /** a book of {@code text.md} alone */
    public static LoreText of(BookText text) {
        return new LoreText(text, List.of());
    }

    /**
     * The lore book whose files are {@code entries}, each read through {@code file} (which falls back to another
     * language as it will); {@code meta} is book.md, for the page style chapters start in. Empty when there is neither
     * a {@code text.md} nor a chapter.
     */
    public static Optional<LoreText> of(BookText meta, Collection<String> entries, Function<String, Optional<BookText>> file) {
        Optional<BookText> opening = entries.contains(TEXT) ? file.apply(TEXT) : Optional.empty();
        List<String> names = chapters(entries);
        if (names.isEmpty()) {
            return opening.map(LoreText::of);
        }
        String style = meta.get(PAGES).filter(pages -> pages.strip().equalsIgnoreCase(ONE_PAGE)).isPresent() ? ONE_PAGE : TWO_PAGES;
        List<Block> blocks = new ArrayList<>(opening.map(BookText::blocks).orElse(List.of()));
        List<Chapter> chapters = new ArrayList<>();
        BookText front = opening.orElse(null);
        for (String name : names) {
            Optional<BookText> chapter = file.apply(name);
            if (chapter.isEmpty()) {
                continue;
            }
            if (front == null) {
                front = chapter.get();
            }
            int start = blocks.size();
            blocks.add(new Block.PageBreak(style));
            List<Block> body = new ArrayList<>(chapter.get().blocks());
            int at = firstHeading(body);
            Block.Heading heading;
            if (at < 0) {
                heading = new Block.Heading(1, Component.literal(title(name)), name);
                body.add(0, heading);
            } else {
                Block.Heading h = (Block.Heading) body.get(at);
                heading = h.anchor().isEmpty() ? new Block.Heading(h.level(), h.text(), name) : h;
                body.set(at, heading);
            }
            blocks.addAll(body);
            chapters.add(new Chapter(name, heading.anchor(), heading.text(), start));
        }
        if (front == null) {
            return Optional.empty();
        }
        return Optional.of(new LoreText(new BookText(front.front(), List.copyOf(blocks)), List.copyOf(chapters)));
    }

    /** the first top-level heading's index, or -1 */
    private static int firstHeading(List<Block> blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i) instanceof Block.Heading) {
                return i;
            }
        }
        return -1;
    }

    /** a title made from a chapter's name, for a chapter with no heading: {@code 02-the-storm} is "The storm" */
    static String title(String chapter) {
        Matcher m = CHAPTER.matcher(chapter);
        String words = (m.matches() ? m.group(2) : chapter).replace('-', ' ').replace('_', ' ').strip();
        return words.isEmpty() ? chapter : words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
    }

    /** the chapter named {@code name} */
    public Optional<Chapter> chapter(String name) {
        return chapters.stream().filter(chapter -> chapter.name().equals(name)).findFirst();
    }
}
