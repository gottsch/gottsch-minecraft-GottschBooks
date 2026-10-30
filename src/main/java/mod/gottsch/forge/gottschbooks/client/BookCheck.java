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

import mod.gottsch.forge.gottschbooks.core.ItemMatch;
import mod.gottsch.forge.gottschbooks.core.lint.BookLint;
import mod.gottsch.forge.gottschbooks.core.lint.LintBook;
import mod.gottsch.forge.gottschbooks.core.text.Block;
import mod.gottsch.forge.gottschbooks.core.text.BookText;
import mod.gottsch.forge.gottschbooks.core.text.EntityArgument;
import mod.gottsch.forge.gottschbooks.core.text.ItemReference;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Finds what would show as {@code [... ?]} on a page, or not show at all, in every file of a book: items, recipes,
 * images and mobs that don't exist, components that don't parse, {@code entry:} links to entries or anchors that
 * aren't there, fonts that aren't, entries in a category that doesn't exist, and fenced blocks the reader doesn't
 * know. For {@code /gottschbooks check}, so an author finds them all at once instead of page by page. Needs a world,
 * since recipes, mobs and many components belong to it.
 * <p>
 * What the files alone show (front matter values, links, categories, unknown blocks, tables, references that don't
 * parse) is {@link BookLint}'s, which builds and CI run without the game; this adds what needs the game, for whatever
 * parses: whether items, tags, recipes, images, mobs, fonts and skin textures exist.
 * <p>
 * It also points out a vanilla item used as an entry's icon in a manual about a mod's items, when the entry doesn't
 * otherwise show the item: such an icon is usually for show, yet it answers item lookups for every one of that item.
 *
 * @author Mark Gottschling on 10/27/2026
 */
public final class BookCheck {
    /** one thing wrong, and the file it is in ({@code assets/<ns>/books/<book>/<locale>/<entry>.md}) */
    public record Problem(String file, String message) {}

    private final LoadedBook book;
    private final Level level;
    private final List<Problem> problems = new ArrayList<>();
    /** entries already told about a vanilla icon, so each is told once, not once per language */
    private final Set<String> iconsTold = new HashSet<>();
    /** whether the book shows items that aren't vanilla's (a mod's manual), worked out on first need */
    @Nullable
    private Boolean modded;
    private final Map<ResourceLocation, Boolean> resources = new HashMap<>();
    private String file;

    private BookCheck(LoadedBook book, Level level) {
        this.book = book;
        this.level = level;
    }

    public static List<Problem> check(LoadedBook book, Level level) {
        BookCheck check = new BookCheck(book, level);
        BookLint.check(new LintBook(book.id().toString(), book.meta(), book.entries()))
                .forEach(problem -> check.problems.add(new Problem(problem.file(), problem.message())));
        check.run();
        return check.problems;
    }

