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

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What a {@code ```recipe} block draws: the input slots in rows of {@code columns}, the station that makes it under
 * them (a furnace, a stonecutter; none for crafting), an arrow and the result. Crafting fills a 3x3 grid as the table
 * does; cooking shows its one input with the cooking time under the arrow; smithing shows template, base and addition.
 * Any other recipe, a mod's included, shows its ingredients in a row, which suits most machines.
 *
 * @param slots each slot's items (cycled through when an ingredient accepts several); null or empty for an empty slot
 * @param note  under the arrow (a cooking time); null for none
 *
 * @author Mark Gottschling on 10/27/2026
 */
record RecipeView(int columns, List<ItemStack[]> slots, ItemStack station, ItemStack result, @Nullable Component note) {

    int rows() {
        return Math.max(1, (slots.size() + columns - 1) / columns);
    }

    /** empty for a recipe with nothing to show, such as dyeing armour, whose ingredients are worked out in code */
    static Optional<RecipeView> of(Recipe<?> recipe, HolderLookup.Provider registries) {
        ItemStack result = recipe.getResultItem(registries);
        if (recipe instanceof CraftingRecipe) {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.isEmpty()) {
                return Optional.empty();
            }
            int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            List<ItemStack[]> grid = new ArrayList<>(Arrays.asList(new ItemStack[9][]));
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                int cell = (i / width) * 3 + i % width;
                if (cell < 9) {
                    grid.set(cell, ingredients.get(i).getItems());
                }
            }
            return Optional.of(new RecipeView(3, grid, ItemStack.EMPTY, result, null));
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            return Optional.of(new RecipeView(1, slots(cooking.getIngredients()), cooking.getToastSymbol(), result,
                    Component.translatable("gottschbooks.recipe.time", seconds(cooking.getCookingTime()))));
        }
        if (recipe instanceof SmithingTransformRecipe smithing) {
            return Optional.of(new RecipeView(3, slots(List.of(smithing.template, smithing.base, smithing.addition)),
                    smithing.getToastSymbol(), result, null));
        }
        if (recipe instanceof SmithingTrimRecipe smithing) {
            return Optional.of(new RecipeView(3, slots(List.of(smithing.template, smithing.base, smithing.addition)),
                    smithing.getToastSymbol(), result, null));
        }
        // stonecutting, and anything else: its ingredients, three to a row
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return Optional.empty();
        }
        // a recipe that doesn't name its station reports the crafting table, which would be wrong here
        ItemStack station = recipe.getToastSymbol();
        if (station.is(Items.CRAFTING_TABLE)) {
            station = ItemStack.EMPTY;
        }
        return Optional.of(new RecipeView(Math.min(3, ingredients.size()), slots(ingredients), station, result, null));
    }

    private static List<ItemStack[]> slots(List<Ingredient> ingredients) {
        return ingredients.stream().map(Ingredient::getItems).toList();
    }

    /** 200 ticks is "10", 50 is "2.5" */
    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }
}
