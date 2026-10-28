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
package mod.gottsch.neo.gottschbooks.export;

import mod.gottsch.neo.gottschbooks.client.LoadedBook;
import mod.gottsch.neo.gottschbooks.client.Manual;
import mod.gottsch.neo.gottschbooks.core.text.Inline;
import mod.gottsch.neo.gottschbooks.core.text.ItemReference;
import mod.gottsch.neo.gottschbooks.core.text.LoreText;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What the HTML and Markdown pages share: which pages a book makes, what each is called, where an {@code entry:} link
 * leads, and a parsed text's styled runs. The manual is read with {@link Manual#of}, so its order, titles and
 * fallbacks match the game's; locked entries are exported too, marked as locked. A lore book is read with
 * {@link LoadedBook#lore}, so its chapters are one page, in the game's order, after a list of them (when there are
 * two or more), and links to a chapter or its headings lead down the page.
 *
 * @author Mark Gottschling on 10/28/2026
 */
abstract class WebPages {
    /** the colour the parser gives links, which a web page's own link style replaces */
    private static final int LINK_COLOUR = 0x1E4FA0;

    protected final LoadedBook book;
    protected final String locale;
    /** null for a lore book */
    protected final Manual manual;
    /** null for a manual, or a lore book with no text */
    protected final LoreText lore;

    WebPages(LoadedBook book, String locale) {
        this.book = book;
        this.locale = locale;
        this.manual = book.isManual() ? Manual.of(book, locale) : null;
        this.lore = manual == null ? book.lore(locale).orElse(null) : null;
    }

    /** every page, file name to contents: the index (or the lore book's one page) first */
    Map<String, String> pages() {
        Map<String, String> pages = new LinkedHashMap<>();
        if (manual == null) {
            if (lore == null) {
                throw new IllegalArgumentException("book " + book.id() + " is a lore book with no " + LoadedBook.LORE_ENTRY
                        + ".md and no chapters");
            }
            pages.put(indexPage() + extension(), lore(lore));
        } else {
            pages.put(indexPage() + extension(), index());
            for (Manual.Entry entry : manual.allEntries()) {
                pages.put(page(entry.id()) + extension(), entry(entry));
            }
        }
        return pages;
    }

    /** {@code .html} or {@code .md} */
    abstract String extension();

    /** the index page's name, without the extension */
    abstract String indexPage();

    abstract String index();

    abstract String entry(Manual.Entry entry);

    /** a lore book's one page: its title, author, chapter list (with two or more), then the text */
    abstract String lore(LoreText lore);

    /** the entry name {@link #anchor} knows a lore book's one page by */
    static final String LORE_PAGE = "";

    /** how a link to {@code entry}'s page reaches the heading with {@code {#anchor}} */
    abstract String anchor(String entry, String anchor);

    /** how a link names a page */
    abstract String link(String page);

    /**
     * An entry's page name: its id, with each folder's {@code /} as {@code --}, so every page sits in one folder (as a
     * GitHub wiki needs) and links between them stay relative.
     */
    static String page(String entry) {
        return entry.replace("/", "--");
    }

    /** the manual's categories, then "" for the entries in none (or in one that doesn't exist); locked ones included */
    List<Manual.Entry> entriesIn(String category) {
        List<Manual.Entry> in = new ArrayList<>();
        for (Manual.Entry entry : manual.allEntries()) {
            boolean known = manual.categories().stream().anyMatch(c -> c.id().equals(entry.category()));
            if ((known ? entry.category() : "").equals(category)) {
                in.add(entry);
            }
        }
        return in;
    }

