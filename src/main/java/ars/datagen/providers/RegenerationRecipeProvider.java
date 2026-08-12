package ars.datagen.providers;

import ars.RegenerationMod;
import ars.core.RegenerationModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.block.Block;
import net.minecraft.data.server.recipe.*;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class RegenerationRecipeProvider extends FabricRecipeProvider {
    public RegenerationRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        // ==================== 1. 原木/木头 → 木板（使用 Shapeless） ====================
        addPlanksRecipeShapeless(exporter, RegenerationModBlocks.CADON_LOG, 4);
        addPlanksRecipeShapeless(exporter, RegenerationModBlocks.STRIPPED_CADON_LOG, 4);
        addPlanksRecipeShapeless(exporter, RegenerationModBlocks.CADON_WOOD, 4);
        addPlanksRecipeShapeless(exporter, RegenerationModBlocks.STRIPPED_CADON_WOOD, 4);

        // ==================== 2. 原木 → 木头（Wood）====================
        // 只有这里注册一次，不要放在 addPlanksRecipeShapeless 里
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_WOOD, 1)
                .pattern("##")
                .pattern("##")
                .input('#', RegenerationModBlocks.CADON_LOG)
                .criterion("has_log", conditionsFromItem(RegenerationModBlocks.CADON_LOG))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_wood_from_logs"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.STRIPPED_CADON_WOOD, 1)
                .pattern("##")
                .pattern("##")
                .input('#', RegenerationModBlocks.STRIPPED_CADON_LOG)
                .criterion("has_stripped_log", conditionsFromItem(RegenerationModBlocks.STRIPPED_CADON_LOG))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "stripped_cadon_wood_from_logs"));

        // ==================== 3. 木板 → 楼梯、台阶等（Shaped） ====================
        // 楼梯 (4个)
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_STAIRS, 4)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .input('#', RegenerationModBlocks.CADON_PLANKS)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_stairs_from_planks"));

        // 台阶 (6个)
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_SLAB, 6)
                .pattern("###")
                .input('#', RegenerationModBlocks.CADON_PLANKS)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_slab_from_planks"));

        // 栅栏 (3个)
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_FENCE, 3)
                .pattern("#/#")
                .pattern("#/#")
                .input('#', RegenerationModBlocks.CADON_PLANKS)
                .input('/', Items.STICK)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_fence_from_planks"));

        // 栅栏门 (1个)
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_FENCE_GATE, 1)
                .pattern("/#/")
                .pattern("/#/")
                .input('#', RegenerationModBlocks.CADON_PLANKS)
                .input('/', Items.STICK)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_fence_gate_from_planks"));

        // 门 (3个)
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_DOOR, 3)
                .pattern("##")
                .pattern("##")
                .pattern("##")
                .input('#', RegenerationModBlocks.CADON_PLANKS)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_door_from_planks"));

        // 活板门 (2个)
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_TRAPDOOR, 2)
                .pattern("###")
                .pattern("###")
                .input('#', RegenerationModBlocks.CADON_PLANKS)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_trapdoor_from_planks"));

        // 压力板 (1个)
        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, RegenerationModBlocks.CADON_PRESSURE_PLATE, 1)
                .pattern("##")
                .input('#', RegenerationModBlocks.CADON_PLANKS)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_pressure_plate_from_planks"));

        // 按钮 (1个) - 单输入，使用 Shapeless
        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, RegenerationModBlocks.CADON_BUTTON, 1)
                .input(RegenerationModBlocks.CADON_PLANKS)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "cadon_button_from_planks"));

        // 木棍 (4个) - 单输入，使用 Shapeless
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Items.STICK, 4)
                .input(RegenerationModBlocks.CADON_PLANKS)
                .criterion("has_planks", conditionsFromItem(RegenerationModBlocks.CADON_PLANKS))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "sticks_from_cadon_planks"));

        // ==================== 4. 熔炉配方：原木/木头 → 木炭 ====================
        CookingRecipeJsonBuilder.createSmelting(Ingredient.ofItems(RegenerationModBlocks.CADON_LOG), RecipeCategory.MISC, Items.CHARCOAL, 0.15f, 200)
                .criterion("has_log", conditionsFromItem(RegenerationModBlocks.CADON_LOG))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "charcoal_from_cadon_log"));

        CookingRecipeJsonBuilder.createSmelting(Ingredient.ofItems(RegenerationModBlocks.STRIPPED_CADON_LOG), RecipeCategory.MISC, Items.CHARCOAL, 0.15f, 200)
                .criterion("has_stripped_log", conditionsFromItem(RegenerationModBlocks.STRIPPED_CADON_LOG))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "charcoal_from_stripped_cadon_log"));

        CookingRecipeJsonBuilder.createSmelting(Ingredient.ofItems(RegenerationModBlocks.CADON_WOOD), RecipeCategory.MISC, Items.CHARCOAL, 0.15f, 200)
                .criterion("has_wood", conditionsFromItem(RegenerationModBlocks.CADON_WOOD))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "charcoal_from_cadon_wood"));

        CookingRecipeJsonBuilder.createSmelting(Ingredient.ofItems(RegenerationModBlocks.STRIPPED_CADON_WOOD), RecipeCategory.MISC, Items.CHARCOAL, 0.15f, 200)
                .criterion("has_stripped_wood", conditionsFromItem(RegenerationModBlocks.STRIPPED_CADON_WOOD))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID, "charcoal_from_stripped_cadon_wood"));
    }

    // 只负责：原木/木头 → 木板 的 shapeless 配方
    private void addPlanksRecipeShapeless(Consumer<RecipeJsonProvider> exporter, Block logBlock, int count) {
        ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RegenerationModBlocks.CADON_PLANKS, count)
                .input(logBlock)
                .criterion("has_log", conditionsFromItem(logBlock))
                .offerTo(exporter, new Identifier(RegenerationMod.MOD_ID,
                        "planks_from_" + fixupBlockKey(logBlock.getTranslationKey())));
    }

    private String fixupBlockKey(String key) {
        return key.substring(key.lastIndexOf(".") + 1);
    }
}