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
package mod.gottsch.forge.gottschbooks.export;

import mod.gottsch.forge.gottschbooks.client.LoadedBook;
import mod.gottsch.forge.gottschbooks.client.Manual;
import mod.gottsch.forge.gottschbooks.core.text.Block;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import mod.gottsch.forge.gottschbooks.core.text.Inline;
import mod.gottsch.forge.gottschbooks.core.text.ItemReference;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * A book as standalone HTML pages, each with its own small style sheet: {@code index.html}, then
 * {@code <entry>.html} for a manual. Headings keep their {@code {#anchor}} as their {@code id}; colours, bold and
 * italic are inline styles; tables keep their alignment.
 *
 * @author Mark Gottschling on 10/28/2026
 */
final class HtmlPages extends WebPages {
    static final String INDEX = "index";

    private static final String STYLE = """
            body { margin: 0; background: #f4ecd8; color: #2b2118; font: 17px/1.55 Georgia, "Times New Roman", serif; }
            main { max-width: 46em; margin: 0 auto; padding: 1.5em 1.25em 4em; }
            nav { font-size: 0.9em; margin-bottom: 1.5em; }
            a { color: #1e4fa0; }
            h1, h2 { border-bottom: 1px solid #c9b88f; padding-bottom: 0.15em; }
            .author { font-style: italic; margin-top: -0.5em; }
            .note { color: #5a5048; font-style: italic; border-left: 3px solid #c9b88f; margin: 1em 0; padding: 0 1em; }
            .ornament { border: 0; text-align: center; margin: 1.5em 0; }
            .ornament::after { content: "\\2766"; color: #8a7a5a; }
            .placeholder { border: 1px dashed #a08f6a; background: #efe4c8; border-radius: 4px; margin: 1em 0; padding: 0.6em 1em; }
            .placeholder > .label { font-size: 0.85em; color: #6a5d48; margin-bottom: 0.3em; }
            .placeholder > :last-child { margin-bottom: 0; }
            code { font-size: 0.85em; background: #e6d9b8; padding: 0 0.25em; border-radius: 3px; overflow-wrap: anywhere; }
            .locked { background: #eadcb8; border-left: 3px solid #8a3a2a; padding: 0.4em 1em; }
            .tag { font-size: 0.85em; color: #8a3a2a; }
            table { border-collapse: collapse; margin: 1em 0; }
            th, td { border: 1px solid #c9b88f; padding: 0.3em 0.6em; vertical-align: top; }
            th { background: #eadcb8; }
            .contents li { margin: 0.2em 0; }
            """;

    HtmlPages(LoadedBook book, String locale) {
        super(book, locale);
    }

    @Override
    String extension() {
        return ".html";
    }

    @Override
    String indexPage() {
        return INDEX;
    }

    @Override
    String anchor(String entry, String anchor) {
        return anchor;
    }

    @Override
    String link(String page) {
        return page + extension();
    }

    @Override
    String index() {
        StringBuilder out = new StringBuilder();
        out.append("<h1>").append(inline(manual.title())).append("</h1>\n");
        blocks(manual.landing(), out);
        for (Manual.Category category : manual.categories()) {
            out.append("<section>\n<h2 id=\"").append(escape(category.id())).append("\">").append(inline(category.title()))
                    .append("</h2>\n");
            blocks(category.description(), out);
            contents(entriesIn(category.id()), out);
            out.append("</section>\n");
        }
        List<Manual.Entry> loose = entriesIn("");
        if (!loose.isEmpty()) {
            out.append("<section>\n<h2>Other entries</h2>\n");
            contents(loose, out);
            out.append("</section>\n");
        }
        return document(title(), null, out);
    }

    private void contents(List<Manual.Entry> entries, StringBuilder out) {
        if (entries.isEmpty()) {
            return;
        }
        out.append("<ul class=\"contents\">\n");
        for (Manual.Entry entry : entries) {
            out.append("<li><a href=\"").append(escape(link(page(entry.id())))).append("\">")
                    .append(escape(entry.title().getString())).append("</a>");
            if (entry.advancement().isPresent()) {
                out.append(" <span class=\"tag\">(locked)</span>");
            }
            out.append("</li>\n");
        }
        out.append("</ul>\n");
    }

    @Override
    String entry(Manual.Entry entry) {
        StringBuilder out = new StringBuilder();
        out.append("<h1>").append(inline(entry.title())).append("</h1>\n");
        entry.advancement().ifPresent(advancement -> out.append("<p class=\"locked\">Locked: in the game, this entry ")
                .append("stays hidden until the player has the advancement <code>").append(escape(advancement.toString()))
                .append("</code>.</p>\n"));
        blocks(entry.text().blocks(), out);
        return document(entry.title().getString() + " - " + title(), INDEX + extension(), out);
    }

    @Override
    String lore(BookText text) {
        StringBuilder out = new StringBuilder();
        out.append("<h1>").append(escape(title())).append("</h1>\n");
        text.get("author").ifPresent(author -> out.append("<p class=\"author\">by ").append(inline(Inline.parse(author)))
                .append("</p>\n"));
        blocks(text.blocks(), out);
        return document(title(), null, out);
    }

    private String document(String title, String contents, StringBuilder body) {
        StringBuilder out = new StringBuilder();
        out.append("<!DOCTYPE html>\n<html lang=\"").append(escape(locale.replace('_', '-'))).append("\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(escape(title)).append("</title>\n")
                .append("<style>\n").append(STYLE).append("</style>\n</head>\n<body>\n<main>\n");
        if (contents != null) {
            out.append("<nav><a href=\"").append(escape(contents)).append("\">").append(escape(title())).append("</a></nav>\n");
        }
        out.append(body).append("</main>\n</body>\n</html>\n");
        return out.toString();
    }

    /** the blocks in order; neighbouring list items of one kind share a list */
    private void blocks(List<Block> blocks, StringBuilder out) {
        String list = null;
        for (Block block : blocks) {
            String kind = block instanceof Block.ListItem item ? (item.number() == 0 ? "ul" : "ol") : null;
            if (list != null && !list.equals(kind)) {
                out.append("</").append(list).append(">\n");
                list = null;
            }
            if (block instanceof Block.ListItem item) {
                if (list == null) {
                    list = kind;
                    out.append(item.number() > 1 ? "<ol start=\"" + item.number() + "\">" : "<" + kind + ">").append('\n');
                }
                out.append("<li>").append(inline(item.text())).append("</li>\n");
            } else if (block instanceof Block.Paragraph p) {
                out.append(p.quote() ? "<p class=\"note\">" : "<p>").append(inline(p.text())).append("</p>\n");
            } else if (block instanceof Block.Heading h) {
                int level = Math.max(1, Math.min(6, h.level()));
                out.append("<h").append(level);
                if (!h.anchor().isEmpty()) {
                    out.append(" id=\"").append(escape(h.anchor())).append('"');
                }
                out.append('>').append(inline(h.text())).append("</h").append(level).append(">\n");
            } else if (block instanceof Block.Ornament) {
                out.append("<hr class=\"ornament\">\n");
            } else if (block instanceof Block.Illustration picture) {
                out.append("<figure class=\"placeholder\">\n");
                label(ILLUSTRATION, id(picture.item()), out);
                if (!picture.caption().isEmpty()) {
                    out.append("<figcaption>").append(inline(Inline.parse(picture.caption()))).append("</figcaption>\n");
                }
                out.append("</figure>\n");
            } else if (block instanceof Block.Fence fence) {
                String label = label(fence.kind()).orElse(null);
                if (label == null) {
                    // a kind the reader doesn't know: it shows its lines as plain text
                    blocks(fence.body(), out);
                } else {
                    out.append("<figure class=\"placeholder ").append(escape(fence.kind())).append("\">\n");
                    label(label, fence.argument(), out);
                    blocks(fence.body(), out);
                    out.append("</figure>\n");
                }
            } else if (block instanceof Block.Table table) {
                table(table, out);
            }
            // a page break has no place on a web page
        }
        if (list != null) {
            out.append("</").append(list).append(">\n");
        }
    }

    private static void label(String label, String id, StringBuilder out) {
        out.append("<div class=\"label\">").append(escape(label)).append(": <code>").append(escape(id)).append("</code></div>\n");
    }

    private void table(Block.Table table, StringBuilder out) {
        out.append("<table>\n");
        if (!table.header().isEmpty()) {
            out.append("<thead>\n");
            row(table.header(), table.align(), "th", out);
            out.append("</thead>\n");
        }
        out.append("<tbody>\n");
        for (List<Block.Table.Cell> row : table.rows()) {
            row(row, table.align(), "td", out);
        }
        out.append("</tbody>\n</table>\n");
    }

    private void row(List<Block.Table.Cell> cells, List<Block.Table.Align> align, String tag, StringBuilder out) {
        out.append("<tr>");
        for (int i = 0; i < cells.size(); i++) {
            Block.Table.Align a = i < align.size() ? align.get(i) : Block.Table.Align.LEFT;
            out.append('<').append(tag);
            if (a != Block.Table.Align.LEFT) {
                out.append(" style=\"text-align:").append(a.name().toLowerCase(Locale.ROOT)).append('"');
            }
            out.append('>');
            for (ItemReference icon : cells.get(i).icons()) {
                out.append("<code>").append(escape(id(icon))).append("</code> ");
            }
            out.append(inline(cells.get(i).text())).append("</").append(tag).append('>');
        }
        out.append("</tr>\n");
    }

    /** the text's runs: a span for each styled one, inside a link where they lead somewhere */
    String inline(Component text) {
        StringBuilder out = new StringBuilder();
        String open = null;
        for (Run run : runs(text)) {
            if (open != null && !open.equals(run.href())) {
                out.append("</a>");
                open = null;
            }
            if (run.href() != null && open == null) {
                open = run.href();
                out.append("<a href=\"").append(escape(open)).append("\">");
            }
            StringBuilder style = new StringBuilder();
            if (run.colour() != null) {
                style.append(String.format(Locale.ROOT, "color:#%06x;", run.colour()));
            }
            if (run.bold()) {
                style.append("font-weight:bold;");
            }
            if (run.italic()) {
                style.append("font-style:italic;");
            }
            String body = escape(run.text()).replace("\n", "<br>\n");
            if (style.isEmpty()) {
                out.append(body);
            } else {
                out.append("<span style=\"").append(style, 0, style.length() - 1).append("\">").append(body).append("</span>");
            }
        }
        if (open != null) {
            out.append("</a>");
        }
        return out.toString();
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
