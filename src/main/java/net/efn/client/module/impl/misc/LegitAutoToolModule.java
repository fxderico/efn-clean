package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.util.antivanish.AntiVanishText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class LegitAutoToolModule extends ClientModule {

    private static final int SWITCH_DELAY_TICKS = 2;

    private static final String SHARD_NAME = "shard pickaxe";

    private static final String SHARD_LORE = "breaks 9 blocks at once";

    private final BooleanSetting drill =
            this.addSetting(
                    "use-drill-for-instabreak", new BooleanSetting("Drill Instabreak", true));

    private final BooleanSetting delayed =
            this.addSetting("delay", new BooleanSetting("Delay", true));

    private BlockPos target;

    private int pending = -1;

    private int delay;

    private int origin = -1;

    private int ourSlot = -1;

    private int returnDelay;

    public LegitAutoToolModule() {
        super(
                "legit-auto-tool",
                "Legit Auto Tool",
                "Auto tool that only switches hotbar slots like a player.",
                ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.reset();
    }

    @Override
    protected void onDisable() {
        if (this.origin != -1 && MinecraftClient.getInstance().player != null) {
            this.select(this.origin);
        }
        this.reset();
    }

    private void reset() {
        this.target = null;
        this.pending = -1;
        this.delay = 0;
        this.origin = -1;
        this.ourSlot = -1;
        this.returnDelay = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            int selectedSlot = player.getInventory().getSelectedSlot();
            if (this.origin != -1 && selectedSlot != this.ourSlot) {
                this.origin = -1;
            }
            BlockPos pos =
                    client.currentScreen == null && client.options.attackKey.isPressed()
                            ? aimedBlock(client)
                            : null;
            BlockState state = pos == null ? null : client.world.getBlockState(pos);
            if (pos != null && !state.isAir()) {
                if (!pos.equals(this.target)) {
                    this.target = pos.toImmutable();
                    this.pending = -1;
                    this.delay = 0;
                }
                int shardSlot = shardSlot(player.getInventory(), state, selectedSlot);
                int drillSlot;
                if (this.drill.get()
                        && (drillSlot = this.drillSlot(client, state, pos, shardSlot)) != -1) {
                    if (drillSlot != selectedSlot) {
                        this.select(drillSlot);
                    }
                    this.pending = -1;
                } else {
                    int bestSlot = this.bestSlot(client, state, pos, selectedSlot, shardSlot);
                    if (bestSlot == selectedSlot) {
                        this.pending = -1;
                    } else {
                        if (this.pending != bestSlot) {
                            this.pending = bestSlot;
                            this.delay = this.switchDelay();
                        }
                        if (this.delay > 0) {
                            this.delay--;
                        } else {
                            this.select(bestSlot);
                            this.pending = -1;
                        }
                    }
                }
            } else {
                this.target = null;
                this.pending = -1;
                this.delay = 0;
                this.giveBack();
            }
        } else {
            this.reset();
        }
    }

    private static BlockPos aimedBlock(MinecraftClient client) {
        HitResult crosshairTarget = client.crosshairTarget;
        BlockHitResult blockHitResult;
        return crosshairTarget instanceof BlockHitResult
                        && (blockHitResult = (BlockHitResult) crosshairTarget).getType()
                                == Type.BLOCK
                ? blockHitResult.getBlockPos()
                : null;
    }

    private int drillSlot(MinecraftClient client, BlockState state, BlockPos pos, int i) {
        if (i != -1) {
            return breakingDelta(client, i, state, pos) >= 1.0F ? i : -1;
        } else {
            PlayerInventory inventory = client.player.getInventory();
            for (int j = 0; j < 9; j++) {
                ItemStack stack = inventory.getStack(j);
                if (stack.isOf(Items.NETHERITE_PICKAXE)
                        && !stack.willBreakNextUse()
                        && breakingDelta(client, j, state, pos) >= 1.0F) {
                    return j;
                }
            }
            return -1;
        }
    }

    private int bestSlot(MinecraftClient client, BlockState state, BlockPos pos, int i, int j) {
        PlayerInventory inventory = client.player.getInventory();
        ItemStack stack = inventory.getStack(i);
        boolean bl = i == this.ourSlot && this.origin != -1 && stack.willBreakNextUse();
        int k = i;
        float f = bl ? -1.0F : breakingDelta(client, i, state, pos);
        for (int m = 0; m < 9; m++) {
            ItemStack stack2 = inventory.getStack(m);
            float breakingDelta;
            if (m != i
                    && !stack2.isEmpty()
                    && !stack2.willBreakNextUse()
                    && (breakingDelta = breakingDelta(client, m, state, pos)) > f) {
                f = breakingDelta;
                k = m;
            }
        }
        return j != -1 && inventory.getStack(k).isIn(ItemTags.PICKAXES) ? j : k;
    }

    private static int shardSlot(PlayerInventory playerInventory, BlockState state, int i) {
        if (isShardFor(playerInventory.getStack(i), state)) {
            return i;
        } else {
            for (int j = 0; j < 9; j++) {
                if (j != i && isShardFor(playerInventory.getStack(j), state)) {
                    return j;
                }
            }
            return -1;
        }
    }

    private static boolean isShardFor(ItemStack stack, BlockState state) {
        return !stack.willBreakNextUse() && stack.isSuitableFor(state) && isShardPickaxe(stack);
    }

    private static boolean isShardPickaxe(ItemStack stack) {
        if (!stack.isIn(ItemTags.PICKAXES)) {
            return false;
        } else if (AntiVanishText.normalize(stack.getName().getString())
                .contains("shard pickaxe")) {
            return true;
        } else {
            for (Text line :
                    stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
                if (AntiVanishText.normalize(line.getString())
                        .contains("breaks 9 blocks at once")) {
                    return true;
                }
            }
            return false;
        }
    }

    private static float breakingDelta(
            MinecraftClient client, int i, BlockState state, BlockPos pos) {
        float hardness = state.getHardness(client.world, pos);
        if (hardness == -1.0F) {
            return 0.0F;
        } else {
            ClientPlayerEntity player = client.player;
            ItemStack stack = player.getInventory().getStack(i);
            float miningSpeedMultiplier = stack.getMiningSpeedMultiplier(state);
            if (miningSpeedMultiplier > 1.0F) {
                double[] doubles = new double[] {0.0};
                stack.applyAttributeModifiers(
                        EquipmentSlot.MAINHAND,
                        (registryEntry, entityAttributeModifier) -> {
                            if (registryEntry == EntityAttributes.MINING_EFFICIENCY) {
                                doubles[0] += entityAttributeModifier.value();
                            }
                        });
                miningSpeedMultiplier += (float) doubles[0];
            }
            boolean bl = !state.isToolRequired() || stack.isSuitableFor(state);
            return withMiningModifiers(player, miningSpeedMultiplier) / hardness / (bl ? 30 : 100);
        }
    }

    private static float withMiningModifiers(ClientPlayerEntity player, float f) {
        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }
        StatusEffectInstance statusEffect;
        if ((statusEffect = player.getStatusEffect(StatusEffects.MINING_FATIGUE)) != null) {
            f *=
                    switch (statusEffect.getAmplifier()) {
                        case 0 -> 0.3F;
                        case 1 -> 0.09F;
                        case 2 -> 0.0027F;
                        default -> 8.1E-4F;
                    };
        }
        f *= (float) player.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
        if (player.isSubmergedIn(FluidTags.WATER)) {
            f *= (float) player.getAttributeValue(EntityAttributes.SUBMERGED_MINING_SPEED);
        }
        if (!player.isOnGround()) {
            f /= 5.0F;
        }
        return f;
    }

    private void select(int selectedSlot) {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        if (this.origin == -1) {
            this.origin = inventory.getSelectedSlot();
        }
        inventory.setSelectedSlot(selectedSlot);
        this.ourSlot = selectedSlot;
        this.returnDelay = this.switchDelay();
    }

    private int switchDelay() {
        return this.delayed.get() ? 2 : 0;
    }

    private void giveBack() {
        if (this.origin != -1) {
            if (this.returnDelay > 0) {
                this.returnDelay--;
            } else {
                this.select(this.origin);
                this.origin = -1;
            }
        }
    }
}
