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

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * A text box that looks written on the page: no border or background (the page draws a frame round it), and its text
 * in {@code color} without the drop shadow 1.20.1's {@link EditBox} always draws (1.21.1's can turn it off). Otherwise
 * drawn as vanilla draws a box without a border: the text scrolled to the cursor, the blinking cursor, the selection,
 * and the hint while empty and unfocused. Reads {@code displayPos}, {@code highlightPos} and {@code frame}, opened by
 * the access transformer.
 *
 * @author Mark Gottschling on 10/28/2026
 */
final class PageEditBox extends EditBox {
    private static final int SELECTION = 0xFF0000FF;

    private final Font font;
    private final int color;
    private int maxLength = 32;
    @Nullable
    private Component hint;

    PageEditBox(Font font, int x, int y, int width, int height, Component message, int color) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.color = color;
        setBordered(false);
        setTextColor(color);
    }

    @Override
    public void setMaxLength(int length) {
        super.setMaxLength(length);
        maxLength = length;
    }

    @Override
    public void setHint(Component hint) {
        super.setHint(hint);
        this.hint = hint;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) {
            return;
        }
        String value = getValue();
        int cursor = getCursorPosition() - displayPos;
        String shown = font.plainSubstrByWidth(value.substring(Math.min(displayPos, value.length())), getInnerWidth());
        int highlight = Math.min(highlightPos - displayPos, shown.length());
        boolean cursorShown = cursor >= 0 && cursor <= shown.length();
        int x = getX(), y = getY();

        // without a shadow, drawString returns exactly where the text ends
        int end = x;
        if (!shown.isEmpty()) {
            end = g.drawString(font, cursorShown ? shown.substring(0, cursor) : shown, x, y, color, false);
        }
        boolean insert = getCursorPosition() < value.length() || value.length() >= maxLength;
        int cursorX = !cursorShown ? (cursor > 0 ? x + width : x) : insert ? end - 1 : end;
        if (!shown.isEmpty() && cursorShown && cursor < shown.length()) {
            g.drawString(font, shown.substring(cursor), end, y, color, false);
        }
        if (hint != null && shown.isEmpty() && !isFocused()) {
            g.drawString(font, hint, x, y, color, false);
        }
        if (isFocused() && frame / 6 % 2 == 0 && cursorShown) {
            if (insert) {
                g.fill(cursorX, y - 1, cursorX + 1, y + 1 + font.lineHeight, color);
            } else {
                g.drawString(font, "_", cursorX, y, color, false);
            }
        }
        if (highlight != cursor) {
            int to = x + font.width(shown.substring(0, Math.max(0, highlight))) - 1;
            int left = Math.max(x, Math.min(cursorX, to)), right = Math.min(x + width, Math.max(cursorX, to));
            g.fill(RenderType.guiTextHighlight(), left, y - 1, right, y + 1 + font.lineHeight, SELECTION);
        }
    }
}