    /**
     * Where a link leads on the web: a web link as written, an {@code entry:} link as a relative link to that entry's
     * page, and in a lore book down its one page to the chapter or heading; empty for an entry link that leads
     * nowhere (to an entry or chapter that doesn't exist), which is then plain text.
     */
    Optional<String> href(ClickEvent click) {
        if (click.getAction() == ClickEvent.Action.OPEN_URL) {
            return Optional.of(click.getValue());
        }
        if (click.getAction() != ClickEvent.Action.CHANGE_PAGE || !click.getValue().startsWith(Inline.ENTRY_LINK)) {
            return Optional.empty();
        }
        String[] target = click.getValue().substring(Inline.ENTRY_LINK.length()).split("#", 2);
        if (manual == null) {
            return loreAnchor(target).map(anchor -> "#" + anchor(LORE_PAGE, anchor));
        }
        boolean exists = manual.allEntries().stream().anyMatch(e -> e.id().equals(target[0]));
        if (!exists) {
            return Optional.empty();
        }
        return Optional.of(link(page(target[0])) + (target.length > 1 ? "#" + anchor(target[0], target[1]) : ""));
    }

    /** where a lore book's link ({@code chapter}, {@code chapter#anchor}, {@code text#anchor}) leads: an anchor */
    private Optional<String> loreAnchor(String[] target) {
        if (lore == null) {
            return Optional.empty();
        }
        Optional<LoreText.Chapter> chapter = lore.chapter(target[0]);
        if (chapter.isPresent()) {
            return Optional.of(target.length > 1 ? target[1] : chapter.get().anchor());
        }
        return target[0].equals(LoreText.TEXT) && target.length > 1 ? Optional.of(target[1]) : Optional.empty();
    }

    /** a placeholder's label for a fenced block's kind; empty for kinds the reader doesn't know */
    static Optional<String> label(String kind) {
        return switch (kind) {
            case "spotlight" -> Optional.of("Item");
            case "recipe" -> Optional.of("Recipe");
            case "image" -> Optional.of("Image");
            case "entity" -> Optional.of("Mob");
            default -> Optional.empty();
        };
    }

    /** an illustration's placeholder label */
    static final String ILLUSTRATION = "Picture";

    /** an item reference as it reads on a placeholder: {@code ns:id}, with the scheme only when it isn't an item */
    static String id(ItemReference item) {
        return (item.scheme().equals(ItemReference.ITEM) ? "" : item.scheme() + ":") + item.id() + item.components();
    }

    /** the title of a lore book, or of a manual's index */
    String title() {
        if (manual != null) {
            return manual.title().getString();
        }
        return Optional.ofNullable(lore).flatMap(t -> t.text().get("title")).map(t -> Inline.parse(t).getString())
                .orElse(book.id().toString());
    }

    /**
     * One styled run of text. {@code colour} is an RGB value or null; {@code href} is where the run links to, or null.
     * A {@code {font:...}} run is plain text: a web page has no resource pack fonts, and a colour chosen for a font's
     * pictures (white, say) would hide the letters on a page.
     */
    record Run(String text, boolean bold, boolean italic, Integer colour, String href) {
        boolean sameStyle(Run other) {
            return bold == other.bold && italic == other.italic && Objects.equals(colour, other.colour)
                    && Objects.equals(href, other.href);
        }
    }

    /** a text's runs, with neighbours of the same style joined */
    List<Run> runs(Component text) {
        List<Run> runs = new ArrayList<>();
        text.visit((style, part) -> {
            if (part.isEmpty()) {
                return Optional.empty();
            }
            String href = style.getClickEvent() == null ? null : href(style.getClickEvent()).orElse(null);
            Run run;
            if (!style.getFont().equals(Style.DEFAULT_FONT)) {
                run = new Run(part, false, false, null, href);
            } else {
                Integer colour = style.getColor() == null ? null : style.getColor().getValue();
                if (style.getClickEvent() != null && colour != null && colour == LINK_COLOUR) {
                    colour = null;
                }
                run = new Run(part, style.isBold(), style.isItalic(), colour, href);
            }
            if (!runs.isEmpty() && runs.get(runs.size() - 1).sameStyle(run)) {
                Run last = runs.remove(runs.size() - 1);
                run = new Run(last.text() + run.text(), run.bold(), run.italic(), run.colour(), run.href());
            }
            runs.add(run);
            return Optional.empty();
        }, Style.EMPTY);
        return runs;
    }
}
