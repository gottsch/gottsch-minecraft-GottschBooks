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
package mod.gottsch.neo.gottschbooks.client;

import mod.gottsch.neo.gottschbooks.api.Illustrations;
import mod.gottsch.neo.gottschbooks.core.text.Block;
import mod.gottsch.neo.gottschbooks.core.text.BookText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The book reader, lore-book mode: an open book sized to the window, two pages side by side (one on a narrow window).
 * <p>
 * The text is flowed onto as many pages as the window needs, so a big window shows more per page and no text, in any
 * language, can run off a page. {@code ---} sections are separated by an ornament, which never ends a page and always
 * has two lines of its section after it. The first page opens with the book's icon, title and author.
 * <p>
 * Illustrations are items drawn at about 1.5x (kept to whole screen pixels), captioned with the caption or, without
 * one, the item's name; hovering shows the item's tooltip. {@code >} paragraphs are editor's notes: italic and grey.
 * The reader remembers the section you were on for each book until the game closes.
 * <p>
 * Moved from Magic Treasures' {@code LoreBookScreen}.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public class BookScreen extends Screen {
    private static final int TEX = 32, COVER_BORDER = 8, PAGE_BORDER = 5;
    private static final int ORNAMENT_W = 21, ORNAMENT_H = 5;

    private static final int INK = 0xFF3B2A1A;
    private static final int NOTE = 0xFF7A6650;
    private static final int LINE_H = 10;
    private static final int ORNAMENT_SLOT = 16;
    private static final int MARGIN = 12;
    private static final int MAX_BOOK_W = 520, MAX_BOOK_H = 340;
    /** below this width a spread's pages get too narrow to read, so the book shows one page */
    private static final int MIN_SPREAD_W = 320;
    private static final int PAGE_PAD = 12;
    private static final int FOOTER = 18;
    private static final float TITLE_SCALE = 1.5F;
    /** illustrations aim for this, rounded so each item pixel covers whole screen pixels (see {@link #init}) */
    private static final double ILLUSTRATION_TARGET = 1.5;

    /** the section each book was left open at, for this game session */
    private static final Map<ResourceLocation, Integer> LAST_SECTION = new HashMap<>();

    private final ResourceLocation bookId;
    private final ItemStack icon;
    private final Optional<Component> author;
    /** the text's blocks, less a level-1 heading used as the title */
    private final List<Block> blocks;
    private final ResourceLocation cover, page, ornament;

    // layout, recomputed in init() (it runs again when the window is resized)
    private int bookX, bookY, bookW, bookH;
    private boolean spread;
    private int pageW, pageH, textW;
    private float illustrationScale;
    private int illustrationSize;
    private final List<List<Line>> pages = new ArrayList<>();
    private int view;
    private PageButton back, forward;
    /** where the visible illustrations were drawn this frame, for their tooltips */
    private final List<Hit> hits = new ArrayList<>();

    /** one row of a page: text, a blank, an ornament, a picture, a forced break, or the title block (first page only) */
    private record Line(Kind kind, FormattedCharSequence text, int color, int section, int height, ItemStack item,
                        Component caption) {
        Line(Kind kind, FormattedCharSequence text, int color, int section, int height) {
            this(kind, text, color, section, height, ItemStack.EMPTY, null);
        }
    }

    private enum Kind { TEXT, BLANK, ORNAMENT, TITLE, ILLUSTRATION, BREAK }

    private record Hit(int x, int y, int size, ItemStack item) {
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + size && my >= y && my < y + size;
        }
    }

    public BookScreen(LoadedBook book, BookText text, ItemStack icon) {
        super(title(book, text));
        this.bookId = book.id();
        this.icon = icon;
        this.author = text.get("author").map(Component::literal);
        List<Block> body = new ArrayList<>(text.blocks());
        if (text.get("title").isEmpty() && !body.isEmpty() && body.getFirst() instanceof Block.Heading h && h.level() == 1) {
            body.removeFirst();
        }
        this.blocks = body;
        ResourceLocation skin = book.skin();
        this.cover = art(skin, "cover");
        this.page = art(skin, "page");
        this.ornament = art(skin, "ornament");
    }

    /** {@code title:} in the front matter, else a leading {@code # heading}, else the book's id */
    private static Component title(LoadedBook book, BookText text) {
        Optional<Component> title = text.get("title").map(Component::literal);
        if (title.isEmpty() && !text.blocks().isEmpty() && text.blocks().getFirst() instanceof Block.Heading h && h.level() == 1) {
            title = Optional.of(h.text());
        }
        return title.orElseGet(() -> Component.literal(book.id().toString()));
    }

    private static ResourceLocation art(ResourceLocation skin, String name) {
        return skin.withSuffix("/" + name + ".png");
    }

    @Override
    protected void init() {
        // where the reader was: the old layout's section, else the remembered one
        int anchor = pages.isEmpty() ? LAST_SECTION.getOrDefault(bookId, 0) : currentSection();

        bookW = Math.min(width - 2 * MARGIN, MAX_BOOK_W);
        bookH = Math.min(height - 2 * MARGIN, MAX_BOOK_H);
        spread = bookW >= MIN_SPREAD_W;
        if (!spread) {
            bookW = Math.min(bookW, MIN_SPREAD_W / 2 + 40);
        }
        bookX = (width - bookW) / 2;
        bookY = (height - bookH) / 2;
        pageW = spread ? (bookW - 2 * COVER_BORDER - 4) / 2 : bookW - 2 * COVER_BORDER;
        pageH = bookH - 2 * COVER_BORDER;
        textW = pageW - 2 * PAGE_PAD;

        // 1.5x of a GUI pixel is 4.5 screen pixels at GUI scale 3, which draws item pixels unevenly; round to whole
        // screen pixels instead (exactly 1.5x at scales 2 and 4, 1.67x at 3)
        double guiScale = minecraft.getWindow().getGuiScale();
        illustrationScale = (float) (Math.max(1, Math.round(ILLUSTRATION_TARGET * guiScale)) / guiScale);
        illustrationSize = (int) Math.ceil(16 * illustrationScale);

        paginate();
        view = Math.min(viewOf(pageOfSection(anchor)), lastView());

        int buttonY = bookY + bookH - COVER_BORDER - 16;
        back = addRenderableWidget(new PageButton(bookX + COVER_BORDER + 6, buttonY, false, b -> turn(-1), true));
        forward = addRenderableWidget(new PageButton(bookX + bookW - COVER_BORDER - 6 - 23, buttonY, true, b -> turn(1), true));
        updateButtons();
    }

    // ---- layout ----

    /** the blocks as one column of lines; sections are counted by ornaments */
    private List<Line> flow() {
        List<Line> flow = new ArrayList<>();
        flow.add(new Line(Kind.TITLE, null, INK, 0, titleHeight()));
        int section = 0;
        boolean textBefore = false;
        for (Block block : blocks) {
            switch (block) {
                case Block.Ornament o -> {
                    section++;
                    flow.add(new Line(Kind.ORNAMENT, null, INK, section, ORNAMENT_SLOT));
                    textBefore = false;
                }
                case Block.Paragraph p -> {
                    addText(flow, p.quote() ? Component.empty().withStyle(ChatFormatting.ITALIC).append(p.text()) : p.text(),
                            p.quote() ? NOTE : INK, section, textBefore);
                    textBefore = true;
                }
                case Block.Heading h -> {
                    addText(flow, Component.empty().withStyle(ChatFormatting.BOLD).append(h.text()), INK, section, textBefore);
                    textBefore = true;
                }
                case Block.Illustration i -> {
                    Optional<ItemStack> item = Illustrations.get(i.scheme(), i.id());
                    if (item.isPresent()) {
                        Component caption = Component.literal(i.caption().isEmpty() ? item.get().getHoverName().getString()
                                : i.caption()).withStyle(ChatFormatting.ITALIC);
                        int captionLines = font.split(caption, textW).size();
                        flow.add(new Line(Kind.ILLUSTRATION, null, NOTE, section,
                                6 + illustrationSize + 3 + captionLines * LINE_H + 6, item.get(), caption));
                    }
                }
                case Block.PageBreak b -> {
                    flow.add(new Line(Kind.BREAK, null, INK, section, 0));
                    textBefore = false;
                }
            }
        }
        return flow;
    }

    /** a paragraph's lines, after a blank line when text came before it in the section */
    private void addText(List<Line> flow, Component text, int color, int section, boolean textBefore) {
        if (textBefore) {
            flow.add(new Line(Kind.BLANK, null, color, section, LINE_H));
        }
        for (FormattedCharSequence seq : font.split(text, textW)) {
            flow.add(new Line(Kind.TEXT, seq, color, section, LINE_H));
        }
    }

    private void paginate() {
        pages.clear();
        int capacity = pageH - PAGE_PAD - FOOTER;
        List<Line> flow = flow();

        List<Line> page = new ArrayList<>();
        int used = 0;
        for (Line line : flow) {
            if (line.kind == Kind.BREAK) {
                if (!page.isEmpty()) {
                    pages.add(page);
                    page = new ArrayList<>();
                    used = 0;
                }
                continue;
            }
            // a page never starts with a gap
            if (page.isEmpty() && (line.kind == Kind.BLANK || line.kind == Kind.ORNAMENT)) {
                continue;
            }
            // an ornament needs two lines of its section after it, or it starts the next page instead
            int needed = line.height + (line.kind == Kind.ORNAMENT ? 2 * LINE_H : 0);
            if (!page.isEmpty() && used + needed > capacity) {
                pages.add(page);
                page = new ArrayList<>();
                used = 0;
                if (line.kind == Kind.BLANK || line.kind == Kind.ORNAMENT) {
                    continue;
                }
            }
            page.add(line);
            used += line.height;
        }
        if (!page.isEmpty()) {
            pages.add(page);
        }
    }

    private int titleHeight() {
        int titleLines = font.split(title, (int) (textW / TITLE_SCALE)).size();
        // icon (32) + gap, title lines, author line, then room before the first section
        return 36 + Math.round(titleLines * LINE_H * TITLE_SCALE) + (author.isPresent() ? LINE_H : 0) + 10;
    }

    private int perView() {
        return spread ? 2 : 1;
    }

    private int viewOf(int page) {
        return page / perView();
    }

    private int lastView() {
        return Math.max(0, (pages.size() - 1) / perView());
    }

    private int pageOfSection(int section) {
        for (int p = 0; p < pages.size(); p++) {
            for (Line line : pages.get(p)) {
                if (line.kind == Kind.TEXT && line.section >= section) {
                    return p;
                }
            }
        }
        return 0;
    }

    private int currentSection() {
        int first = view * perView();
        if (first >= pages.size()) {
            return 0;
        }
        for (Line line : pages.get(first)) {
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
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
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
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && back.visible) {
            back.onPress();
            return true;
        }
        if (scrollY < 0 && forward.visible) {
            forward.onPress();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        LAST_SECTION.put(bookId, currentSection());
        super.removed();
    }

    // ---- drawing ----

    /** the book is drawn as the background, so the page buttons sit on top of it */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        hits.clear();
        nineSlice(g, cover, bookX, bookY, bookW, bookH, COVER_BORDER);
        int first = view * perView();
        for (int i = 0; i < perView(); i++) {
            int x = bookX + COVER_BORDER + i * (pageW + 4);
            int y = bookY + COVER_BORDER;
            nineSlice(g, page, x, y, pageW, pageH, PAGE_BORDER);
            if (spread) {
                gutterShadow(g, x, y, i == 0);
            }
            if (first + i < pages.size()) {
                drawPage(g, pages.get(first + i), x + PAGE_PAD, y + PAGE_PAD);
                String number = String.valueOf(first + i + 1);
                g.drawString(font, number, x + (pageW - font.width(number)) / 2, y + pageH - 12, NOTE, false);
            }
        }
    }

    private void drawPage(GuiGraphics g, List<Line> lines, int x, int y) {
        for (Line line : lines) {
            switch (line.kind) {
                case TEXT -> g.drawString(font, line.text, x, y, line.color, false);
                case ORNAMENT -> g.blit(ornament, x + (textW - ORNAMENT_W) / 2, y + (ORNAMENT_SLOT - ORNAMENT_H) / 2,
                        0, 0, ORNAMENT_W, ORNAMENT_H, ORNAMENT_W, ORNAMENT_H);
                case TITLE -> drawTitle(g, x, y);
                case ILLUSTRATION -> drawIllustration(g, line, x, y);
                case BLANK, BREAK -> { }
            }
            y += line.height;
        }
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

    private void drawIllustration(GuiGraphics g, Line line, int x, int y) {
        int ix = x + (textW - illustrationSize) / 2;
        int iy = y + 6;
        g.pose().pushPose();
        g.pose().translate(ix, iy, 0);
        g.pose().scale(illustrationScale, illustrationScale, 1F);
        g.renderItem(line.item, 0, 0);
        g.pose().popPose();
        hits.add(new Hit(ix, iy, illustrationSize, line.item));
        int cy = iy + illustrationSize + 3;
        for (FormattedCharSequence seq : font.split(line.caption, textW)) {
            g.drawString(font, seq, x + (textW - font.width(seq)) / 2, cy, NOTE, false);
            cy += LINE_H;
        }
    }

    /** the illustrations' tooltips, over everything else */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        for (Hit hit : hits) {
            if (hit.contains(mouseX, mouseY)) {
                g.renderTooltip(font, hit.item, mouseX, mouseY);
                break;
            }
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
