package ars.item.data;

import ars.api.RegenerationCapable;
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
    private static final int COOLDOWN_TICKS = 100; // 5 seconds
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

        // 检查是否为时间领主
        if (!(user instanceof RegenerationCapable capable) || !capable.isTimelord()) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        // 检查所有权
        UUID ownerId = getOwner(stack);
        if (ownerId != null && !ownerId.equals(user.getUuid())) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        // 首次使用标记主人
        if (ownerId == null) {
            markOwner(stack, user);
        }

        RegenerationCore info = capable.getRegenerationInfo();
        if (info == null) {
            return TypedActionResult.fail(stack);
        }

        int charges = getCharges(stack);
        int usesLeft = info.getUsesLeft();

        // 计算传输方向：哪边少就往哪边补
        int transferable;
        if (charges > usesLeft) {
            // 怀表次数多 → 传给玩家
            transferable = Math.min(charges - usesLeft, RegenerationCore.MAX_REGENERATIONS - usesLeft);
            charges -= transferable;
            usesLeft += transferable;
        } else if (usesLeft > charges) {
            // 玩家次数多 → 传给怀表
            transferable = Math.min(usesLeft - charges, RegenerationCore.MAX_REGENERATIONS - charges);
            usesLeft -= transferable;
            charges += transferable;
        } else {
            // 两边相等，无需传输
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), user.getSoundCategory(), 0.5F, 1.0F);
            return TypedActionResult.success(stack, false);
        }

        // 应用变更
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

        // 所有者信息
        if (ownerId != null && world != null) {
            PlayerEntity owner = world.getPlayerByUuid(ownerId);
            if (owner != null) {
                tooltip.add(Text.translatable("item.timelordregen.pocket_watch.owner", owner.getName())
                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(true)));
            }
        }

        // 存储次数
        tooltip.add(Text.translatable("item.timelordregen.pocket_watch.charges", charges, RegenerationCore.MAX_REGENERATIONS)
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));

        // 使用说明
        tooltip.add(Text.translatable("item.timelordregen.pocket_watch.desc")
                .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(true)));
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
        return 0; // 新怀表默认空容器
    }

    private static void setCharges(ItemStack stack, int charges) {
        stack.getOrCreateNbt().putInt(CHARGES_KEY, charges);
    }
}