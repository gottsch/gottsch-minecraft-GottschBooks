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

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import mod.gottsch.neo.gottschbooks.GottschBooks;
import mod.gottsch.neo.gottschbooks.api.BookItem;
import mod.gottsch.neo.gottschbooks.api.Books;
import mod.gottsch.neo.gottschbooks.api.client.GottschBooksClient;
import mod.gottsch.neo.gottschbooks.core.text.Block;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * "Where is this item explained?": hovering an item in any inventory screen adds a tooltip line naming the manual
 * that explains it, and the lookup key (B by default) opens the manual there, on the page that shows the item. The
 * player must carry the manual, unless its book.md says {@code lookup: always} ({@code lookup: never} turns the
 * lookup off for a book). Locked entries never answer.
 * <p>
 * An entry explains an item when it has a {@code spotlight} of it, a {@code recipe} that makes it, the item as its
 * {@code icon}, or the item (or a {@code #tag} holding it) in its {@code items:} front matter; in that order of
 * preference when several entries could answer, then by book and contents order.
 *
 * @author Mark Gottschling on 10/27/2026
 */
@EventBusSubscriber(modid = GottschBooks.MOD_ID, value = Dist.CLIENT)
public final class ItemLookup {
    /** book.md's key for when a book answers lookups, and its values */
    static final String LOOKUP = "lookup", CARRIED = "carried", ALWAYS = "always", NEVER = "never";
    /** an entry's front matter list of the items it explains, beyond its spotlights, recipes and icon */
    static final String ITEMS = "items";

    public static final KeyMapping KEY = new KeyMapping("key.gottschbooks.lookup", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.gottschbooks");

    /** how an entry shows an item, best first */
    private enum Rank { SPOTLIGHT, RECIPE, ICON, LISTED }

    /** an entry that explains an item */
    record Hit(ResourceLocation book, Manual manual, Manual.Entry entry, Rank rank) {}

    /** a tooltip drawn this long ago still counts as the item under the mouse when the key is pressed */
    private static final long TOOLTIP_MILLIS = 250;
    /** the hint line may be this wide even when the rest of the tooltip is narrower (a short item name) */
    private static final int HINT_MIN_WIDTH = 150;

    // the index, and what it was built from; it is rebuilt when any of them changes
    private static Map<Item, List<Hit>> index = Map.of();
    @Nullable
    private static Map<ResourceLocation, LoadedBook> indexedBooks;
    @Nullable
    private static String indexedLocale;
    @Nullable
    private static Level indexedLevel;

    /** the item whose tooltip was drawn last, and when */
    private static ItemStack tooltipStack = ItemStack.EMPTY;
    private static long tooltipAt;

    private ItemLookup() {}

    /** the entry that explains {@code stack} to {@code player}, if a manual they can use has one they can read */
    public static Optional<Hit> find(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        List<Hit> hits = index().getOrDefault(stack.getItem(), List.of());
        if (hits.isEmpty()) {
            return Optional.empty();
        }
        Set<ResourceLocation> carried = carried(player.getInventory());
        return hits.stream()
                .filter(hit -> hit.entry().unlocked())
                .filter(hit -> carried.contains(hit.book()) || lookup(hit.book()).equals(ALWAYS))
                .findFirst();
    }

    /** book.md's {@code lookup:} value, {@code carried} when it has none */
    private static String lookup(ResourceLocation book) {
        return BookLoader.get(book).flatMap(b -> b.meta().get(LOOKUP))
                .map(value -> value.strip().toLowerCase(Locale.ROOT)).orElse(CARRIED);
    }

    /** the books in {@code inventory}: stacks with a book component, and book items that are always one book */
    private static Set<ResourceLocation> carried(Inventory inventory) {
        Set<ResourceLocation> books = new HashSet<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            Optional<ResourceLocation> id = stack.getItem() instanceof BookItem item ? item.bookId(stack) : Books.getId(stack);
            id.ifPresent(books::add);
        }
        return books;
    }

    // ---- the index ----

    private static Map<Item, List<Hit>> index() {
        Minecraft minecraft = Minecraft.getInstance();
        Map<ResourceLocation, LoadedBook> books = BookLoader.all();
        String locale = minecraft.getLanguageManager().getSelected();
        if (books != indexedBooks || !locale.equals(indexedLocale) || minecraft.level != indexedLevel) {
            indexedBooks = books;
            indexedLocale = locale;
            indexedLevel = minecraft.level;
            index = build(books.values(), locale, minecraft.level);
        }
        return index;
    }

    private static Map<Item, List<Hit>> build(Iterable<LoadedBook> books, String locale, @Nullable Level level) {
        List<LoadedBook> manuals = new ArrayList<>();
        books.forEach(book -> {
            if (book.isManual() && !lookup(book.id()).equals(NEVER)) {
                manuals.add(book);
            }
        });
        manuals.sort(Comparator.comparing(book -> book.id().toString()));

        Map<Item, List<Hit>> built = new HashMap<>();
        for (LoadedBook book : manuals) {
            Manual manual = Manual.of(book, locale);
            for (Manual.Entry entry : manual.allEntries()) {
                // the entry's best rank for each item it shows
                Map<Item, Rank> shown = new HashMap<>();
                spotlightsAndRecipes(entry.text().blocks(), level, shown);
                Icons.stack(entry.icon()).ifPresent(stack -> shown.putIfAbsent(stack.getItem(), Rank.ICON));
                entry.text().get(ITEMS).ifPresent(items -> listed(items).forEach(item -> shown.putIfAbsent(item, Rank.LISTED)));
                shown.remove(Items.AIR);
                shown.forEach((item, rank) -> built.computeIfAbsent(item, k -> new ArrayList<>())
                        .add(new Hit(book.id(), manual, entry, rank)));
            }
        }
        // stable, so book and contents order break ties
        built.values().forEach(hits -> hits.sort(Comparator.comparing(Hit::rank)));
        return built;
    }

    private static void spotlightsAndRecipes(List<Block> blocks, @Nullable Level level, Map<Item, Rank> shown) {
        for (Block block : blocks) {
            if (block instanceof Block.Fence fence) {
                if (fence.kind().equals("spotlight")) {
                    Icons.stack(fence.argument()).ifPresent(stack -> shown.merge(stack.getItem(), Rank.SPOTLIGHT, ItemLookup::better));
                } else if (fence.kind().equals("recipe") && level != null) {
                    recipeResult(fence.argument(), level).ifPresent(stack -> shown.merge(stack.getItem(), Rank.RECIPE, ItemLookup::better));
                }
                spotlightsAndRecipes(fence.body(), level, shown);
            }
        }
    }

    private static Rank better(Rank a, Rank b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    static Optional<ItemStack> recipeResult(String argument, Level level) {
        ResourceLocation id = ResourceLocation.tryParse(argument.strip());
        if (id == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().byKey(id).map(holder -> holder.value().getResultItem(level.registryAccess()))
                .filter(stack -> !stack.isEmpty());
    }

    /** {@code items:}'s item references and {@code #tags}, separated by commas or spaces */
    static List<String> tokens(String items) {
        return Arrays.stream(items.split("[,\\s]+")).filter(token -> !token.isEmpty()).toList();
    }

    /** the items {@code #ns:tag} holds; empty when it isn't a tag the game has */
    static Optional<List<Item>> tag(String token) {
        ResourceLocation tag = ResourceLocation.tryParse(token.substring(1));
        return tag == null ? Optional.empty() : BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag))
                .map(set -> set.stream().map(holder -> holder.value()).toList());
    }

    private static List<Item> listed(String items) {
        List<Item> list = new ArrayList<>();
        for (String token : tokens(items)) {
            if (token.startsWith("#")) {
                tag(token).ifPresent(list::addAll);
            } else {
                Icons.stack(token).ifPresent(stack -> list.add(stack.getItem()));
            }
        }
        return list;
    }

    // ---- the tooltip line and the key ----

    /**
     * Adds the hint line, early, so mods that fit a tall tooltip to the screen later in this event (Magic Treasures)
     * measure it too. It is left out while SHIFT is held, which mods use for their long detail views; the key works
     * either way.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !offersLookup(minecraft.screen) || KEY.isUnbound()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        tooltipStack = stack;
        tooltipAt = Util.getMillis();
        if (Screen.hasShiftDown()) {
            return;
        }
        find(stack, minecraft.player).ifPresent(hit -> event.getTooltipElements().add(Either.left(
                oneLine(Component.translatable("gottschbooks.lookup.hint", KEY.getTranslatedKeyMessage(), hit.manual().title())
                        .withStyle(ChatFormatting.GRAY), event, minecraft.font))));
    }

    /**
     * {@code hint}, cut short with "…" to fit the tooltip's width (its widest text line, at least
     * {@link #HINT_MIN_WIDTH}, and never past the event's max width), so it never wraps or widens the tooltip much
     */
    private static FormattedText oneLine(Component hint, RenderTooltipEvent.GatherComponents event, Font font) {
        int width = HINT_MIN_WIDTH;
        for (Either<FormattedText, TooltipComponent> element : event.getTooltipElements()) {
            width = Math.max(width, element.left().map(font::width).orElse(0));
        }
        if (event.getMaxWidth() > 0) {
            width = Math.min(width, event.getMaxWidth());
        }
        if (font.width(hint) <= width) {
            return hint;
        }
        FormattedText ellipsis = Component.literal("…").withStyle(ChatFormatting.GRAY);
        return FormattedText.composite(font.substrByWidth(hint, width - font.width(ellipsis)), ellipsis);
    }

    /** after the screen: a key the screen used (typing in a search box, say) never gets here */
    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = event.getScreen();
        if (minecraft.player == null || !offersLookup(screen) || !KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))
                || Util.getMillis() - tooltipAt > TOOLTIP_MILLIS || typing(screen)) {
            return;
        }
        Optional<Hit> hit = find(tooltipStack, minecraft.player);
        if (hit.isPresent()) {
            GottschBooksClient.open(hit.get().book(), hit.get().entry().id(), tooltipStack.getItem());
            event.setCanceled(true);
        }
    }

    /** any screen but a book, whose own items need no lookup, and chat, whose text a key press is part of */
    private static boolean offersLookup(@Nullable Screen screen) {
        return screen != null && !(screen instanceof BookScreen) && !(screen instanceof ChatScreen);
    }

    private static boolean typing(Screen screen) {
        return screen.children().stream().anyMatch(child -> child instanceof EditBox box && box.isFocused() && box.isVisible());
    }
}
