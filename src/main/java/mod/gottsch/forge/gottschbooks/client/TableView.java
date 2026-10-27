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
import mod.gottsch.forge.gottschbooks.core.text.ItemReference;
import mod.gottsch.forge.gottschbooks.core.text.TableWidths;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A table laid out for a page's width: each column's width and position ({@link TableWidths}), and each row's cells,
 * wrapped to them. A cell's icons sit to the left of its text when there is room for the text's longest word beside
 * them, else above it. A row is as tall as its tallest cell, and a line in a picture font gets the room its glyphs
 * need, as in a paragraph.
 *
 * @param header null when the table has none
 *
 * @author Mark Gottschling on 10/27/2026
 */
record TableView(int[] widths, int[] xs, int width, List<Block.Table.Align> align, @Nullable Row header, List<Row> rows) {
    static final int ICON = 16, ICON_STEP = 18, ICON_GAP = 4, COLUMN_GAP = 6, ROW_PAD = 2;

    /** a row: its cells, and its height with the padding above and below */
    record Row(List<Cell> cells, int height) {}

    /**
     * a cell, laid out: its icons, whether its text sits beside them, its text's lines and where each is drawn (from
     * the row's padding), the widest line, and the width of everything together
     */
    record Cell(List<ItemStack> icons, boolean beside, List<FormattedCharSequence> lines, int[] lineY, int textW, int blockW) {
        int iconsW() {
            return icons.isEmpty() ? 0 : icons.size() * ICON_STEP - (ICON_STEP - ICON);
        }
    }

    /** a cell ready to measure: its icons, found, and its text, with a note for any icon that isn't */
    private record Prepared(List<ItemStack> icons, Component text) {
        int iconsW() {
            return icons.isEmpty() ? 0 : icons.size() * ICON_STEP - (ICON_STEP - ICON);
        }
    }

    /**
     * @param lineH    a text line's height
     * @param note     the colour of the note shown for an icon that can't be drawn
     * @param overhang how far a line's glyphs reach above and below the usual line, as BookScreen measures it
     */
    static TableView of(Block.Table table, Font font, int available, int lineH, int note,
                        Function<FormattedCharSequence, int[]> overhang) {
        int columns = table.align().size();
        List<Prepared> header = table.header().isEmpty() ? null
                : table.header().stream().map(cell -> prepare(cell, note, true)).toList();
        List<List<Prepared>> rows = table.rows().stream()
                .map(row -> row.stream().map(cell -> prepare(cell, note, false)).toList()).toList();

        int[] natural = new int[columns], min = new int[columns];
        List<List<Prepared>> all = new ArrayList<>(rows);
        if (header != null) {
            all.add(header);
        }
        for (List<Prepared> row : all) {
            for (int c = 0; c < columns; c++) {
                Prepared cell = row.get(c);
                boolean text = !cell.text().getString().isEmpty();
                natural[c] = Math.max(natural[c], cell.iconsW() + (text ? (cell.icons().isEmpty() ? 0 : ICON_GAP) + font.width(cell.text()) : 0));
                min[c] = Math.max(min[c], Math.max(cell.iconsW(), longestWord(font, cell.text())));
            }
        }
        int[] widths = TableWidths.columns(natural, min, available, COLUMN_GAP);
        int[] xs = new int[columns];
        for (int c = 1; c < columns; c++) {
            xs[c] = xs[c - 1] + widths[c - 1] + COLUMN_GAP;
        }
        int width = columns == 0 ? 0 : xs[columns - 1] + widths[columns - 1];
        return new TableView(widths, xs, width, table.align(),
                header == null ? null : row(header, widths, font, lineH, overhang),
                rows.stream().map(row -> row(row, widths, font, lineH, overhang)).toList());
    }

    private static Prepared prepare(Block.Table.Cell cell, int note, boolean header) {
        List<ItemStack> icons = new ArrayList<>();
        MutableComponent text = Component.empty();
        for (ItemReference icon : cell.icons()) {
            Optional<ItemStack> stack = Icons.stack(icon);
            if (stack.isPresent()) {
                icons.add(stack.get());
            } else {
                text.append(Component.literal("[item " + icon.text() + "?] ").withStyle(style -> style.withColor(note)));
            }
        }
        text.append(cell.text());
        return new Prepared(List.copyOf(icons), header ? Component.empty().withStyle(ChatFormatting.BOLD).append(text) : text);
    }

    private static Row row(List<Prepared> cells, int[] widths, Font font, int lineH, Function<FormattedCharSequence, int[]> overhang) {
        List<Cell> laid = new ArrayList<>();
        int height = 0;
        for (int c = 0; c < cells.size(); c++) {
            Cell cell = cell(cells.get(c), widths[c], font, lineH, overhang);
            laid.add(cell);
            int textBottom = cell.lines().isEmpty() ? 0 : cell.lineY()[cell.lines().size() - 1] + lineH;
            int iconBottom = cell.icons().isEmpty() ? 0 : ICON;
            height = Math.max(height, Math.max(textBottom, iconBottom));
        }
        return new Row(List.copyOf(laid), height + 2 * ROW_PAD);
    }

    private static Cell cell(Prepared cell, int width, Font font, int lineH, Function<FormattedCharSequence, int[]> overhang) {
        boolean hasText = !cell.text().getString().isEmpty();
        int iconsW = cell.iconsW();
        int beside = width - iconsW - ICON_GAP;
        boolean isBeside = !cell.icons().isEmpty() && hasText && beside >= Math.max(1, longestWord(font, cell.text()));
        List<FormattedCharSequence> lines = hasText ? font.split(cell.text(), Math.max(1, isBeside ? beside : width)) : List.of();
        int[] lineY = new int[lines.size()];
        int y = 0, textW = 0;
        for (int i = 0; i < lines.size(); i++) {
            int[] reach = overhang.apply(lines.get(i));
            lineY[i] = y + reach[0];
            y += lineH + reach[0] + reach[1];
            textW = Math.max(textW, font.width(lines.get(i)));
        }
        // text beside the icons: a short text is centred on them, a long one starts at their top
        int shift = isBeside ? Math.max(0, (ICON - y + 2) / 2) : cell.icons().isEmpty() ? 0 : ICON + 2;
        for (int i = 0; i < lineY.length; i++) {
            lineY[i] += shift;
        }
        int blockW = isBeside ? iconsW + ICON_GAP + textW : Math.max(iconsW, textW);
        return new Cell(cell.icons(), isBeside, lines, lineY, textW, blockW);
    }

    /** the widest word in {@code text}, each measured in its own style */
    private static int longestWord(Font font, Component text) {
        int[] widest = {0};
        text.visit((style, run) -> {
            for (String word : run.split(" ")) {
                widest[0] = Math.max(widest[0], font.width(FormattedText.of(word, style)));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return widest[0];
    }

    /** how far to move something {@code extra} narrower than its space, for an alignment */
    static int offset(Block.Table.Align align, int extra) {
        return switch (align) {
            case LEFT -> 0;
            case CENTER -> extra / 2;
            case RIGHT -> extra;
        };
    }
}
