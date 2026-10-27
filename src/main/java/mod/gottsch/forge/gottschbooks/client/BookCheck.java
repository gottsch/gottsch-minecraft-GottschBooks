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
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import mod.gottsch.forge.gottschbooks.core.text.Inline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds what would show as {@code [... ?]} on a page, or not show at all, in every file of a book: items, recipes,
 * images and mobs that don't exist, components that don't parse, {@code entry:} links to entries or anchors that
 * aren't there, fonts that aren't, entries in a category that doesn't exist, and fenced blocks the reader doesn't
 * know. For {@code /gottschbooks check}, so an author finds them all at once instead of page by page. Needs a world,
 * since recipes, mobs and many components belong to it.
 *
 * @author Mark Gottschling on 10/27/2026
 */
public final class BookCheck {
    /** the fenced blocks the reader draws; any other kind shows its lines as plain text */
    private static final Set<String> FENCES = Set.of("spotlight", "recipe", "image", "entity");

    /** one thing wrong, and the file it is in ({@code assets/<ns>/books/<book>/<locale>/<entry>.md}) */
    public record Problem(String file, String message) {}

    private final LoadedBook book;
    private final Level level;
    private final List<Problem> problems = new ArrayList<>();
    private final Map<ResourceLocation, Boolean> resources = new HashMap<>();
    private String file;

    private BookCheck(LoadedBook book, Level level) {
        this.book = book;
        this.level = level;
    }

    public static List<Problem> check(LoadedBook book, Level level) {
        BookCheck check = new BookCheck(book, level);
        check.run();
        return check.problems;
    }

    private void run() {
        file = "assets/" + book.id().getNamespace() + "/books/" + book.id().getPath() + "/book.md";
        book.meta().get("skin").ifPresent(skin -> {
            ResourceLocation folder = ResourceLocation.tryParse(skin);
            if (folder == null) {
                problem("skin '" + skin + "' isn't a resource location");
                return;
            }
            for (String art : List.of("cover", "page", "ornament")) {
                ResourceLocation texture = folder.withSuffix("/" + art + ".png");
                if (!exists(texture)) {
                    problem("skin '" + skin + "' has no " + art + ".png (assets/" + texture.getNamespace() + "/"
                            + texture.getPath() + ")");
                }
            }
        });
        if (book.isManual()) {
            if (!book.entries().containsKey(Manual.INDEX)) {
                problem("manual has no " + Manual.INDEX + ".md, so its title is its id and it has no landing text");
            }
        } else if (!book.entries().containsKey(LoadedBook.LORE_ENTRY)) {
            problem("lore book has no " + LoadedBook.LORE_ENTRY + ".md; files: " + book.entries().keySet());
        }

        Set<String> categories = book.entries().keySet().stream().filter(name -> name.startsWith(Manual.CATEGORIES))
                .map(name -> name.substring(Manual.CATEGORIES.length())).collect(Collectors.toSet());
        book.entries().forEach((entry, byLocale) -> byLocale.forEach((locale, text) -> {
            file = "assets/" + book.id().getNamespace() + "/books/" + book.id().getPath() + "/" + locale + "/" + entry + ".md";
            entry(entry, locale, text, categories);
        }));
    }

    private void entry(String entry, String locale, BookText text, Set<String> categories) {
        text.get("icon").flatMap(Icons::problem).ifPresent(why -> problem("icon: " + why));
        if (book.isManual() && !entry.equals(Manual.INDEX) && !entry.startsWith(Manual.CATEGORIES)) {
            text.get("category").filter(category -> !categories.contains(category)).ifPresent(category ->
                    problem("category '" + category + "' has no categories/" + category + ".md, so the entry is listed after the categories"));
            text.get("advancement").filter(id -> ResourceLocation.tryParse(id) == null).ifPresent(id ->
                    problem("advancement '" + id + "' isn't a resource location, so the entry is never hidden"));
        }
        blocks(text.blocks(), locale);
    }

