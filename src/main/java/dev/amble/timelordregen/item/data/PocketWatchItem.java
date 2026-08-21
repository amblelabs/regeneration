package dev.amble.timelordregen.item.data;

import dev.amble.timelordregen.api.RegenerationCapable;
import dev.amble.timelordregen.client.util.ShiftTooltipHelper;
import dev.amble.timelordregen.core.RegenerationCore;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PocketWatchItem extends Item {
    private static final int COOLDOWN_TICKS = 100;
    private static final String OWNER_KEY = "MarkedOwner";
    private static final String CHARGES_KEY = "Charges";
    private static final String OPEN_KEY = "Open";

    private static final int MESSAGE_INTERVAL_TICKS = 200;
    private static final Map<UUID, Long> messageCooldowns = new HashMap<>();

    private static final String[] WHISPER_MESSAGES = {
            "message.timelordregen.pocket_watch.whisper.0",
            "message.timelordregen.pocket_watch.whisper.1",
            "message.timelordregen.pocket_watch.whisper.2",
            "message.timelordregen.pocket_watch.whisper.3",
            "message.timelordregen.pocket_watch.whisper.4",
            "message.timelordregen.pocket_watch.whisper.5",
            "message.timelordregen.pocket_watch.whisper.6",
            "message.timelordregen.pocket_watch.whisper.7",
            "message.timelordregen.pocket_watch.whisper.8",
            "message.timelordregen.pocket_watch.whisper.9",
    };

    public PocketWatchItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isOpen(stack)) {
            setOpen(stack, false);
            messageCooldowns.remove(user.getUuid());
            return TypedActionResult.success(stack);
        }

        if (user.isSneaking()) {
            return transferRegenerations(world, user, stack);
        }

        return openPocketWatch(world, user, stack);
    }

    /**
     * 打开怀表逻辑
     */
    private TypedActionResult<ItemStack> openPocketWatch(World world, PlayerEntity user, ItemStack stack) {
        setOpen(stack, true);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        UUID ownerId = getOwner(stack);

        if (ownerId == null) {
            markOwner(stack, user);
            ownerId = user.getUuid();
        }

        boolean isOwner = ownerId.equals(user.getUuid());

        if (!isOwner) {
            if (getCharges(stack) > 0) {
                messageCooldowns.put(user.getUuid(), world.getTime());
            }
            return TypedActionResult.success(stack);
        }

        int charges = getCharges(stack);
        if (charges > 0 && user instanceof RegenerationCapable capable) {
            RegenerationCore info = capable.getRegenerationInfo();
            if (info != null) {
                int usesLeft = info.getUsesLeft();
                int transferable = Math.min(charges, RegenerationCore.MAX_REGENERATIONS - usesLeft);
                if (transferable > 0) {
                    info.setUsesLeft(usesLeft + transferable);
                    setCharges(stack, charges - transferable);
                    world.playSound(null, user.getX(), user.getY(), user.getZ(),
                            SoundEvents.ITEM_TOTEM_USE, user.getSoundCategory(), 1.0F, 1.0F);
                }
            }
        }

        if (getCharges(stack) > 0) {
            messageCooldowns.put(user.getUuid(), world.getTime());
        }

        return TypedActionResult.success(stack);
    }

    /**
     * 重生次数储存
     */
    private TypedActionResult<ItemStack> transferRegenerations(World world, PlayerEntity user, ItemStack stack) {
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

        int transferable = Math.min(usesLeft, RegenerationCore.MAX_REGENERATIONS - charges);
        if (transferable > 0) {
            info.setUsesLeft(usesLeft - transferable);
            setCharges(stack, charges + transferable);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_TOTEM_USE, user.getSoundCategory(), 1.0F, 1.0F);
        } else {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), user.getSoundCategory(), 0.5F, 1.0F);
            return TypedActionResult.success(stack, false);
        }

        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack, false);
    }

    /**
     * 物品栏 tick
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient()) return;
        if (!isOpen(stack)) return;
        if (!(entity instanceof PlayerEntity player)) return;

        boolean inMainHand = slot == player.getInventory().selectedSlot;
        boolean inOffHand = slot == PlayerInventory.OFF_HAND_SLOT;
        if (!inMainHand && !inOffHand) return;

        int charges = getCharges(stack);
        if (charges <= 0) return;

        long currentTime = world.getTime();
        Long lastMessageTime = messageCooldowns.get(player.getUuid());
        if (lastMessageTime == null || currentTime - lastMessageTime >= MESSAGE_INTERVAL_TICKS) {
            sendMemoryWhisper(player);
            messageCooldowns.put(player.getUuid(), currentTime);
        }
    }

    /**
     * 发送低语
     */
    private static void sendMemoryWhisper(PlayerEntity player) {
        String key = WHISPER_MESSAGES[player.getWorld().random.nextInt(WHISPER_MESSAGES.length)];
        player.sendMessage(
                Text.translatable(key)
                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(true)),
                true
        );
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int charges = getCharges(stack);
        UUID ownerId = getOwner(stack);
        boolean open = isOpen(stack);

        if (open) {
            tooltip.add(Text.translatable("item.timelordregen.pocket_watch.state.open")
                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
        } else {
            tooltip.add(Text.translatable("item.timelordregen.pocket_watch.state.closed")
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        }

        if (ownerId != null && world != null) {
            PlayerEntity owner = world.getPlayerByUuid(ownerId);
            if (owner != null) {
                tooltip.add(Text.translatable("item.timelordregen.pocket_watch.owner", owner.getName())
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
            }
        }
        tooltip.add(Text.translatable("item.timelordregen.pocket_watch.charges", charges, RegenerationCore.MAX_REGENERATIONS)
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
        tooltip.add(Text.translatable("item.timelordregen.pocket_watch.desc.short")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
        Text longDesc = Text.translatable("item.timelordregen.pocket_watch.desc.long")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
        ShiftTooltipHelper.addShiftTooltip(tooltip, longDesc);
    }

    private static void markOwner(ItemStack stack, PlayerEntity player) {
        stack.getOrCreateNbt().putUuid(OWNER_KEY, player.getUuid());
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains(OWNER_KEY)) {
            return stack.getNbt().getUuid(OWNER_KEY);
        }
        return null;
    }

    public static int getCharges(ItemStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains(CHARGES_KEY)) {
            return stack.getNbt().getInt(CHARGES_KEY);
        }
        return 0;
    }

    private static void setCharges(ItemStack stack, int charges) {
        stack.getOrCreateNbt().putInt(CHARGES_KEY, charges);
    }

    public static boolean isOpen(ItemStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains(OPEN_KEY)) {
            return stack.getNbt().getBoolean(OPEN_KEY);
        }
        return false;
    }

    private static void setOpen(ItemStack stack, boolean open) {
        stack.getOrCreateNbt().putBoolean(OPEN_KEY, open);
    }

    public static void onPlayerDisconnect(PlayerEntity player) {
        messageCooldowns.remove(player.getUuid());
    }
}