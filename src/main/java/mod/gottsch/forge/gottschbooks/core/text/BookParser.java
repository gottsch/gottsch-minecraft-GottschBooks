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

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The block pass over a book file (the Markdown subset in the plan's section 2): front matter, paragraphs (a blank
 * line ends one; a line ending in two spaces or {@code \} breaks the line), {@code #} headings, {@code ---} ornaments,
 * {@code >} notes, {@code ![caption](scheme:ns:id)} illustrations on a line of their own, and {@code <!-- page -->}.
 * Anything else, fenced blocks included for now, is paragraph text, so no file can fail to load.
 *
 * @author Mark Gottschling on 10/17/2026
 */
public final class BookParser {
    private static final Pattern FRONT_LINE = Pattern.compile("([A-Za-z0-9_-]+)\\s*:\\s*(.*)");
    /** a comment after a front matter value: whitespace, then {@code #} at the end or before whitespace */
    private static final Pattern FRONT_COMMENT = Pattern.compile("\\s+#(\\s.*)?$");
    private static final Pattern ORNAMENT = Pattern.compile("(-\\s*){3,}|(\\*\\s*){3,}|(_\\s*){3,}");
    private static final Pattern HEADING = Pattern.compile("(#{1,6})\\s+(.*?)\\s*#*");
    private static final Pattern ILLUSTRATION =
            Pattern.compile("!\\[(.*)]\\(([a-z0-9_]+):([a-z0-9_.-]+:[a-z0-9_./-]+)\\)");
    private static final Pattern PAGE_BREAK = Pattern.compile("<!--\\s*page\\s*-->", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST_ITEM = Pattern.compile("([-*+]|\\d+[.)])\\s+.*");

    private BookParser() {}

    public static BookText parse(String source) {
        List<String> lines = List.of(source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));
        Map<String, String> front = new LinkedHashMap<>();
        int start = frontMatter(lines, front);

        List<Block> blocks = new ArrayList<>();
        Paragraphs para = new Paragraphs(blocks);
        boolean fenced = false;
        for (int n = start; n < lines.size(); n++) {
            String raw = lines.get(n);
            String line = raw.strip();
            if (line.startsWith("```")) {
                // fenced blocks (spotlight, recipe, ...) arrive with manual mode; until then their lines are text
                fenced = !fenced;
                para.end();
                continue;
            }
            if (fenced) {
                if (!line.isEmpty()) {
                    para.add(raw.stripTrailing(), false, true);
                }
                continue;
            }
            if (line.isEmpty()) {
                para.end();
                continue;
            }
            if (PAGE_BREAK.matcher(line).matches()) {
                para.end();
                blocks.add(new Block.PageBreak());
                continue;
            }
            if (ORNAMENT.matcher(line).matches()) {
                para.end();
                blocks.add(new Block.Ornament());
                continue;
            }
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                para.end();
                blocks.add(new Block.Heading(heading.group(1).length(), Inline.parse(heading.group(2))));
                continue;
            }
            Matcher picture = ILLUSTRATION.matcher(line);
            if (picture.matches()) {
                para.end();
                blocks.add(new Block.Illustration(picture.group(2), new ResourceLocation(picture.group(3)),
                        picture.group(1).strip()));
                continue;
            }
            if (line.startsWith(">")) {
                String text = line.substring(1).strip();
                if (text.isEmpty()) {
                    para.end();
                } else {
                    para.add(withBreak(raw, text), true, false);
                }
                continue;
            }
            // one level of list: an item starts its own line (lists get real layout with manual mode)
            para.add(withBreak(raw, line), para.quote, LIST_ITEM.matcher(line).matches());
        }
        para.end();
        return new BookText(Collections.unmodifiableMap(front), List.copyOf(blocks));
    }

    /** reads the front matter into {@code front}; returns the first line after it (0 when there is none) */
    private static int frontMatter(List<String> lines, Map<String, String> front) {
        if (lines.isEmpty() || !lines.get(0).strip().equals("---")) {
            return 0;
        }
        for (int n = 1; n < lines.size(); n++) {
            String line = lines.get(n).strip();
            if (line.equals("---")) {
                return n + 1;
            }
            Matcher m = FRONT_LINE.matcher(line);
            if (m.matches()) {
                front.put(m.group(1).toLowerCase(Locale.ROOT), value(m.group(2)));
            }
        }
        // never closed: not front matter after all
        front.clear();
        return 0;
    }

    private static String value(String raw) {
        String value = raw.strip();
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return FRONT_COMMENT.matcher(value).replaceFirst("").strip();
    }

    /** the line's text, marked to break after it when the source line ends in two spaces or {@code \} */
    private static String withBreak(String raw, String text) {
        if (text.endsWith("\\") && !text.endsWith("\\\\")) {
            return text.substring(0, text.length() - 1) + "\n";
        }
        return raw.endsWith("  ") ? text + "\n" : text;
    }

    /** gathers lines into the paragraph being read */
    private static final class Paragraphs {
        private final List<Block> blocks;
        private final StringBuilder text = new StringBuilder();
        private boolean quote;

        Paragraphs(List<Block> blocks) {
            this.blocks = blocks;
        }

        void add(String line, boolean asQuote, boolean ownLine) {
            if (!text.isEmpty() && asQuote != quote) {
                end();
            }
            quote = asQuote;
            if (!text.isEmpty() && text.charAt(text.length() - 1) != '\n') {
                text.append(ownLine ? '\n' : ' ');
            }
            text.append(line);
        }

        void end() {
            if (!text.isEmpty()) {
                String body = text.toString().stripTrailing();
                blocks.add(new Block.Paragraph(Inline.parse(body), quote));
                text.setLength(0);
            }
            quote = false;
        }
    }
}
