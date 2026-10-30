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

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * "New in <manual>": a toast, drawn like an advancement's, naming the manual and the entry an advancement just
 * unlocked (or how many, when several in one book unlock at once). While it shows, its second line takes turns with
 * "Press [B] to read", and {@link UnlockToasts} opens the entry when the player presses the key.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public final class UnlockToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    /** how long it shows, before the player's notification time setting */
    private static final long DISPLAY_MILLIS = 8000;
    /** how long the second line shows the entry, then the key, in turn */
    private static final long LINE_MILLIS = 2000;
    private static final int TEXT_X = 30, TEXT_W = 125;
    private static final int TITLE_COLOR = 0xFFFFFF00, TEXT_COLOR = 0xFFFFFFFF;

    private final ResourceLocation book;
    private final String entry;
    private final ItemStack icon;
    private final Component title;
    private final Component detail;
    @Nullable
    private final Component hint;
    /** when it was last drawn; it counts as showing while it is drawn */
    private long drawnAt;
    private boolean gone;

    /**
     * @param entry the entry the key opens
     * @param hint  the key's line, or null when the key is unbound
     */
    public UnlockToast(ResourceLocation book, String entry, ItemStack icon, Component title, Component detail,
                       @Nullable Component hint) {
        this.book = book;
        this.entry = entry;
        this.icon = icon;
        this.title = title;
        this.detail = detail;
        this.hint = hint;
    }

    public ResourceLocation book() {
        return book;
    }

    public String entry() {
        return entry;
    }

    /** on screen right now */
    public boolean showing() {
        return !gone && Util.getMillis() - drawnAt < 200;
    }

    /** gone for good: timed out, or opened */
    public boolean gone() {
        return gone;
    }

    /** slides the toast away (the player opened it) */
    public void dismiss() {
        gone = true;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long timeSinceLastVisible) {
        drawnAt = Util.getMillis();
        Font font = toasts.getMinecraft().font;
        g.blitSprite(BACKGROUND, 0, 0, width(), height());
        g.drawString(font, cut(font, title), TEXT_X, 7, TITLE_COLOR, false);
        Component line = hint != null && (timeSinceLastVisible / LINE_MILLIS) % 2 == 1 ? hint : detail;
        g.drawString(font, cut(font, line), TEXT_X, 18, TEXT_COLOR, false);
        g.renderFakeItem(icon, 8, 8);
        if (timeSinceLastVisible >= DISPLAY_MILLIS * toasts.getNotificationDisplayTimeMultiplier()) {
            gone = true;
        }
        return gone ? Visibility.HIDE : Visibility.SHOW;
    }

    /** {@code text} on one line, cut short with "…" to fit beside the icon */
    private static String cut(Font font, Component text) {
        String plain = text.getString();
        if (font.width(plain) <= TEXT_W) {
            return plain;
        }
        return font.plainSubstrByWidth(plain, TEXT_W - font.width("…")) + "…";
    }
}
