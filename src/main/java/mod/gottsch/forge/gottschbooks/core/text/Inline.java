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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The inline pass: {@code **bold**}, {@code *italic*}, {@code {gold}colour{}}, {@code {#d4a017}colour{}},
 * {@code {font:ns:id}font{}} and {@code [text](entry:id)} / {@code [text](https://...)} links, with
 * {@code \} escaping the next character. A marker with nothing to close it is plain text, so a stray {@code *} or
 * {@code {} never breaks a book. Colours don't nest: a second colour replaces the first until {@code {}}. A font is
 * held apart from the colour, so {@code {white}{font:ns:id}} sets both, and one {@code {}} ends both.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class Inline {
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final String ESCAPABLE = "\\*_{}[]()!#>-`<";
    private static final String FONT = "font:";

    private Inline() {}

    public static Component parse(String text) {
        MutableComponent root = Component.empty();
        StringBuilder run = new StringBuilder();
        Style style = Style.EMPTY;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && ESCAPABLE.indexOf(text.charAt(i + 1)) >= 0) {
                run.append(text.charAt(i + 1));
                i += 2;
            } else if (c == '*') {
                // a run of stars: 1 toggles italic, 2 bold, 3 both; a toggle that nothing closes stays as text
                int n = starRun(text, i);
                boolean bold = (n == 2 || n == 3) && (style.isBold() || closes(text, i + n, true));
                boolean italic = (n == 1 || n == 3) && (style.isItalic() || closes(text, i + n, false));
                if (bold || italic) {
                    flush(root, run, style);
                }
                if (bold) {
                    style = style.withBold(!style.isBold());
                }
                if (italic) {
                    style = style.withItalic(!style.isItalic());
                }
                run.append("*".repeat(n - (bold ? 2 : 0) - (italic ? 1 : 0)));
                i += n;
            } else if (c == '[' && link(text, i) != null) {
                Link link = link(text, i);
                flush(root, run, style);
                root.append(Component.empty().setStyle(LINK.withClickEvent(link.click()).applyTo(style))
                        .append(parse(link.text())));
                i = link.end();
            } else if (c == '{') {
                int end = text.indexOf('}', i);
                String name = end < 0 ? "" : text.substring(i + 1, end);
                Optional<TextColor> colour = colour(name);
                Optional<ResourceLocation> font = font(name);
                boolean closed = text.indexOf("{}", end) >= 0;
                if (end == i + 1 && (style.getColor() != null || !style.getFont().equals(Style.DEFAULT_FONT))) {
                    // {} ends the colour and the font
                    flush(root, run, style);
                    style = style.withColor((TextColor) null).withFont(null);
                    i = end + 1;
                } else if (colour.isPresent() && closed) {
                    flush(root, run, style);
                    style = style.withColor(colour.get());
                    i = end + 1;
                } else if (font.isPresent() && closed) {
                    flush(root, run, style);
                    style = style.withFont(font.get());
                    i = end + 1;
                } else {
                    run.append(c);
                    i++;
                }
            } else {
                run.append(c);
                i++;
            }
        }
        flush(root, run, style);
        return root;
    }

    /** where a book link leads: {@code entry:<entry>[#anchor]} (another entry of the same book) */
    public static final String ENTRY_LINK = "entry:";
    /** links are underlined in a dark blue that reads on the page */
    private static final Style LINK = Style.EMPTY.withUnderlined(true).withColor(0x1E4FA0);
    private static final Pattern ENTRY_TARGET = Pattern.compile("entry:[a-z0-9_./-]+(#[a-z0-9_-]+)?");
    private static final Pattern URL_TARGET = Pattern.compile("https?://[^\s)]+");

    /** a link at {@code [}: its text, where it leads, and the index after its {@code )} */
    private record Link(String text, ClickEvent click, int end) {}

    /**
     * {@code [text](entry:id#anchor)} or {@code [text](https://...)} starting at {@code open}; null when it isn't one,
     * so {@code [brackets]} in ordinary text stay as they are. Entry links ride on {@code CHANGE_PAGE}, which the
     * book screen handles; web links use {@code OPEN_URL}, which asks the player before opening.
     */
    private static Link link(String text, int open) {
        int close = text.indexOf("](", open + 1);
        if (close < 0) {
            return null;
        }
        int end = text.indexOf(')', close + 2);
        if (end < 0) {
            return null;
        }
        String target = text.substring(close + 2, end).strip();
        ClickEvent click;
        if (ENTRY_TARGET.matcher(target).matches()) {
            click = new ClickEvent(ClickEvent.Action.CHANGE_PAGE, target);
        } else if (URL_TARGET.matcher(target).matches()) {
            click = new ClickEvent(ClickEvent.Action.OPEN_URL, target);
        } else {
            return null;
        }
        return new Link(text.substring(open + 1, close), click, end + 1);
    }

    /** {@code gold}, {@code dark_aqua} (vanilla's 16 colour names) or {@code #d4a017} */
    public static Optional<TextColor> colour(String name) {
        if (HEX.matcher(name).matches()) {
            return Optional.of(TextColor.fromRgb(Integer.parseInt(name.substring(1), 16)));
        }
        ChatFormatting formatting = ChatFormatting.getByName(name);
        return formatting != null && formatting.isColor() ? Optional.of(TextColor.fromLegacyFormat(formatting)) : Optional.empty();
    }

    /** {@code font:mypack:icons}: the font a run is drawn in, as in a resource pack's {@code font/} folder */
    public static Optional<ResourceLocation> font(String name) {
        return name.startsWith(FONT) ? Optional.ofNullable(ResourceLocation.tryParse(name.substring(FONT.length())))
                : Optional.empty();
    }

    private static int starRun(String text, int from) {
        int end = from;
        while (end < text.length() && text.charAt(end) == '*') {
            end++;
        }
        return end - from;
    }

    /** whether a later run of stars can close bold (a run of 2 or 3) or italic (1 or 3) */
    private static boolean closes(String text, int from, boolean bold) {
        int i = from;
        while (i < text.length()) {
            if (text.charAt(i) == '\\') {
                i += 2;
            } else if (text.charAt(i) == '*') {
                int n = starRun(text, i);
                if (n == 3 || n == (bold ? 2 : 1)) {
                    return true;
                }
                i += n;
            } else {
                i++;
            }
        }
        return false;
    }

    private static void flush(MutableComponent root, StringBuilder run, Style style) {
        if (!run.isEmpty()) {
            root.append(Component.literal(run.toString()).setStyle(style));
            run.setLength(0);
        }
    }
}