    private void run() {
        file = "assets/" + book.id().getNamespace() + "/books/" + book.id().getPath() + "/book.md";
        book.meta().get("skin").ifPresent(skin -> {
            ResourceLocation folder = ResourceLocation.tryParse(skin);
            if (folder == null) {
                // BookLint's
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
        book.entries().forEach((entry, byLocale) -> byLocale.forEach((locale, text) -> {
            file = "assets/" + book.id().getNamespace() + "/books/" + book.id().getPath() + "/" + locale + "/" + entry + ".md";
            entry(entry, text);
        }));
    }

    private void entry(String entry, BookText text) {
        text.get("icon").flatMap(BookCheck::missing).ifPresent(why -> problem("icon: " + why));
        if (book.isManual() && !entry.equals(Manual.INDEX) && !entry.startsWith(Manual.CATEGORIES)) {
            text.get(ItemLookup.ITEMS).ifPresent(this::items);
            vanillaIcon(entry, text);
        }
        blocks(text.blocks());
    }

    /** why a reference that parses isn't drawn as written; a reference that doesn't parse is BookLint's */
    private static Optional<String> missing(String reference) {
        return ItemReference.parse(reference).isPresent() ? Icons.problem(reference) : Optional.empty();
    }

    /**
     * a vanilla icon that answers lookups in a mod's manual, though the entry shows the item nowhere else: a torch on
     * "Bring Light" sends every torch in the game to the entry
     */
    private void vanillaIcon(String entry, BookText text) {
        Optional<ItemReference> icon = text.get("icon").flatMap(ItemReference::parse)
                .filter(ref -> ref.scheme().equals(ItemReference.ITEM) && ref.nbt().isEmpty()
                        && ref.id().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE));
        String from = ItemMatch.lookupFrom(text.get(ItemLookup.LOOKUP_FROM), book.meta().get(ItemLookup.LOOKUP_FROM));
        boolean answers = book.meta().get(ItemLookup.LOOKUP).map(value -> !value.strip().equalsIgnoreCase(ItemLookup.NEVER)).orElse(true)
                && !from.equals(ItemLookup.FROM_ITEMS) && !from.equals(ItemLookup.FROM_NONE);
        if (icon.isEmpty() || !answers || !modded() || shows(text, icon.get().id()) || !iconsTold.add(entry)) {
            return;
        }
        ResourceLocation id = icon.get().id();
        problem("icon " + id + " answers item lookups, so every " + id.getPath() + " in the game names this manual;"
                + " if the icon is only for show, add lookup_from: items (the entry then answers only for its items:)");
    }

    /** whether the entry shows {@code item} besides as its icon: a spotlight, a recipe's result, or in {@code items:} */
    private boolean shows(BookText text, ResourceLocation item) {
        if (shows(text.blocks(), item)) {
            return true;
        }
        for (String token : text.get(ItemLookup.ITEMS).map(ItemLookup::tokens).orElse(List.of())) {
            boolean listed = token.startsWith("#")
                    ? ItemLookup.tag(token).map(items -> items.stream().anyMatch(i -> item.equals(BuiltInRegistries.ITEM.getKey(i)))).orElse(false)
                    : ItemReference.parse(token).map(ref -> ref.id().equals(item)).orElse(false);
            if (listed) {
                return true;
            }
        }
        return false;
    }

    private boolean shows(List<Block> blocks, ResourceLocation item) {
        for (Block block : blocks) {
            if (block instanceof Block.Fence fence) {
                boolean shown = switch (fence.kind()) {
                    case "spotlight" -> ItemReference.parse(fence.argument()).map(ref -> ref.id().equals(item)).orElse(false);
                    case "recipe" -> ItemLookup.recipeResult(fence.argument(), level)
                            .map(stack -> item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))).orElse(false);
                    default -> false;
                };
                if (shown || shows(fence.body(), item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** whether any icon, spotlight or {@code items:} reference in the book is outside the minecraft namespace */
    private boolean modded() {
        if (modded == null) {
            modded = book.entries().values().stream().flatMap(byLocale -> byLocale.values().stream()).anyMatch(text -> {
                List<String> refs = new ArrayList<>();
                text.get("icon").ifPresent(refs::add);
                text.get(ItemLookup.ITEMS).map(ItemLookup::tokens).ifPresent(tokens -> tokens.stream()
                        .filter(token -> !token.startsWith("#")).forEach(refs::add));
                spotlights(text.blocks(), refs);
                return refs.stream().flatMap(ref -> ItemReference.parse(ref).stream())
                        .anyMatch(ref -> !ref.id().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE));
            });
        }
        return modded;
    }

    private static void spotlights(List<Block> blocks, List<String> into) {
        for (Block block : blocks) {
            if (block instanceof Block.Fence fence) {
                if (fence.kind().equals("spotlight")) {
                    into.add(fence.argument());
                }
                spotlights(fence.body(), into);
            }
        }
    }

    /** each of {@code items:}'s references is an item, and each {@code #tag} a tag the game has */
    private void items(String items) {
        for (String token : ItemLookup.tokens(items)) {
            if (token.startsWith("#")) {
                if (ResourceLocation.tryParse(token.substring(1)) != null && ItemLookup.tag(token).isEmpty()) {
                    problem("items: no item tag '" + token + "'");
                }
            } else {
                missing(token).ifPresent(why -> problem("items: " + why));
            }
        }
    }

    private void blocks(List<Block> blocks) {
        for (Block block : blocks) {
            if (block instanceof Block.Paragraph p) {
                text(p.text());
            } else if (block instanceof Block.Heading h) {
                text(h.text());
            } else if (block instanceof Block.ListItem item) {
                text(item.text());
            } else if (block instanceof Block.Illustration i) {
                Icons.problem(i.item().text()).ifPresent(this::problem);
            } else if (block instanceof Block.Fence f) {
                fence(f);
                blocks(f.body());
            } else if (block instanceof Block.Table t) {
                table(t);
            }
            // a page break's style is BookLint's, and an ornament is just an ornament
        }
    }

    /** a table's icons and text; its rows and cells are BookLint's */
    private void table(Block.Table table) {
        List<List<Block.Table.Cell>> rows = new ArrayList<>(table.rows());
        rows.add(table.header());
        for (List<Block.Table.Cell> row : rows) {
            for (Block.Table.Cell cell : row) {
                cell.icons().forEach(icon -> Icons.problem(icon.text()).ifPresent(why -> problem("table icon: " + why)));
                text(cell.text());
            }
        }
    }

    /** whether what a fence names exists; an argument that doesn't parse, and an unknown kind, are BookLint's */
    private void fence(Block.Fence fence) {
        String argument = fence.argument().strip();
        ResourceLocation id = argument.isEmpty() ? null : ResourceLocation.tryParse(argument);
        switch (fence.kind()) {
            case "spotlight" -> missing(argument).ifPresent(why -> problem("spotlight: " + why));
            case "recipe" -> {
                if (id != null) {
                    level.getRecipeManager().byKey(id).ifPresentOrElse(recipe -> {
                        if (RecipeView.of(recipe, level.registryAccess()).isEmpty()) {
                            problem("recipe '" + id + "' has no ingredients to show (its ingredients are worked out in code)");
                        }
                    }, () -> problem("no recipe '" + id + "'"));
                }
            }
            case "image" -> {
                String texture = argument.split("\\s+")[0];
                ResourceLocation image = texture.isEmpty() ? null : ResourceLocation.tryParse(texture);
                if (image != null && !exists(image)) {
                    problem("no image '" + texture + "' (the full path, such as mymod:textures/gui/map.png)");
                }
            }
            case "entity" -> {
                String mob = EntityArgument.parse(argument).id();
                if (ResourceLocation.tryParse(mob) != null) {
                    Optional<EntityType<?>> type = EntityType.byString(mob);
                    if (type.isEmpty()) {
                        problem("no mob '" + mob + "'");
                    } else if (!(type.get().create(level) instanceof LivingEntity)) {
                        problem("'" + mob + "' isn't a mob; entity blocks draw living entities only");
                    }
                }
            }
            default -> { }
        }
    }

    /** a run of text's fonts; its links are BookLint's */
    private void text(Component text) {
        text.visit((style, run) -> {
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

    private boolean exists(ResourceLocation resource) {
        return resources.computeIfAbsent(resource,
                id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
    }

    private void problem(String message) {
        problems.add(new Problem(file, message));
    }
}
