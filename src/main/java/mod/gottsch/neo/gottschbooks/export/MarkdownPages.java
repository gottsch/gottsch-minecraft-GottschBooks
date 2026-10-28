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
import mod.gottsch.neo.gottschbooks.core.text.Block;
import mod.gottsch.neo.gottschbooks.core.text.BookText;
import mod.gottsch.neo.gottschbooks.core.text.Inline;
import mod.gottsch.neo.gottschbooks.core.text.ItemReference;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A book as GitHub-wiki-flavoured Markdown: {@code Home.md} (a wiki's front page), then {@code <entry>.md} for a
 * manual, linked as wiki pages are, without the extension. GitHub gives each heading an id from its text, so a link
 * to {@code {#anchor}} is written with the id GitHub gives that heading. GitHub drops inline styles, so colours are
 * left out; bold and italic stay.
 *
 * @author Mark Gottschling on 10/28/2026
 */
final class MarkdownPages extends WebPages {
    static final String HOME = "Home";
    /** a paragraph's start that Markdown would read as a block of its own */
    private static final Pattern BLOCK_START = Pattern.compile("^(#|>|[-+=]|\\d+[.)])");
    private static final String ESCAPED = "\\`*_[]<|";

    /** entry id to anchor to the id GitHub gives that heading */
    private final Map<String, Map<String, String>> anchors = new HashMap<>();

    MarkdownPages(LoadedBook book, String locale) {
        super(book, locale);
        if (manual != null) {
            for (Manual.Entry entry : manual.allEntries()) {
                Slugs slugs = new Slugs();
                slugs.slug(entry.title().getString());
                Map<String, String> byAnchor = new HashMap<>();
                headings(entry.text().blocks(), slugs, byAnchor);
                anchors.put(entry.id(), byAnchor);
            }
        }
    }

    /** gives each heading its id in page order, as GitHub does, noting the ones with anchors */
    private static void headings(List<Block> blocks, Slugs slugs, Map<String, String> byAnchor) {
        for (Block block : blocks) {
            if (block instanceof Block.Heading h) {
                String slug = slugs.slug(h.text().getString());
                if (!h.anchor().isEmpty()) {
                    byAnchor.putIfAbsent(h.anchor(), slug);
                }
            } else if (block instanceof Block.Fence f) {
                headings(f.body(), slugs, byAnchor);
            }
        }
    }

    @Override
    String extension() {
        return ".md";
    }

    @Override
    String indexPage() {
        return HOME;
    }

    @Override
    String anchor(String entry, String anchor) {
        return anchors.getOrDefault(entry, Map.of()).getOrDefault(anchor, anchor);
    }

    @Override
    String link(String page) {
        return page;
    }

    @Override
    String index() {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(inline(manual.title())).append("\n\n");
        blocks(manual.landing(), out);
        for (Manual.Category category : manual.categories()) {
            out.append("## ").append(inline(category.title())).append("\n\n");
            blocks(category.description(), out);
            contents(entriesIn(category.id()), out);
        }
        List<Manual.Entry> loose = entriesIn("");
        if (!loose.isEmpty()) {
            out.append("## Other entries\n\n");
            contents(loose, out);
        }
        return out.toString();
    }

    private void contents(List<Manual.Entry> entries, StringBuilder out) {
        for (Manual.Entry entry : entries) {
            out.append("- [").append(escape(entry.title().getString())).append("](").append(link(page(entry.id()))).append(')');
            if (entry.advancement().isPresent()) {
                out.append(" *(locked)*");
            }
            out.append('\n');
        }
        if (!entries.isEmpty()) {
            out.append('\n');
        }
    }

    @Override
    String entry(Manual.Entry entry) {
        StringBuilder out = new StringBuilder();
        out.append('[').append(escape(title())).append("](").append(HOME).append(")\n\n");
        out.append("# ").append(inline(entry.title())).append("\n\n");
        entry.advancement().ifPresent(advancement -> out.append("> **Locked:** in the game, this entry stays hidden ")
                .append("until the player has the advancement `").append(advancement).append("`.\n\n"));
        blocks(entry.text().blocks(), out);
        return out.toString();
    }

    @Override
    String lore(BookText text) {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(escape(title())).append("\n\n");
        text.get("author").ifPresent(author -> out.append("*by ").append(inline(Inline.parse(author))).append("*\n\n"));
        blocks(text.blocks(), out);
        return out.toString();
    }

    /** the blocks, each followed by a blank line; neighbouring list items of one kind make one list */
    private void blocks(List<Block> blocks, StringBuilder out) {
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            if (block instanceof Block.ListItem item) {
                out.append(item.number() == 0 ? "- " : item.number() + ". ").append(inline(item.text())).append('\n');
                boolean more = i + 1 < blocks.size() && blocks.get(i + 1) instanceof Block.ListItem next
                        && (next.number() == 0) == (item.number() == 0);
                if (!more) {
                    out.append('\n');
                }
            } else if (block instanceof Block.Paragraph p) {
                String text = startSafe(inline(p.text()));
                out.append(p.quote() ? "> " + text : text).append("\n\n");
            } else if (block instanceof Block.Heading h) {
                int level = Math.max(1, Math.min(6, h.level()));
                out.append("#".repeat(level)).append(' ').append(inline(h.text())).append("\n\n");
            } else if (block instanceof Block.Ornament) {
                out.append("---\n\n");
            } else if (block instanceof Block.Illustration picture) {
                StringBuilder body = new StringBuilder();
                if (!picture.caption().isEmpty()) {
                    body.append(startSafe(inline(Inline.parse(picture.caption())))).append("\n\n");
                }
                placeholder(ILLUSTRATION, id(picture.item()), body, out);
            } else if (block instanceof Block.Fence fence) {
                String label = label(fence.kind()).orElse(null);
                if (label == null) {
                    // a kind the reader doesn't know: it shows its lines as plain text
                    blocks(fence.body(), out);
                } else {
                    StringBuilder body = new StringBuilder();
                    blocks(fence.body(), body);
                    placeholder(label, fence.argument(), body, out);
                }
            } else if (block instanceof Block.Table table) {
                table(table, out);
            }
            // a page break has no place on a web page
        }
    }

    /** a quote headed {@code **Label:** `id`}, holding the body */
    private static void placeholder(String label, String id, StringBuilder body, StringBuilder out) {
        out.append("> **").append(label).append(":** ").append(code(id)).append('\n');
        String text = body.toString().strip();
        if (!text.isEmpty()) {
            out.append(">\n");
            for (String line : text.split("\n", -1)) {
                out.append(line.isEmpty() ? ">" : "> " + line).append('\n');
            }
        }
        out.append('\n');
    }

    private void table(Block.Table table, StringBuilder out) {
        int columns = table.align().size();
        // GitHub needs a header row; a table without one gets an empty one
        if (table.header().isEmpty()) {
            out.append("|").append("   |".repeat(columns)).append('\n');
        } else {
            row(table.header(), out);
        }
        out.append('|');
        for (Block.Table.Align align : table.align()) {
            out.append(switch (align) {
                case LEFT -> "---";
                case CENTER -> ":---:";
                case RIGHT -> "---:";
            }).append('|');
        }
        out.append('\n');
        for (List<Block.Table.Cell> row : table.rows()) {
            row(row, out);
        }
        out.append('\n');
    }

    private void row(List<Block.Table.Cell> cells, StringBuilder out) {
        out.append('|');
        for (Block.Table.Cell cell : cells) {
            StringBuilder text = new StringBuilder();
            for (ItemReference icon : cell.icons()) {
                text.append(code(id(icon))).append(' ');
            }
            text.append(inline(cell.text()));
            out.append(' ').append(text.toString().strip()).append(" |");
        }
        out.append('\n');
    }

    /** an id in backticks; a {@code |} in it is escaped, as a table cell needs */
    private static String code(String id) {
        return "`" + id.replace("|", "\\|") + "`";
    }

    /** the text's runs as emphasis and links; line breaks are {@code <br>}, which also works in tables */
    String inline(Component text) {
        StringBuilder out = new StringBuilder();
        String open = null;
        for (Run run : runs(text)) {
            if (open != null && !open.equals(run.href())) {
                out.append("](").append(open).append(')');
                open = null;
            }
            if (run.href() != null && open == null) {
                open = run.href();
                out.append('[');
            }
            String marks = run.bold() && run.italic() ? "***" : run.bold() ? "**" : run.italic() ? "*" : "";
            String body = escape(run.text()).replace("\n", "<br>");
            // emphasis can't start or end with a space, so the spaces go outside the marks
            String core = body.strip();
            if (marks.isEmpty() || core.isEmpty()) {
                out.append(body);
            } else {
                int lead = body.indexOf(core);
                out.append(body, 0, lead).append(marks).append(core).append(marks).append(body.substring(lead + core.length()));
            }
        }
        if (open != null) {
            out.append("](").append(open).append(')');
        }
        return out.toString();
    }

    /** escapes a start that Markdown would read as a heading, note, list or rule */
    private static String startSafe(String text) {
        if (!BLOCK_START.matcher(text).find()) {
            return text;
        }
        int at = 0;
        while (at < text.length() && Character.isDigit(text.charAt(at))) {
            at++;
        }
        return text.substring(0, at) + "\\" + text.substring(at);
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (ESCAPED.indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * The ids GitHub gives headings: the text in lower case, without punctuation, spaces as {@code -}; a repeated one
     * gets {@code -1}, {@code -2} ...
     */
    static final class Slugs {
        private final Map<String, Integer> seen = new HashMap<>();

        String slug(String heading) {
            StringBuilder slug = new StringBuilder();
            heading.toLowerCase(Locale.ROOT).strip().codePoints().forEach(c -> {
                if (c == ' ') {
                    slug.append('-');
                } else if (Character.isLetterOrDigit(c) || c == '-' || c == '_'
                        || Character.getType(c) == Character.NON_SPACING_MARK) {
                    slug.appendCodePoint(c);
                }
            });
            String base = slug.toString();
            int count = seen.merge(base, 1, Integer::sum) - 1;
            return count == 0 ? base : base + "-" + count;
        }
    }
}
