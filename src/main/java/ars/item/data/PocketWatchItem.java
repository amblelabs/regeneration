package ars.item.data;

import ars.api.RegenerationCapable;
import ars.client.util.ShiftTooltipHelper;  // 导入潜行工具类
import ars.core.RegenerationCore;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PocketWatchItem extends Item {
    private static final int COOLDOWN_TICKS = 100;
    private static final String OWNER_KEY = "MarkedOwner";
    private static final String CHARGES_KEY = "Charges";

    public PocketWatchItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof RegenerationCapable capable) || !capable.isTimelord()) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        UUID ownerId = getOwner(stack);
        if (ownerId != null && !ownerId.equals(user.getUuid())) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        if (ownerId == null) {
            markOwner(stack, user);
        }

        RegenerationCore info = capable.getRegenerationInfo();
        if (info == null) {
            return TypedActionResult.fail(stack);
        }

        int charges = getCharges(stack);
        int usesLeft = info.getUsesLeft();

        int transferable;
        if (charges > usesLeft) {
            transferable = Math.min(charges - usesLeft, RegenerationCore.MAX_REGENERATIONS - usesLeft);
            charges -= transferable;
            usesLeft += transferable;
        } else if (usesLeft > charges) {
            transferable = Math.min(usesLeft - charges, RegenerationCore.MAX_REGENERATIONS - charges);
            usesLeft -= transferable;
            charges += transferable;
        } else {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), user.getSoundCategory(), 0.5F, 1.0F);
            return TypedActionResult.success(stack, false);
        }

        info.setUsesLeft(usesLeft);
        setCharges(stack, charges);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_TOTEM_USE, user.getSoundCategory(), 1.0F, 1.0F);

        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int charges = getCharges(stack);
        UUID ownerId = getOwner(stack);

        // 所有者信息（始终显示）
        if (ownerId != null && world != null) {
            PlayerEntity owner = world.getPlayerByUuid(ownerId);
            if (owner != null) {
                tooltip.add(Text.translatable("item.timelordregen.pocket_watch.owner", owner.getName())
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
            }
        }

        // 存储次数（始终显示）
        tooltip.add(Text.translatable("item.timelordregen.pocket_watch.charges", charges, RegenerationCore.MAX_REGENERATIONS)
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));

        // 短提示（始终显示）
        tooltip.add(Text.translatable("item.timelordregen.pocket_watch.desc.short")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));

        // 长描述（按住 Shift 显示）
        Text longDesc = Text.translatable("item.timelordregen.pocket_watch.desc.long")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
        ShiftTooltipHelper.addShiftTooltip(tooltip, longDesc);
    }

    private static void markOwner(ItemStack stack, PlayerEntity player) {
        stack.getOrCreateNbt().putUuid(OWNER_KEY, player.getUuid());
    }

    @Nullable
    private static UUID getOwner(ItemStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains(OWNER_KEY)) {
            return stack.getNbt().getUuid(OWNER_KEY);
        }
        return null;
    }

    private static int getCharges(ItemStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains(CHARGES_KEY)) {
            return stack.getNbt().getInt(CHARGES_KEY);
        }
        return 0;
    }

    private static void setCharges(ItemStack stack, int charges) {
        stack.getOrCreateNbt().putInt(CHARGES_KEY, charges);
    }
}