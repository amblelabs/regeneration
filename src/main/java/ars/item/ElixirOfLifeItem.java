package ars.item;

import ars.api.RegenerationCapable;
import ars.core.RegenerationCore;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElixirOfLifeItem extends Item {

    public ElixirOfLifeItem(Settings settings) {
        super(settings.food(new FoodComponent.Builder().alwaysEdible().build()));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof RegenerationCapable capable) {
            if (!capable.isTimelord()) {
                capable.setTimelord(true);
            }

            RegenerationCore info = capable.getRegenerationInfo();
            if (info != null) {
                info.setUsesLeft(RegenerationCore.MAX_REGENERATIONS);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.ITEM_TOTEM_USE, user.getSoundCategory(), 1.0F, 1.0F);
            }
        }

        return super.finishUsing(stack, world, user);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.timelordregen.elixir_of_life.desc")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.GRAY)
                        .withItalic(true)));
    }

    @Override
    public SoundEvent getEatSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }
}