    private void blocks(List<Block> blocks, String locale) {
        for (Block block : blocks) {
            if (block instanceof Block.Paragraph p) {
                text(p.text(), locale);
            } else if (block instanceof Block.Heading h) {
                text(h.text(), locale);
            } else if (block instanceof Block.ListItem item) {
                text(item.text(), locale);
            } else if (block instanceof Block.Illustration i) {
                Icons.problem(i.item().text()).ifPresent(this::problem);
            } else if (block instanceof Block.Fence f) {
                fence(f);
                blocks(f.body(), locale);
            }
        }
    }

    private void fence(Block.Fence fence) {
        String argument = fence.argument().strip();
        switch (fence.kind()) {
            case "spotlight" -> Icons.problem(argument).ifPresent(why -> problem("spotlight: " + why));
            case "recipe" -> {
                ResourceLocation id = ResourceLocation.tryParse(argument);
                if (id == null) {
                    problem("recipe '" + argument + "' isn't a resource location");
                } else {
                    level.getRecipeManager().byKey(id).ifPresentOrElse(recipe -> {
                        if (RecipeView.of(recipe, level.registryAccess()).isEmpty()) {
                            problem("recipe '" + id + "' has no ingredients to show (its ingredients are worked out in code)");
                        }
                    }, () -> problem("no recipe '" + id + "'"));
                }
            }
            case "image" -> {
                String texture = argument.split("\\s+")[0];
                ResourceLocation id = ResourceLocation.tryParse(texture);
                if (id == null || !exists(id)) {
                    problem("no image '" + texture + "' (the full path, such as mymod:textures/gui/map.png)");
                }
            }
            case "entity" -> {
                Optional<EntityType<?>> type = EntityType.byString(argument);
                if (type.isEmpty()) {
                    problem("no mob '" + argument + "'");
                } else if (!(type.get().create(level) instanceof LivingEntity)) {
                    problem("'" + argument + "' isn't a mob; entity blocks draw living entities only");
                }
            }
            default -> {
                if (!fence.kind().isEmpty() && !FENCES.contains(fence.kind())) {
                    problem("unknown block '```" + fence.kind() + "', shown as plain lines; the reader knows " + FENCES);
                }
            }
        }
    }

    /** a run of text's links and fonts */
    private void text(Component text, String locale) {
        text.visit((style, run) -> {
            link(style, locale);
            if (!style.getFont().equals(Style.DEFAULT_FONT)) {
                ResourceLocation font = style.getFont();
                if (!exists(font.withPath(path -> "font/" + path + ".json"))) {
                    problem("no font '" + font + "' (assets/" + font.getNamespace() + "/font/" + font.getPath()
                            + ".json); its text draws as boxes");
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    private void link(Style style, String locale) {
        ClickEvent click = style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.CHANGE_PAGE || !click.getValue().startsWith(Inline.ENTRY_LINK)) {
            return;
        }
        String target = click.getValue().substring(Inline.ENTRY_LINK.length());
        if (!book.isManual()) {
            problem("link to '" + target + "': links between entries only work in manuals");
            return;
        }
        String[] parts = target.split("#", 2);
        String entry = parts[0];
        if (entry.equals(Manual.INDEX) || entry.startsWith(Manual.CATEGORIES) || !book.entries().containsKey(entry)) {
            problem("link to '" + target + "': no entry '" + entry + "'");
            return;
        }
        if (parts.length > 1) {
            String anchor = parts[1];
            boolean found = book.entry(entry, locale).map(text -> anchors(text.blocks()).contains(anchor)).orElse(false);
            if (!found) {
                problem("link to '" + target + "': entry '" + entry + "' has no heading {#" + anchor + "}");
            }
        }
    }

    private static Set<String> anchors(List<Block> blocks) {
        Set<String> anchors = new HashSet<>();
        for (Block block : blocks) {
            if (block instanceof Block.Heading h && !h.anchor().isEmpty()) {
                anchors.add(h.anchor());
            } else if (block instanceof Block.Fence f) {
                anchors.addAll(anchors(f.body()));
            }
        }
        return anchors;
    }

    private boolean exists(ResourceLocation resource) {
        return resources.computeIfAbsent(resource,
                id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
    }

    private void problem(String message) {
        problems.add(new Problem(file, message));
    }
}
