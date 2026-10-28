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
import mod.gottsch.forge.gottschbooks.core.ItemMatch;
import mod.gottsch.forge.gottschbooks.core.text.Block;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import mod.gottsch.forge.gottschbooks.core.text.Inline;
import mod.gottsch.forge.gottschbooks.core.text.TextSearch;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The book reader: an open book sized to the window, two pages side by side (one on a narrow window).
 * <p>
 * The text is flowed onto as many pages as the window needs, so a big window shows more per page and no text, in any
 * language, can run off a page. {@code ---} sections are separated by an ornament; an ornament or a heading never
 * ends a page and always has two lines after it. Illustrations and spotlights are items drawn larger (kept to whole
 * screen pixels) with the item's tooltip on hover. {@code >} paragraphs are editor's notes: italic and grey.
 * <p>
 * Two modes, from the book's {@code book.md}:
 * <ul>
 *     <li><b>Lore book</b>: one text, opening on a title page (icon, title, author), turned page by page. The reader
 *     remembers the section you were on for each book until the game closes.</li>
 *     <li><b>Manual</b> ({@link Manual}): opens on the contents (the landing text, then each category's entries);
 *     a click opens an entry, {@code [text](entry:id#anchor)} links jump between entries, and Back and Contents sit
 *     above the book. Entries can show {@code spotlight}, {@code recipe}, {@code image} and {@code entity}
 *     blocks. Search (above the book, or Ctrl+F) finds the entries that mention every word typed.</li>
 * </ul>
 * Moved from Magic Treasures' {@code LoreBookScreen}.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public class BookScreen extends Screen {
    private static final int TEX = 32, COVER_BORDER = 8, PAGE_BORDER = 5;
    private static final int ORNAMENT_W = 21, ORNAMENT_H = 5;

    private static final int INK = 0xFF3B2A1A;
    private static final int NOTE = 0xFF7A6650;
    private static final int LINK = 0xFF1E4FA0;
    private static final int SLOT = 0x30000000;
    private static final int LINE_H = 10;
    private static final int ROW_H = 18;
    private static final int LIST_GAP = 3;
    private static final int ORNAMENT_SLOT = 16;
    private static final int MARGIN = 12;
    private static final int MAX_BOOK_W = 520, MAX_BOOK_H = 340;
    /** the front matter key for a book's or entry's page style, and the styles: one wide page, or two side by side */
    static final String PAGES = "pages", ONE_PAGE = "single", TWO_PAGES = "spread";
    /** below this width a spread's pages get too narrow to read, so the book shows one page */
    private static final int MIN_SPREAD_W = 320;
    private static final int PAGE_PAD = 12;
    private static final int FOOTER = 18;
    private static final float TITLE_SCALE = 1.5F;
    /** illustrations aim for this, rounded so each item pixel covers whole screen pixels (see {@link #init}) */
    private static final double ILLUSTRATION_TARGET = 1.5;
    /** the {@code entry} of a manual's search page, which no entry id can be */
    private static final String SEARCH = "\u0000search";
    /** the search box's frame, drawn on the page, and how far its text sits inside it */
    private static final int SEARCH_BOX_H = 14, SEARCH_INSET_X = 4, SEARCH_INSET_Y = 3;

    /** where each book was left, for this game session: a lore book's section, a manual's entry and section */
    private static final Map<ResourceLocation, Place> LAST_PLACE = new HashMap<>();

    /** an entry (null for a manual's contents) and the section within it */
    private record Place(@Nullable String entry, int section) {}

    private final ResourceLocation bookId;
    private final ItemStack icon;
    private final ResourceLocation cover, page, ornament;
    /** book.md's {@code pages:} */
    private final Optional<String> bookPages;
    // lore book
    private final Optional<Component> author;
    private final List<Block> loreBlocks;
    // manual
    @Nullable
    private final Manual manual;
    /** the entry being read; null on the contents */
    @Nullable
    private String entry;
    private final Deque<Place> history = new ArrayDeque<>();
    /** an anchor to open at, once the layout exists */
    @Nullable
    private String pendingAnchor;
    /** an item whose page to open at, once the layout exists */
    @Nullable
    private ItemStack pendingItem;
    /** the screen to go back to on closing; null to close to the game */
    @Nullable
    private Screen parent;
    /** what the search page searches for, kept while reading what it found */
    private String query = "";
    @Nullable
    private ManualSearch search;
    private List<ManualSearch.Result> results = List.of();
    @Nullable
    private EditBox searchBox;

    // layout, recomputed in init() (it runs again when the window is resized)
    private int bookX, bookY, bookW, bookH;
    /** the window is wide enough for two pages side by side */
    private boolean spread;
    private int pageW, pageH;
    /** the text width of a page in a spread, and of a single wide page; and of the page being laid out or drawn */
    private int pageTextW, wideTextW, textW;
    private float illustrationScale;
    /** {@code #} and {@code ##} heading text scales, kept to whole screen pixels like the illustrations */
    private float headingScale1, headingScale2;
    private int illustrationSize;
    private final List<Page> pages = new ArrayList<>();
    /** what each turn of the page shows: a spread's one or two pages, or one wide page */
    private final List<View> views = new ArrayList<>();
    private int view;
    private PageButton back, forward;
    /** where things were drawn this frame, for tooltips and clicks */
    private final List<ItemHit> itemHits = new ArrayList<>();
    private final List<LinkHit> linkHits = new ArrayList<>();
    private final List<TextHit> textHits = new ArrayList<>();

    private enum Kind { TEXT, BLANK, ORNAMENT, TITLE, ENTRY_TITLE, ILLUSTRATION, SPOTLIGHT, RECIPE, IMAGE, ENTITY,
        CATEGORY, LINK_ROW, BREAK, HEADING, TABLE_HEADER, TABLE_ROW, SEARCH_BOX }

    /** a page's lines, and whether it is a single wide page ({@code {page:single}}) */
    private record Page(List<Line> lines, boolean wide) {}

    /** one turn of the page: {@code count} pages from {@code first}, side by side, or one wide page */
    private record View(int first, int count, boolean wide) {}

    /** one row of a page */
    private static final class Line {
        final Kind kind;
        final int height;
        int section;
        /** laid out for a single wide page; a page break's is the style of the pages after it */
        boolean wide;
        /** height that must follow this line on the same page (headings, ornaments, spotlights) */
        int keep;
        FormattedCharSequence text;
        int color = INK;
        int indent;
        /** how far a text line's text sits below the line's top, to make room for tall glyphs above it */
        int lift;
        String bullet;
        ItemStack item = ItemStack.EMPTY;
        Component label;
        String target;
        String anchor;
        /** a heading's text scale, and whether a rule goes under it */
        float scale = 1F;
        boolean rule;
        RecipeView recipe;
        ImageView image;
        LivingEntity entity;
        /** a table header or row: its table, and which row (-1 for the header) */
        TableView table;
        int row;

        Line(Kind kind, int height, int section) {
            this.kind = kind;
            this.height = height;
            this.section = section;
        }
    }

    private record ImageView(ResourceLocation texture, int texW, int texH, int w, int h) {}

    private record ItemHit(int x, int y, int size, ItemStack item) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + size && my >= y && my < y + size;
        }
    }

    private record LinkHit(int x, int y, int w, int h, String target) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record TextHit(int x, int y, FormattedCharSequence text) {}

    /** a lore book */
    public BookScreen(LoadedBook book, BookText text, ItemStack icon) {
        super(loreTitle(book, text));
        this.bookId = book.id();
        this.icon = icon;
        this.author = text.get("author").map(Component::literal);
        List<Block> body = new ArrayList<>(text.blocks());
        if (text.get("title").isEmpty() && !body.isEmpty() && body.get(0) instanceof Block.Heading h && h.level() == 1) {
            body.remove(0);
        }
        this.loreBlocks = body;
        this.manual = null;
        ResourceLocation skin = book.skin();
        this.cover = art(skin, "cover");
        this.page = art(skin, "page");
        this.ornament = art(skin, "ornament");
        this.bookPages = book.meta().get(PAGES);
    }

    /** a manual, opened at {@code target} ({@code entry} or {@code entry#anchor}), else where it was left */
    public BookScreen(LoadedBook book, Manual manual, ItemStack icon, Optional<String> target) {
        super(manual.title());
        this.bookId = book.id();
        this.icon = icon;
        this.author = Optional.empty();
        this.loreBlocks = List.of();
        this.manual = manual;
        ResourceLocation skin = book.skin();
        this.cover = art(skin, "cover");
        this.page = art(skin, "page");
        this.ornament = art(skin, "ornament");
        this.bookPages = book.meta().get(PAGES);
        if (target.isPresent()) {
            String[] parts = target.get().split("#", 2);
            if (manual.entry(parts[0]).isPresent()) {
                entry = parts[0];
                pendingAnchor = parts.length > 1 ? parts[1] : null;
            }
        } else {
            Place last = LAST_PLACE.get(bookId);
            if (last != null && last.entry() != null && manual.entry(last.entry()).isPresent()) {
                entry = last.entry();
            }
        }
    }

    /** opens at the page showing {@code item} (see {@link #pageOfItem}), and closes back to {@code parent} */
    public void showing(ItemStack item, @Nullable Screen parent) {
        this.pendingItem = item;
        this.parent = parent;
    }

    /** {@code title:} in the front matter, else a leading {@code # heading}, else the book's id */
    private static Component loreTitle(LoadedBook book, BookText text) {
        Optional<Component> title = text.get("title").map(Component::literal);
        if (title.isEmpty() && !text.blocks().isEmpty() && text.blocks().get(0) instanceof Block.Heading h && h.level() == 1) {
            title = Optional.of(h.text());
        }
        return title.orElseGet(() -> Component.literal(book.id().toString()));
    }

    private static ResourceLocation art(ResourceLocation skin, String name) {
        return skin.withSuffix("/" + name + ".png");
    }

    private boolean isManual() {
        return manual != null;
    }

    /**
     * the style the text starts in: {@code pages: single} (one wide page, for tables) or {@code pages: spread} (two
     * pages, the default), from the open entry's front matter, else book.md's; {@code {page:...}} breaks switch it
     */
    private boolean wantsOnePage() {
        Optional<String> pages = manual == null || entry == null ? Optional.empty()
                : manual.entry(entry).flatMap(e -> e.text().get(PAGES));
        return pages.or(() -> bookPages).map(style -> style.strip().equalsIgnoreCase(ONE_PAGE)).orElse(false);
    }

    @Override
    protected void init() {
        // where the reader was: the old layout's section, else the remembered one
        int anchor = pages.isEmpty() ? remembered() : currentSection();

        bookW = Math.min(width - 2 * MARGIN, MAX_BOOK_W);
        bookH = Math.min(height - 2 * MARGIN, MAX_BOOK_H);
        // a window too narrow for two pages shows one narrow page at a time, whatever the style
        spread = bookW >= MIN_SPREAD_W;
        if (!spread) {
            bookW = Math.min(bookW, MIN_SPREAD_W / 2 + 40);
        }
        bookX = (width - bookW) / 2;
        bookY = (height - bookH) / 2;
        pageW = spread ? (bookW - 2 * COVER_BORDER - 4) / 2 : bookW - 2 * COVER_BORDER;
        pageH = bookH - 2 * COVER_BORDER;
        pageTextW = pageW - 2 * PAGE_PAD;
        wideTextW = spread ? bookW - 2 * COVER_BORDER - 2 * PAGE_PAD : pageTextW;
        textW = pageTextW;

        // 1.5x of a GUI pixel is 4.5 screen pixels at GUI scale 3, which draws item pixels unevenly; round to whole
        // screen pixels instead (exactly 1.5x at scales 2 and 4, 1.67x at 3)
        double guiScale = minecraft.getWindow().getGuiScale();
        illustrationScale = (float) (Math.max(1, Math.round(ILLUSTRATION_TARGET * guiScale)) / guiScale);
        illustrationSize = (int) Math.ceil(16 * illustrationScale);
        headingScale1 = (float) (Math.max(1, Math.round(1.5 * guiScale)) / guiScale);
        headingScale2 = (float) (Math.max(1, Math.round(1.25 * guiScale)) / guiScale);

        paginate();
        if (pendingAnchor != null) {
            view = viewOf(pageOfAnchor(pendingAnchor));
            pendingAnchor = null;
        } else if (pendingItem != null) {
            view = viewOf(pageOfItem(pendingItem));
            pendingItem = null;
        } else {
            view = Math.min(viewOf(pageOfSection(anchor)), lastView());
        }

        int buttonY = bookY + bookH - COVER_BORDER - 16;
        back = addRenderableWidget(new PageButton(bookX + COVER_BORDER + 6, buttonY, false, b -> turn(-1), true));
        forward = addRenderableWidget(new PageButton(bookX + bookW - COVER_BORDER - 6 - 23, buttonY, true, b -> turn(1), true));
        if (isManual()) {
            // no border or shadow, in ink: the page draws the frame (see drawPage), so the box looks written on
            searchBox = new PageEditBox(font, 0, 0, pageTextW, SEARCH_BOX_H - SEARCH_INSET_Y, Component.translatable("gottschbooks.search"), INK);
            searchBox.setMaxLength(64);
            searchBox.setHint(Component.translatable("gottschbooks.search.hint")
                    .withStyle(style -> style.withColor(NOTE & 0xFFFFFF).withItalic(true)));
            searchBox.setValue(query);
            searchBox.setResponder(this::searched);
            addRenderableWidget(searchBox);
        }
        updateButtons();
        if (SEARCH.equals(entry)) {
            focusSearch();
        }
    }

    private int remembered() {
        Place last = LAST_PLACE.get(bookId);
        if (last == null || isManual() && !Objects.equals(last.entry(), entry)) {
            return 0;
        }
        return last.section();
    }

    // ---- layout ----

    /** builds a column of lines from blocks; sections are counted by ornaments */
    private final class Flow {
        final List<Line> lines = new ArrayList<>();
        /** the style being laid out; {@code textW} follows it */
        boolean wide = wantsOnePage();
        int section;
        boolean textBefore;
        boolean listBefore;

        Line add(Kind kind, int height) {
            Line line = new Line(kind, height, section);
            line.wide = wide;
            lines.add(line);
            return line;
        }

        void blank() {
            add(Kind.BLANK, LINE_H);
        }

        /** {@code single} or {@code spread} switches the style (and the width text is laid out in); anything else keeps it */
        void style(String style) {
            if (style.equals(ONE_PAGE) || style.equals(TWO_PAGES)) {
                wide = style.equals(ONE_PAGE);
                textW = wide ? wideTextW : pageTextW;
            }
        }

        void blocks(List<Block> blocks) {
            for (Block block : blocks) {
                block(block);
            }
        }

        void block(Block block) {
            if (block instanceof Block.Ornament) {
                section++;
                Line line = add(Kind.ORNAMENT, ORNAMENT_SLOT);
                line.keep = 2 * LINE_H;
                textBefore = false;
                listBefore = false;
            } else if (block instanceof Block.Paragraph p) {
                if (textBefore) {
                    blank();
                }
                text(p.quote() ? Component.empty().withStyle(ChatFormatting.ITALIC).append(p.text()) : p.text(),
                        p.quote() ? NOTE : INK, null);
                textBefore = true;
                listBefore = false;
            } else if (block instanceof Block.Heading h) {
                if (textBefore || listBefore) {
                    blank();
                }
                List<Line> added = h.level() <= 2
                        ? heading(h.text(), h.level() == 1 ? headingScale1 : headingScale2)
                        : text(Component.empty().withStyle(ChatFormatting.BOLD).append(h.text()), INK, null);
                if (!added.isEmpty()) {
                    added.get(0).anchor = h.anchor().isEmpty() ? null : h.anchor();
                    added.get(added.size() - 1).keep = 2 * LINE_H;
                }
                // the text under a heading follows it directly
                textBefore = false;
                listBefore = false;
            } else if (block instanceof Block.ListItem item) {
                if (textBefore && !listBefore) {
                    blank();
                } else if (listBefore) {
                    // a little air between items, so wrapped items don't run together
                    add(Kind.BLANK, LIST_GAP);
                }
                text(item.text(), INK, item.number() > 0 ? item.number() + "." : "\u2022");
                textBefore = true;
                listBefore = true;
            } else if (block instanceof Block.Illustration i) {
                Icons.stack(i.item()).ifPresentOrElse(item -> {
                    Component caption = Component.literal(i.caption().isEmpty() ? item.getHoverName().getString()
                            : i.caption()).withStyle(ChatFormatting.ITALIC);
                    int captionLines = font.split(caption, textW).size();
                    Line line = add(Kind.ILLUSTRATION, 6 + illustrationSize + 3 + captionLines * LINE_H + 6);
                    line.item = item;
                    line.label = caption;
                    line.color = NOTE;
                }, () -> missing("item", i.item().text()));
            } else if (block instanceof Block.PageBreak b) {
                style(b.style());
                add(Kind.BREAK, 0);
                textBefore = false;
                listBefore = false;
            } else if (block instanceof Block.Fence f) {
                fence(f);
            } else if (block instanceof Block.Table t) {
                table(t);
            }
        }

        private void table(Block.Table t) {
            if (textBefore || listBefore) {
                blank();
            }
            TableView view = TableView.of(t, font, textW, LINE_H, NOTE, BookScreen.this::overhang);
            if (view.header() != null) {
                Line header = add(Kind.TABLE_HEADER, view.header().height());
                header.table = view;
                header.row = -1;
                // never the last thing on a page
                header.keep = view.rows().isEmpty() ? 0 : view.rows().get(0).height();
            }
            for (int r = 0; r < view.rows().size(); r++) {
                Line row = add(Kind.TABLE_ROW, view.rows().get(r).height());
                row.table = view;
                row.row = r;
            }
            textBefore = true;
            listBefore = false;
        }

        private void fence(Block.Fence fence) {
            switch (fence.kind()) {
                case "spotlight" -> {
                    Optional<ItemStack> item = Icons.stack(fence.argument());
                    if (item.isPresent()) {
                        Line line = add(Kind.SPOTLIGHT, 44);
                        line.item = item.get();
                        line.keep = 2 * LINE_H;
                    } else {
                        missing("item", fence.argument());
                    }
                }
                case "recipe" -> {
                    Optional<RecipeView> recipe = recipe(fence.argument());
                    if (recipe.isPresent()) {
                        RecipeView view = recipe.get();
                        boolean under = !view.station().isEmpty() || view.note() != null;
                        Line line = add(Kind.RECIPE, (view.rows() + (under ? 1 : 0)) * ROW_H + 10);
                        line.recipe = recipe.get();
                    } else {
                        missing("recipe", fence.argument());
                    }
                }
                case "image" -> {
                    Optional<ImageView> image = image(fence.argument());
                    if (image.isPresent()) {
                        Line line = add(Kind.IMAGE, image.get().h() + 8);
                        line.image = image.get();
                    } else {
                        missing("image", fence.argument());
                    }
                }
                case "entity" -> {
                    Optional<LivingEntity> entity = entity(fence.argument());
                    if (entity.isPresent()) {
                        Line line = add(Kind.ENTITY, 70);
                        line.entity = entity.get();
                    } else {
                        missing("entity", fence.argument());
                    }
                }
                default -> { }
            }
            // the block's own text follows it, as a new paragraph
            textBefore = false;
            listBefore = false;
            blocks(fence.body());
            if (!fence.body().isEmpty()) {
                textBefore = true;
            }
        }

        /** shows the author what is missing, rather than leaving a silent gap */
        private void missing(String what, String id) {
            GottschBooks.LOGGER.warn("book {}: no {} '{}'", bookId, what, id);
            text(Component.literal("[" + what + " " + id + "?]"), NOTE, null);
        }

        /** wrapped text; a list item draws {@code bullet} and hangs its lines after it */
        /**
         * a {@code #}/{@code ##} heading: drawn at {@code scale}, bold by {@link #drawHeading}, with a faint rule under
         * its last line
         */
        List<Line> heading(Component text, float scale) {
            List<Line> added = new ArrayList<>();
            List<FormattedCharSequence> rows = font.split(text, (int) (textW / scale));
            for (int i = 0; i < rows.size(); i++) {
                boolean last = i == rows.size() - 1;
                Line line = add(Kind.HEADING, (int) Math.ceil(LINE_H * scale) + (last ? 4 : 0));
                line.text = rows.get(i);
                line.scale = scale;
                line.rule = last;
                added.add(line);
            }
            return added;
        }

        List<Line> text(Component text, int color, @Nullable String bullet) {
            int indent = bullet == null ? 0 : font.width(bullet + " ");
            List<Line> added = new ArrayList<>();
            for (FormattedCharSequence seq : font.split(text, textW - indent)) {
                int[] overhang = overhang(seq);
                Line line = add(Kind.TEXT, LINE_H + overhang[0] + overhang[1]);
                line.lift = overhang[0];
                line.text = seq;
                line.color = color;
                line.indent = indent;
                if (added.isEmpty()) {
                    line.bullet = bullet;
                }
                added.add(line);
            }
            return added;
        }
    }

    /**
     * how far {@code seq}'s glyphs reach above the line's text and below it: room a font of tall pictures needs, so
     * they don't overlap the lines around them. Only other fonts are measured, so plain text keeps its fixed height.
     */
    private int[] overhang(FormattedCharSequence seq) {
        // a default glyph's box: from the top of a capital (0) to the bottom of a descender (8)
        float[] reach = {0F, 8F};
        seq.accept((index, style, codepoint) -> {
            if (!style.getFont().equals(Style.DEFAULT_FONT)) {
                BakedGlyph glyph = font.getFontSet(style.getFont()).getGlyph(codepoint);
                reach[0] = Math.min(reach[0], glyph.up);
                reach[1] = Math.max(reach[1], glyph.down);
            }
            return true;
        });
        return new int[] {(int) Math.ceil(-reach[0]), (int) Math.ceil(reach[1] - 8F)};
    }

    private List<Line> flow() {
        textW = wantsOnePage() ? wideTextW : pageTextW;
        Flow flow = new Flow();
        if (!isManual()) {
            flow.add(Kind.TITLE, titleHeight(title, author.isPresent()));
            flow.blocks(loreBlocks);
        } else if (entry == null) {
            contents(flow);
        } else if (SEARCH.equals(entry)) {
            searchPage(flow);
        } else {
            Manual.Entry current = manual.entry(entry).orElseThrow();
            Line title = flow.add(Kind.ENTRY_TITLE, entryTitleHeight(current.title()));
            title.label = current.title();
            title.item = Icons.stack(current.icon()).orElse(ItemStack.EMPTY);
            List<Block> blocks = new ArrayList<>(current.text().blocks());
            if (!blocks.isEmpty() && blocks.get(0) instanceof Block.Heading h && h.level() == 1) {
                blocks.remove(0);
            }
            flow.blocks(blocks);
        }
        return flow.lines;
    }

    /** a manual's opening: its title, the landing text, then each category with its entries */
    private void contents(Flow flow) {
        flow.add(Kind.TITLE, titleHeight(title, false));
        flow.blocks(manual.landing());
        for (Manual.Category category : manual.categories()) {
            List<Manual.Entry> entries = manual.entriesIn(category.id());
            if (entries.isEmpty()) {
                continue;
            }
            flow.blank();
            Line heading = flow.add(Kind.CATEGORY, ROW_H + 2);
            heading.label = category.title();
            heading.item = Icons.stack(category.icon()).orElse(ItemStack.EMPTY);
            heading.keep = ROW_H;
            flow.textBefore = false;
            if (!category.description().isEmpty()) {
                flow.blocks(category.description());
                flow.textBefore = false;
            }
            entries.forEach(entry -> row(flow, entry));
        }
        List<Manual.Entry> loose = manual.entriesIn("");
        if (!loose.isEmpty()) {
            flow.blank();
            loose.forEach(entry -> row(flow, entry));
        }
    }

    private Line row(Flow flow, Manual.Entry entry) {
        Line row = flow.add(Kind.LINK_ROW, ROW_H);
        row.label = entry.title();
        row.item = Icons.stack(entry.icon()).orElse(ItemStack.EMPTY);
        row.target = entry.id();
        return row;
    }

    /**
     * the search page: room for the search box (a widget, placed by {@link #updateButtons}), then what the query
     * finds, each entry with a line of its text that a click also opens
     */
    private void searchPage(Flow flow) {
        flow.add(Kind.SEARCH_BOX, SEARCH_BOX_H + 8);
        if (TextSearch.terms(query).isEmpty()) {
            results = List.of();
            flow.text(Component.translatable("gottschbooks.search.prompt"), NOTE, null);
            return;
        }
        if (search == null) {
            search = new ManualSearch(manual);
        }
        results = search.find(query, font, textW);
        flow.text(results.isEmpty() ? Component.translatable("gottschbooks.search.none", query.strip())
                : Component.translatable("gottschbooks.search.found", results.size()), NOTE, null);
        for (ManualSearch.Result result : results) {
            flow.add(Kind.BLANK, LIST_GAP);
            Line row = row(flow, result.entry());
            result.snippet().ifPresent(snippet -> {
                row.keep = LINE_H;
                ClickEvent open = new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Inline.ENTRY_LINK + result.entry().id());
                flow.text(snippet.copy().withStyle(style -> style.withClickEvent(open)), NOTE, null);
            });
        }
    }

    private Optional<RecipeView> recipe(String argument) {
        ResourceLocation id = ResourceLocation.tryParse(argument);
        if (id == null || minecraft.level == null) {
            return Optional.empty();
        }
        return minecraft.level.getRecipeManager().byKey(id)
                .flatMap(recipe -> RecipeView.of(recipe, minecraft.level.registryAccess()));
    }

    /** {@code ns:textures/....png [width height]}; the size is the texture's, 256x256 when not given */
    private Optional<ImageView> image(String argument) {
        String[] parts = argument.strip().split("\\s+");
        ResourceLocation texture = ResourceLocation.tryParse(parts[0]);
        if (texture == null || minecraft.getResourceManager().getResource(texture).isEmpty()) {
            return Optional.empty();
        }
        int texW = 256, texH = 256;
        if (parts.length >= 3) {
            try {
                texW = Integer.parseInt(parts[1]);
                texH = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                // keep 256x256
            }
        }
        int w = Math.min(textW, texW);
        return Optional.of(new ImageView(texture, texW, texH, w, Math.max(1, w * texH / Math.max(1, texW))));
    }

    private Optional<LivingEntity> entity(String argument) {
        if (minecraft.level == null) {
            return Optional.empty();
        }
        return EntityType.byString(argument.strip()).map(type -> type.create(minecraft.level))
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast);
    }

    private void paginate() {
        pages.clear();
        views.clear();
        int capacity = pageH - PAGE_PAD - FOOTER;
        List<Line> flow = flow();

        List<Line> page = new ArrayList<>();
        int used = 0;
        boolean wide = wantsOnePage();
        for (int i = 0; i < flow.size(); i++) {
            Line line = flow.get(i);
            if (line.kind == Kind.BREAK) {
                if (!page.isEmpty()) {
                    pages.add(new Page(page, wide));
                    page = new ArrayList<>();
                    used = 0;
                }
                wide = line.wide;
                continue;
            }
            // a page never starts with a gap
            if (page.isEmpty() && (line.kind == Kind.BLANK || line.kind == Kind.ORNAMENT)) {
                continue;
            }
            // headings, ornaments and spotlights keep some of what follows them, or start the next page instead
            int needed = line.height + line.keep;
            // a heading over a picture (a spotlight, recipe, image or mob) keeps the whole picture with it
            if (line.keep > 0 && (line.kind == Kind.TEXT || line.kind == Kind.HEADING) && i + 1 < flow.size() && isPicture(flow.get(i + 1))) {
                Line next = flow.get(i + 1);
                needed = line.height + next.height + next.keep;
            }
            if (!page.isEmpty() && used + needed > capacity) {
                pages.add(new Page(page, wide));
                page = new ArrayList<>();
                used = 0;
                if (line.kind == Kind.BLANK || line.kind == Kind.ORNAMENT) {
                    continue;
                }
            }
            // a table that carries on from the page before repeats its header
            if (page.isEmpty() && line.kind == Kind.TABLE_ROW && line.row > 0 && line.table.header() != null) {
                Line header = new Line(Kind.TABLE_HEADER, line.table.header().height(), line.section);
                header.table = line.table;
                header.row = -1;
                page.add(header);
                used += header.height;
            }
            page.add(line);
            used += line.height;
        }
        if (!page.isEmpty()) {
            pages.add(new Page(page, wide));
        }
        // a wide page has a view to itself; spread pages pair up, starting afresh after each wide one
        for (int p = 0; p < pages.size(); ) {
            boolean alone = !spread || pages.get(p).wide() || p + 1 >= pages.size() || pages.get(p + 1).wide();
            views.add(new View(p, alone ? 1 : 2, spread && pages.get(p).wide()));
            p += alone ? 1 : 2;
        }
        if (views.isEmpty()) {
            views.add(new View(0, 0, false));
        }
    }

    private static boolean isPicture(Line line) {
        return line.kind == Kind.SPOTLIGHT || line.kind == Kind.RECIPE || line.kind == Kind.IMAGE || line.kind == Kind.ENTITY
                || line.kind == Kind.TABLE_HEADER || line.kind == Kind.TABLE_ROW;
    }

    private int titleHeight(Component text, boolean withAuthor) {
        int titleLines = font.split(text, (int) (textW / TITLE_SCALE)).size();
        // icon (32) + gap, title lines, author line, then room before the first section
        return 36 + Math.round(titleLines * LINE_H * TITLE_SCALE) + (withAuthor ? LINE_H : 0) + 10;
    }

    private int entryTitleHeight(Component text) {
        int titleLines = font.split(text, (int) ((textW - 20) / TITLE_SCALE)).size();
        return Math.max(ROW_H, Math.round(titleLines * LINE_H * TITLE_SCALE)) + 8;
    }

    private int viewOf(int page) {
        for (int v = 0; v < views.size(); v++) {
            if (page < views.get(v).first() + Math.max(1, views.get(v).count())) {
                return v;
            }
        }
        return lastView();
    }

    private int lastView() {
        return Math.max(0, views.size() - 1);
    }

    private int pageOfSection(int section) {
        for (int p = 0; p < pages.size(); p++) {
            for (Line line : pages.get(p).lines()) {
                if (line.kind == Kind.TEXT && line.section >= section) {
                    return p;
                }
            }
        }
        return 0;
    }

    private int pageOfAnchor(String anchor) {
        for (int p = 0; p < pages.size(); p++) {
            for (Line line : pages.get(p).lines()) {
                if (anchor.equals(line.anchor)) {
                    return p;
                }
            }
        }
        return 0;
    }

    /**
     * the first page with a spotlight of {@code item} written with NBT it has (a Dungeon Map among maps), else
     * with a spotlight of the item, else with a recipe that makes it, else the first page
     */
    private int pageOfItem(ItemStack item) {
        for (int pass = 0; pass < 3; pass++) {
            Kind kind = pass < 2 ? Kind.SPOTLIGHT : Kind.RECIPE;
            for (int p = 0; p < pages.size(); p++) {
                for (Line line : pages.get(p).lines()) {
                    ItemStack shown = line.kind != kind ? ItemStack.EMPTY : kind == Kind.SPOTLIGHT ? line.item : line.recipe.result();
                    boolean specific = pass > 0 || shown.hasTag() && ItemMatch.matches(shown.getTag(), item.getTag());
                    if (shown.is(item.getItem()) && specific) {
                        return p;
                    }
                }
            }
        }
        return 0;
    }

    /** the first page with text holding one of {@code terms}, trying them in order; else the first page */
    private int pageOfText(List<String> terms) {
        for (String term : terms) {
            for (int p = 0; p < pages.size(); p++) {
                for (Line line : pages.get(p).lines()) {
                    if (TextSearch.indexOf(lineText(line), term, 0) >= 0) {
                        return p;
                    }
                }
            }
        }
        return 0;
    }

    /** a line's text, as search sees it ("" for a line without any) */
    private static String lineText(Line line) {
        StringBuilder text = new StringBuilder();
        switch (line.kind) {
            case TEXT, HEADING -> plain(line.text, text);
            case SPOTLIGHT -> text.append(line.item.getHoverName().getString());
            case ILLUSTRATION -> text.append(line.item.getHoverName().getString()).append(' ').append(line.label.getString());
            case TABLE_HEADER, TABLE_ROW -> {
                TableView.Row row = line.row < 0 ? line.table.header() : line.table.rows().get(line.row);
                row.cells().forEach(cell -> cell.lines().forEach(seq -> plain(seq, text.append(' '))));
            }
            default -> { }
        }
        return text.toString();
    }

    private static void plain(FormattedCharSequence seq, StringBuilder into) {
        seq.accept((index, style, codepoint) -> {
            into.appendCodePoint(codepoint);
            return true;
        });
    }

    private int currentSection() {
        int first = view < views.size() ? views.get(view).first() : 0;
        if (first >= pages.size()) {
            return 0;
        }
        for (Line line : pages.get(first).lines()) {
            if (line.kind == Kind.TEXT) {
                return line.section;
            }
        }
        return 0;
    }

    // ---- navigation ----

    private void turn(int by) {
        view = Math.max(0, Math.min(lastView(), view + by));
        updateButtons();
    }

    private void updateButtons() {
        back.visible = view > 0;
        forward.visible = view < lastView();
        if (searchBox != null) {
            // on the search page's first page only, over the room left for it
            searchBox.visible = SEARCH.equals(entry) && view == 0;
            if (searchBox.visible) {
                searchBox.setX(bookX + COVER_BORDER + PAGE_PAD + SEARCH_INSET_X);
                searchBox.setY(bookY + COVER_BORDER + PAGE_PAD + SEARCH_INSET_Y);
                searchBox.setWidth((views.get(0).wide() ? wideTextW : pageTextW) - 2 * SEARCH_INSET_X);
            } else if (getFocused() == searchBox) {
                setFocused(null);
            }
        }
    }

    /** opens the search page at its first page, ready to type */
    private void openSearch() {
        if (!isManual()) {
            return;
        }
        if (!SEARCH.equals(entry)) {
            history.push(new Place(entry, view));
            show(SEARCH, null, 0);
        } else if (view != 0) {
            view = 0;
            updateButtons();
        }
        focusSearch();
    }

    private void focusSearch() {
        if (searchBox != null && searchBox.visible) {
            setFocused(searchBox);
        }
    }

    /** the search box changed: search again */
    private void searched(String text) {
        query = text;
        paginate();
        view = 0;
        updateButtons();
    }

    /** opens {@code target} ({@code entry}, {@code entry#anchor}, or null for the contents), remembering where we were */
    private void go(@Nullable String target) {
        if (!isManual()) {
            return;
        }
        String to = null, anchor = null;
        if (target != null) {
            String[] parts = target.split("#", 2);
            to = parts[0].isEmpty() ? entry : parts[0];
            anchor = parts.length > 1 ? parts[1] : null;
            if (to == null || manual.entry(to).isEmpty()) {
                return;
            }
        }
        boolean fromSearch = SEARCH.equals(entry);
        history.push(new Place(entry, view));
        show(to, anchor, -1);
        // a search result opens where its words are
        if (fromSearch && anchor == null && to != null) {
            view = viewOf(pageOfText(TextSearch.terms(query)));
            updateButtons();
        }
    }

    private void goBack() {
        if (!history.isEmpty()) {
            Place place = history.pop();
            show(place.entry(), null, place.section());
        }
    }

    /** lays out {@code to}; opens at {@code anchor}, else view {@code atView}, else the first page */
    private void show(@Nullable String to, @Nullable String anchor, int atView) {
        entry = to;
        paginate();
        view = anchor != null ? viewOf(pageOfAnchor(anchor)) : Math.max(0, Math.min(atView, lastView()));
        updateButtons();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    @Override
    public boolean handleComponentClicked(@Nullable Style style) {
        if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.CHANGE_PAGE
                && style.getClickEvent().getValue().startsWith(Inline.ENTRY_LINK)) {
            go(style.getClickEvent().getValue().substring(Inline.ENTRY_LINK.length()));
            return true;
        }
        return super.handleComponentClicked(style);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (LinkHit hit : linkHits) {
                if (hit.contains(mouseX, mouseY)) {
                    switch (hit.target()) {
                        case NAV_BACK -> goBack();
                        case NAV_CONTENTS -> go(null);
                        case NAV_SEARCH -> openSearch();
                        default -> go(hit.target());
                    }
                    return true;
                }
            }
            Style style = styleAt(mouseX, mouseY);
            if (style != null && style.getClickEvent() != null && handleComponentClicked(style)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** the text style under the mouse, for links */
    @Nullable
    private Style styleAt(double mouseX, double mouseY) {
        for (TextHit hit : textHits) {
            if (mouseY >= hit.y() && mouseY < hit.y() + LINE_H - 1 && mouseX >= hit.x()) {
                return font.getSplitter().componentStyleAtWidth(hit.text(), (int) mouseX - hit.x());
            }
        }
        return null;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        // while typing a search, keys are text (Enter opens the first entry found); Escape still closes the book
        if (searchBox != null && searchBox.visible && getFocused() == searchBox && key != GLFW.GLFW_KEY_ESCAPE) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (!results.isEmpty()) {
                    go(results.get(0).entry().id());
                }
            } else {
                searchBox.keyPressed(key, scanCode, modifiers);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_F && hasControlDown() && isManual()) {
            openSearch();
            return true;
        }
        // through the buttons, for their page-turn sound; a hidden button means there's no page that way
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            if (back.visible) {
                back.onPress();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            if (forward.visible) {
                forward.onPress();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && isManual() && !history.isEmpty()) {
            goBack();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && back.visible) {
            back.onPress();
            return true;
        }
        if (delta < 0 && forward.visible) {
            forward.onPress();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (parent != null) {
            minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public void removed() {
        LAST_PLACE.put(bookId, new Place(entry, currentSection()));
        super.removed();
    }

    // ---- drawing ----

    private static final String NAV_BACK = "\u0000back", NAV_CONTENTS = "\u0000contents", NAV_SEARCH = "\u0000search";

    /** the book, drawn before the widgets so the page buttons sit on top of it */
    private void drawBook(GuiGraphics g, int mouseX, int mouseY) {
        itemHits.clear();
        linkHits.clear();
        textHits.clear();
        nineSlice(g, cover, bookX, bookY, bookW, bookH, COVER_BORDER);
        View shown = views.get(Math.min(view, views.size() - 1));
        // one wide page across the book, or a spread's two (the second blank when the spread has one page)
        int slots = shown.wide() || !spread ? 1 : 2;
        int w = shown.wide() ? bookW - 2 * COVER_BORDER : pageW;
        textW = shown.wide() ? wideTextW : pageTextW;
        for (int i = 0; i < slots; i++) {
            int x = bookX + COVER_BORDER + i * (pageW + 4);
            int y = bookY + COVER_BORDER;
            nineSlice(g, page, x, y, w, pageH, PAGE_BORDER);
            if (slots == 2) {
                gutterShadow(g, x, y, i == 0);
            }
            if (i < shown.count()) {
                int p = shown.first() + i;
                drawPage(g, pages.get(p).lines(), x + PAGE_PAD, y + PAGE_PAD, mouseX, mouseY);
                String number = String.valueOf(p + 1);
                g.drawString(font, number, x + (w - font.width(number)) / 2, y + pageH - 12, NOTE, false);
            }
        }
        if (isManual()) {
            drawNav(g, mouseX, mouseY);
        }
    }

    /** Back on the left, Search and Contents on the right, above the book */
    private void drawNav(GuiGraphics g, int mouseX, int mouseY) {
        int y = bookY - LINE_H;
        if (!history.isEmpty()) {
            navLabel(g, Component.translatable("gottschbooks.book.back"), bookX + 4, y, NAV_BACK, mouseX, mouseY);
        }
        int right = bookX + bookW - 4;
        if (entry != null) {
            Component contents = Component.translatable("gottschbooks.book.contents");
            right -= font.width(contents);
            navLabel(g, contents, right, y, NAV_CONTENTS, mouseX, mouseY);
            right -= 12;
        }
        if (!SEARCH.equals(entry)) {
            Component search = Component.translatable("gottschbooks.book.search");
            navLabel(g, search, right - font.width(search), y, NAV_SEARCH, mouseX, mouseY);
        }
    }

    private void navLabel(GuiGraphics g, Component label, int x, int y, String target, int mouseX, int mouseY) {
        LinkHit hit = new LinkHit(x - 2, y - 1, font.width(label) + 4, LINE_H, target);
        linkHits.add(hit);
        boolean hover = hit.contains(mouseX, mouseY);
        g.drawString(font, hover ? label.copy().withStyle(ChatFormatting.UNDERLINE) : label, x, y, hover ? 0xFFFFFFA0 : 0xFFFFFFFF, true);
    }

    private void drawPage(GuiGraphics g, List<Line> lines, int x, int y, int mouseX, int mouseY) {
        for (Line line : lines) {
            switch (line.kind) {
                case TEXT -> {
                    int ty = y + line.lift;
                    if (line.bullet != null) {
                        g.drawString(font, line.bullet, x, ty, line.color, false);
                    }
                    drawText(g, line.text, x + line.indent, ty, line.color);
                    textHits.add(new TextHit(x + line.indent, ty, line.text));
                }
                case HEADING -> {
                    drawHeading(g, line, x, y);
                    if (line.rule) {
                        g.fill(x, y + line.height - 2, x + textW, y + line.height - 1, NOTE & 0x60FFFFFF);
                    }
                }
                case ORNAMENT -> g.blit(ornament, x + (textW - ORNAMENT_W) / 2, y + (ORNAMENT_SLOT - ORNAMENT_H) / 2,
                        0, 0, ORNAMENT_W, ORNAMENT_H, ORNAMENT_W, ORNAMENT_H);
                case TITLE -> drawTitle(g, x, y);
                case ENTRY_TITLE -> drawEntryTitle(g, line, x, y);
                case ILLUSTRATION -> drawIllustration(g, line, x, y);
                case SPOTLIGHT -> drawSpotlight(g, line, x, y);
                case RECIPE -> drawRecipe(g, line.recipe, x, y);
                case IMAGE -> drawImage(g, line.image, x, y);
                case ENTITY -> drawEntity(g, line.entity, x, y, line.height);
                case TABLE_HEADER, TABLE_ROW -> drawTableRow(g, line, x, y);
                case CATEGORY -> drawRow(g, line, x, y, true, mouseX, mouseY);
                case LINK_ROW -> drawRow(g, line, x, y, false, mouseX, mouseY);
                case SEARCH_BOX -> {
                    // the search box's frame: a faint shade with an ink outline, like a recipe's slots
                    g.fill(x, y, x + textW, y + SEARCH_BOX_H, 0x18000000);
                    g.renderOutline(x, y, textW, SEARCH_BOX_H, NOTE & 0x80FFFFFF);
                }
                case BLANK, BREAK -> { }
            }
            y += line.height;
        }
    }

    /**
     * A heading line, drawn twice to make it bold. Vanilla bold doubles each glyph one glyph pixel to the right, which,
     * scaled up, closes the one-pixel gaps in letters such as m and w. This doubles it half a glyph pixel to the right
     * instead (at least one screen pixel), so the letters thicken and their gaps stay open.
     */
    private void drawHeading(GuiGraphics g, Line line, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(line.scale, line.scale, 1F);
        g.drawString(font, line.text, 0, 0, INK, false);
        g.pose().translate(boldOffset(line.scale), 0, 0);
        g.drawString(font, line.text, 0, 0, INK, false);
        g.pose().popPose();
    }

    /** half a glyph pixel, kept to whole screen pixels and at least one, in glyph pixels, for text drawn at {@code scale} */
    private float boldOffset(float scale) {
        // screen pixels per glyph pixel: a whole number, since the book's text scales are kept to whole screen pixels
        int glyphPixel = Math.max(1, Math.round((float) minecraft.getWindow().getGuiScale() * scale));
        return Math.max(1, glyphPixel / 2) / (float) glyphPixel;
    }

    /**
     * A line of text with its bold runs thickened by {@link #boldOffset} rather than vanilla's whole glyph pixel, which
     * fills in the gaps of letters such as m and w at most GUI scales. Each character still sits exactly where vanilla
     * puts it, bold's wider spacing included, so wrapping and link positions are unchanged. A line with no bold is
     * drawn in one go.
     */
    private void drawText(GuiGraphics g, FormattedCharSequence text, int x, int y, int color) {
        boolean[] bold = {false};
        text.accept((index, style, codepoint) -> {
            bold[0] |= style.isBold();
            return !bold[0];
        });
        if (!bold[0]) {
            g.drawString(font, text, x, y, color, false);
            return;
        }
        float offset = boldOffset(1F);
        float[] at = {x};
        text.accept((index, style, codepoint) -> {
            FormattedCharSequence glyph = FormattedCharSequence.codepoint(codepoint, style.withBold(false));
            g.pose().pushPose();
            g.pose().translate(at[0], y, 0);
            g.drawString(font, glyph, 0, 0, color, false);
            if (style.isBold()) {
                g.pose().translate(offset, 0, 0);
                g.drawString(font, glyph, 0, 0, color, false);
            }
            g.pose().popPose();
            at[0] += font.getSplitter().stringWidth(FormattedCharSequence.codepoint(codepoint, style));
            return true;
        });
    }

    private void drawTitle(GuiGraphics g, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x + textW / 2F - 16, y, 0);
        g.pose().scale(2F, 2F, 1F);
        g.renderItem(icon, 0, 0);
        g.pose().popPose();
        y += 36;

        g.pose().pushPose();
        g.pose().translate(x + textW / 2F, y, 0);
        g.pose().scale(TITLE_SCALE, TITLE_SCALE, 1F);
        int titleW = (int) (textW / TITLE_SCALE);
        int row = 0;
        for (FormattedCharSequence seq : font.split(title, titleW)) {
            g.drawString(font, seq, -font.width(seq) / 2, row * LINE_H, INK, false);
            row++;
        }
        g.pose().popPose();
        y += Math.round(row * LINE_H * TITLE_SCALE);

        int by = y;
        author.ifPresent(name -> {
            Component line = Component.translatable("gottschbooks.book.author", name).withStyle(Style.EMPTY.withItalic(true));
            g.drawString(font, line, x + (textW - font.width(line)) / 2, by, NOTE, false);
        });
    }

    /** an entry's name, larger, after its icon */
    private void drawEntryTitle(GuiGraphics g, Line line, int x, int y) {
        int textX = x;
        if (!line.item.isEmpty()) {
            g.renderItem(line.item, x, y);
            itemHits.add(new ItemHit(x, y, 16, line.item));
            textX += 20;
        }
        g.pose().pushPose();
        g.pose().translate(textX, y + 2, 0);
        g.pose().scale(TITLE_SCALE, TITLE_SCALE, 1F);
        int row = 0;
        for (FormattedCharSequence seq : font.split(line.label, (int) ((textW - (textX - x)) / TITLE_SCALE))) {
            g.drawString(font, seq, 0, row * LINE_H, INK, false);
            row++;
        }
        g.pose().popPose();
        g.fill(x, y + line.height - 4, x + textW, y + line.height - 3, NOTE & 0x80FFFFFF);
    }

    /** a category heading (bold) or an entry row (a link) on the contents */
    private void drawRow(GuiGraphics g, Line line, int x, int y, boolean heading, int mouseX, int mouseY) {
        int textX = x;
        if (!line.item.isEmpty()) {
            g.renderItem(line.item, x, y + 1);
            textX += 20;
        }
        Component label = line.label;
        boolean hover = false;
        if (!heading) {
            LinkHit hit = new LinkHit(x, y, textW, ROW_H, line.target);
            linkHits.add(hit);
            hover = hit.contains(mouseX, mouseY);
        } else {
            label = Component.empty().withStyle(ChatFormatting.BOLD).append(label);
        }
        String shown = font.plainSubstrByWidth(label.getString(), textW - (textX - x));
        Component text = Component.literal(shown).withStyle(label.getStyle());
        if (hover) {
            text = text.copy().withStyle(ChatFormatting.UNDERLINE);
        }
        drawText(g, (heading ? Component.empty().withStyle(ChatFormatting.BOLD).append(shown) : text).getVisualOrderText(),
                textX, y + (ROW_H - 8) / 2 + 1, hover ? LINK : INK);
    }

    private void drawIllustration(GuiGraphics g, Line line, int x, int y) {
        int ix = x + (textW - illustrationSize) / 2;
        int iy = y + 6;
        g.pose().pushPose();
        g.pose().translate(ix, iy, 0);
        g.pose().scale(illustrationScale, illustrationScale, 1F);
        g.renderItem(line.item, 0, 0);
        g.pose().popPose();
        itemHits.add(new ItemHit(ix, iy, illustrationSize, line.item));
        int cy = iy + illustrationSize + 3;
        for (FormattedCharSequence seq : font.split(line.label, textW)) {
            g.drawString(font, seq, x + (textW - font.width(seq)) / 2, cy, NOTE, false);
            cy += LINE_H;
        }
    }

    /** a big item, centred in a frame */
    private void drawSpotlight(GuiGraphics g, Line line, int x, int y) {
        int size = 32;
        int ix = x + (textW - size) / 2;
        int iy = y + 4;
        g.fill(ix - 3, iy - 3, ix + size + 3, iy + size + 3, SLOT);
        g.pose().pushPose();
        g.pose().translate(ix, iy, 0);
        g.pose().scale(2F, 2F, 1F);
        g.renderItem(line.item, 0, 0);
        g.pose().popPose();
        itemHits.add(new ItemHit(ix, iy, size, line.item));
    }

    /**
     * the input slots, the station under them, an arrow (with the note under it) and the result; ingredients that
     * accept several items cycle through them
     */
    private void drawRecipe(GuiGraphics g, RecipeView recipe, int x, int y) {
        int columns = recipe.columns(), gridW = columns * ROW_H, gridH = recipe.rows() * ROW_H, total = gridW + 22 + ROW_H;
        int gx = x + (textW - total) / 2, gy = y + 4;
        long tick = System.currentTimeMillis() / 1000;
        for (int cell = 0; cell < recipe.slots().size(); cell++) {
            int cx = gx + (cell % columns) * ROW_H, cy = gy + (cell / columns) * ROW_H;
            g.fill(cx, cy, cx + ROW_H - 1, cy + ROW_H - 1, SLOT);
            ItemStack[] options = recipe.slots().get(cell);
            if (options != null && options.length > 0) {
                ItemStack item = options[(int) (tick % options.length)];
                g.renderItem(item, cx + 1, cy + 1);
                itemHits.add(new ItemHit(cx + 1, cy + 1, 16, item));
            }
        }
        if (!recipe.station().isEmpty()) {
            // under the inputs, where a furnace's fuel would go
            int sx = gx + (gridW - 16) / 2, sy = gy + gridH + 1;
            g.renderItem(recipe.station(), sx, sy);
            itemHits.add(new ItemHit(sx, sy, 16, recipe.station()));
        }
        int middle = gy + (gridH - ROW_H) / 2;
        int ax = gx + gridW + 5, ay = middle + 4;
        g.drawString(font, "→", ax, ay, INK, false);
        if (recipe.note() != null) {
            int noteX = gx + gridW + 11 - font.width(recipe.note()) / 2;
            g.drawString(font, recipe.note(), noteX, middle + ROW_H + 5, NOTE, false);
        }
        int rx = gx + gridW + 22, ry = middle;
        g.fill(rx, ry, rx + ROW_H - 1, ry + ROW_H - 1, SLOT);
        g.renderItem(recipe.result(), rx + 1, ry + 1);
        g.renderItemDecorations(font, recipe.result(), rx + 1, ry + 1);
        itemHits.add(new ItemHit(rx + 1, ry + 1, 16, recipe.result()));
    }

    /**
     * a table's header (bold, with a faint rule under it) or one of its rows (every other one faintly shaded); each cell
     * aligned in its column, its icons with their tooltips, its text with its links
     */
    private void drawTableRow(GuiGraphics g, Line line, int x, int y) {
        TableView table = line.table;
        boolean header = line.row < 0;
        TableView.Row row = header ? table.header() : table.rows().get(line.row);
        if (header) {
            g.fill(x, y + row.height() - 1, x + table.width(), y + row.height(), NOTE & 0x60FFFFFF);
        } else if (line.row % 2 == 1) {
            g.fill(x, y, x + table.width(), y + row.height(), 0x0C000000);
        }
        for (int c = 0; c < row.cells().size(); c++) {
            TableView.Cell cell = row.cells().get(c);
            Block.Table.Align align = table.align().get(c);
            int blockX = x + table.xs()[c] + TableView.offset(align, table.widths()[c] - cell.blockW());
            int top = y + TableView.ROW_PAD;
            int iconX = cell.beside() ? blockX : blockX + TableView.offset(align, cell.blockW() - cell.iconsW());
            for (ItemStack icon : cell.icons()) {
                g.renderItem(icon, iconX, top);
                g.renderItemDecorations(font, icon, iconX, top);
                itemHits.add(new ItemHit(iconX, top, TableView.ICON, icon));
                iconX += TableView.ICON_STEP;
            }
            int areaX = cell.beside() ? blockX + cell.iconsW() + TableView.ICON_GAP : blockX;
            int areaW = cell.beside() ? cell.textW() : cell.blockW();
            for (int i = 0; i < cell.lines().size(); i++) {
                FormattedCharSequence text = cell.lines().get(i);
                int lx = areaX + TableView.offset(align, areaW - font.width(text));
                int ly = top + cell.lineY()[i];
                drawText(g, text, lx, ly, INK);
                textHits.add(new TextHit(lx, ly, text));
            }
        }
    }

    private void drawImage(GuiGraphics g, ImageView image, int x, int y) {
        g.blit(image.texture(), x + (textW - image.w()) / 2, y + 4, image.w(), image.h(), 0, 0, image.texW(), image.texH(),
                image.texW(), image.texH());
    }

    private void drawEntity(GuiGraphics g, LivingEntity entity, int x, int y, int height) {
        float size = Math.max(entity.getBbHeight(), entity.getBbWidth());
        int scale = (int) Math.max(8, (height - 14) / Math.max(1F, size));
        // a slow turn, so the whole mob shows; 1.20.1 draws it standing on (x, y)
        float angle = (float) Math.sin(System.currentTimeMillis() / 1500.0) * 1.5F;
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x + textW / 2, y + height - 4, scale, angle, 0F, entity);
    }

    /**
     * 1.20.1's {@code Screen.render} draws only the widgets (1.21 also draws the background), so the darkened
     * background and the book come first here; the items' tooltips go over everything else.
     */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        drawBook(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
        for (ItemHit hit : itemHits) {
            if (hit.contains(mouseX, mouseY)) {
                g.renderTooltip(font, hit.item(), mouseX, mouseY);
                return;
            }
        }
        Style style = styleAt(mouseX, mouseY);
        if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) {
            g.renderTooltip(font, Component.literal(style.getClickEvent().getValue()), mouseX, mouseY);
        }
    }

    /** a darker band along the spine, so the two pages read as one bound book */
    private void gutterShadow(GuiGraphics g, int x, int y, boolean left) {
        for (int i = 0; i < 6; i++) {
            int alpha = (6 - i) * 10;
            int col = left ? x + pageW - 1 - i : x + i;
            g.fill(col, y + 1, col + 1, y + pageH - 1, alpha << 24);
        }
    }

    /** draws a 32x32 texture as a nine-slice: corners as they are, edges and centre tiled (never stretched) */
    private static void nineSlice(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int b) {
        int inner = TEX - 2 * b;
        tile(g, tex, x + b, y + b, w - 2 * b, h - 2 * b, b, b, inner, inner);
        tile(g, tex, x + b, y, w - 2 * b, b, b, 0, inner, b);
        tile(g, tex, x + b, y + h - b, w - 2 * b, b, b, TEX - b, inner, b);
        tile(g, tex, x, y + b, b, h - 2 * b, 0, b, b, inner);
        tile(g, tex, x + w - b, y + b, b, h - 2 * b, TEX - b, b, b, inner);
        g.blit(tex, x, y, 0, 0, b, b, TEX, TEX);
        g.blit(tex, x + w - b, y, TEX - b, 0, b, b, TEX, TEX);
        g.blit(tex, x, y + h - b, 0, TEX - b, b, b, TEX, TEX);
        g.blit(tex, x + w - b, y + h - b, TEX - b, TEX - b, b, b, TEX, TEX);
    }

    private static void tile(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int u, int v, int uw, int vh) {
        for (int dy = 0; dy < h; dy += vh) {
            for (int dx = 0; dx < w; dx += uw) {
                g.blit(tex, x + dx, y + dy, u, v, Math.min(uw, w - dx), Math.min(vh, h - dy), TEX, TEX);
            }
        }
    }
}
