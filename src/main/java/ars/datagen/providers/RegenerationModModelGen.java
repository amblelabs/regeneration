package ars.datagen.providers;

import dev.amble.ait.AITMod;
import dev.amble.lib.datagen.model.AmbleModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.data.family.BlockFamily;
import net.minecraft.item.Item;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegenerationModModelGen extends AmbleModelProvider {
    private final List<Block> directionalBlocksToRegister = new ArrayList<>();
    private final List<BlockSetRecord> blockSetToRegister = new ArrayList<>();
    private final List<Block> simpleBlocksToRegister = new ArrayList<>();
    private final List<Pair<Block, Block>> logBlockToRegister = new ArrayList<>();

    public RegenerationModModelGen (FabricDataOutput output) {
        super(output);
    }

    private static Model item(String modid, String parent, TextureKey... requiredTextureKeys) {
        return new Model(Optional.of(new Identifier(modid, "item/" + parent)), Optional.empty(), requiredTextureKeys);
    }

    private static Model item(String parent, TextureKey... requiredTextureKeys) {
        return item(AITMod.MOD_ID, parent, requiredTextureKeys);
    }

    private static Model item(TextureKey... requiredTextureKeys) {
        return item("minecraft", "generated", requiredTextureKeys);
    }

    private static Model item(String name) {
        return item(name, TextureKey.LAYER0);
    }

    private static String getItemName(Item item) {
        return item.getTranslationKey().split("\\.")[2];
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator generator) {
        for (Block block : directionalBlocksToRegister) {
            generator.blockStateCollector.accept(MultipartBlockStateSupplier.create(block).with(
                    When.create().set(Properties.HORIZONTAL_FACING, Direction.NORTH),
                    BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R0)));
        }

        for (Block block : simpleBlocksToRegister) {
            generator.registerSimpleCubeAll(block);
        }

        for (Pair<Block, Block> blockPair : logBlockToRegister) {
            Block log = blockPair.getLeft();
            Block wood = blockPair.getRight();
            generator.registerLog(log).log(log).wood(wood);
        }

        for (BlockSetRecord record : blockSetToRegister) {
            BlockStateModelGenerator.BlockTexturePool pool = generator.registerCubeAllModelTexturePool(record.block());
            pool.family(new BlockFamily.Builder(record.block())
                    .stairs(record.stairs())
                    .button(record.button())
                    .slab(record.slab())
                    .fence(record.fence())
                    .fenceGate(record.fenceGate())
                    .trapdoor(record.trapdoor())
                    .door(record.door())
                    .pressurePlate(record.pressurePlate())
                .build()
            );
        }

        super.generateBlockStateModels(generator);
    }

    @Override
    public void generateItemModels(ItemModelGenerator generator) {
        super.generateItemModels(generator);
    }

    public void registerDirectionalBlock(Block block) {
        directionalBlocksToRegister.add(block);
    }

    public void registerSimpleBlock(Block block) {
        simpleBlocksToRegister.add(block);
    }

    public void registerLogBlock(Block log, Block wood) {
        logBlockToRegister.add(new Pair<>(log, wood));
    }

    public void registerBlockSet(BlockSetRecord blockSetRecord) {
        blockSetToRegister.add(blockSetRecord);
    }

    private void registerItem(ItemModelGenerator generator, Item item, String modid) {
        Model model = item(TextureKey.LAYER0);
        model.upload(ModelIds.getItemModelId(item), createTextureMap(item, modid), generator.writer);
    }

    private TextureMap createTextureMap(Item item, String modid) {
        Identifier texture = new Identifier(modid, "item/" + getItemName(item));
        if (!(doesTextureExist(texture))) {
            texture = AITMod.id("item/error");
        }

        return new TextureMap().put(TextureKey.LAYER0, texture);
    }

    public boolean doesTextureExist(Identifier texture) {
        return this.output.getModContainer().findPath("assets/" + texture.getNamespace() + "/textures/" + texture.getPath() + ".png").isPresent();
    }
}

