// SPDX-FileCopyrightText: 2023 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.theurgy.registry;

import com.klikli_dev.theurgy.Theurgy;
import com.klikli_dev.theurgy.content.item.AlchemicalSaltItem;
import com.klikli_dev.theurgy.util.LevelUtil;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SaltRegistry {
    public static final DeferredRegister.Items SALTS = DeferredRegister.createItems(Theurgy.MODID);

    /**
     * Geological term for sedimentary, rock, soil, etc. Here means Stone, Dirt, Sand, Gravel, Clay, etc
     */
    public static final DeferredItem<AlchemicalSaltItem> STRATA =
            register("strata");
    public static final DeferredItem<AlchemicalSaltItem> MINERAL =
            register("mineral");

    public static final DeferredItem<AlchemicalSaltItem> CROPS =
            register("crops");

    public static <T extends Item> DeferredItem<AlchemicalSaltItem> register(String name) {
        return register(name, () -> new AlchemicalSaltItem(new Item.Properties()));
    }

    public static <T extends Item> DeferredItem<T> register(String name, Supplier<T> sup) {
        return SALTS.register("alchemical_salt_" + name, sup);
    }

    /**
     * We add only those salts that have a recipe to the creative tab.
     * Other salts are registered, but should not be shown to players, as the related items are from mods that are not loaded
     */
    public static void onBuildCreativeModTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == CreativeModeTabRegistry.THEURGY.get()) {
            var level = LevelUtil.getLevelWithoutContext();
            if (level == null) {
                return;
            }

            var recipeManager = level.getRecipeManager();
            var calcinationRecipes = recipeManager.getAllRecipesFor(RecipeTypeRegistry.CALCINATION.get());

            SALTS.getEntries().stream()
                    .map(DeferredHolder::get)
                    .forEach(sulfur -> {
                        calcinationRecipes.stream()
                                .filter(recipe -> recipe.value().getResultItem(level.registryAccess()) != null && recipe.value().getResultItem(level.registryAccess()).getItem() == sulfur)
                                .forEach(recipe -> event.accept(recipe.value().getResultItem(level.registryAccess()).copyWithCount(1)));
                    });
        }
    }
}
