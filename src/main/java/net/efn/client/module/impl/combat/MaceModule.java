package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class MaceModule extends ClientModule {

    private static final String PICK_DAMAGE = "Damage";

    private static final String PICK_WIND_BURST = "Wind Burst";

    private static final String SMASH_DIRECT = "Direct";

    private static final String SMASH_DELAYED = "Delayed";

    private static final double REACH_MARGIN = 0.35;

    private static final float BREACH_GAIN = 1.15F;

    private final BooleanSetting autoSwap =
            this.addSetting("auto-swap", new BooleanSetting("Auto Swap", true));

    private final ModeSetting pick =
            this.addSetting("pick", new ModeSetting("Pick", "Damage", "Damage", "Wind Burst"));

    private final BooleanSetting breachSwap =
            this.addSetting("breach-swap", new BooleanSetting("Breach Swap", false));

    private final BooleanSetting swapBack =
            this.addSetting("swap-back", new BooleanSetting("Swap Back", true));

    private final BooleanSetting smashAssist =
            this.addSetting("smash-assist", new BooleanSetting("Smash Assist", false));

    private final ModeSetting smashTiming =
            this.addSetting(
                    "smash-timing",
                    new ModeSetting("Smash Timing", "Delayed", "Direct", "Delayed"));

    private final NumberSetting minFall =
            this.addSetting(
                    "min-fall", new NumberSetting("Min Fall", 3.0F, 1.5F, 30.0F).step(0.5F));

    private final BooleanSetting mobs = this.addSetting("mobs", new BooleanSetting("Mobs", false));

    private final SlotReturn slots = new SlotReturn(this);

    private boolean smashed;

    public MaceModule() {
        super(
                "mace",
                "Mace",
                "Swaps to the strongest mace for smash hits, with an optional smash assist.",
                ModuleCategory.COMBAT);
        this.pick.visibleWhen(this.autoSwap::get);
        this.breachSwap.visibleWhen(this.autoSwap::get);
        this.swapBack.visibleWhen(() -> this.autoSwap.get() || this.smashAssist.get());
        this.smashTiming.visibleWhen(this.smashAssist::get);
        this.minFall.visibleWhen(this.smashAssist::get);
        this.mobs.visibleWhen(this.smashAssist::get);
    }

    @Override
    protected void onDisable() {
        this.smashed = false;
        this.slots.drop(MinecraftClient.getInstance(), this.swapBack.get());
    }

    public void beforeAttack(ClientPlayerEntity player, Entity entity) {
        if (this.autoSwap.get() && entity instanceof LivingEntity entity2) {
            if (!MeleeAttacks.shieldBlocks(entity2, player) || MeleeAttacks.shieldBroken(entity2)) {
                boolean shouldDealAdditionalDamage = MaceItem.shouldDealAdditionalDamage(player);
                if (shouldDealAdditionalDamage || this.breachSwap.get()) {
                    int bestMace = this.bestMace(player, entity2, shouldDealAdditionalDamage);
                    if (bestMace >= 0) {
                        if (shouldDealAdditionalDamage
                                || !(expected(
                                                player,
                                                player.getInventory().getStack(bestMace),
                                                entity2)
                                        < expected(player, player.getMainHandStack(), entity2)
                                                * 1.15F)) {
                            this.slots.take(player, bestMace);
                        }
                    }
                }
            }
        }
    }

    private int bestMace(ClientPlayerEntity player, LivingEntity entity, boolean bl) {
        boolean bl2 = bl && this.pick.is("Wind Burst");
        PlayerInventory inventory = player.getInventory();
        int i = -1;
        boolean bl3 = false;
        float f = 0.0F;
        for (int j = 0; j < PlayerInventory.getHotbarSize(); j++) {
            ItemStack stack = inventory.getStack(j);
            if (stack.isOf(Items.MACE)
                    && !stack.willBreakNextUse()
                    && (bl || level(stack, Enchantments.BREACH) != 0)) {
                boolean bl4 = bl2 && level(stack, Enchantments.WIND_BURST) > 0;
                float expected = expected(player, stack, entity);
                if (i < 0 || (!bl3 || bl4) && (bl3 != bl4 || !(expected <= f))) {
                    i = j;
                    bl3 = bl4;
                    f = expected;
                }
            }
        }
        return i;
    }

    private static float expected(ClientPlayerEntity player, ItemStack stack, LivingEntity entity) {
        float charge = MeleeAttacks.charge(player);
        double fallDistance = player.fallDistance;
        float f =
                (float) (1.0 + MeleeAttacks.attackDamage(player.getMainHandStack()))
                        * (0.2F + 0.8F * charge * charge);
        if (stack.isOf(Items.MACE) && MaceItem.shouldDealAdditionalDamage(player)) {
            f +=
                    (float)
                            (smashBonus(fallDistance)
                                    + 0.5 * level(stack, Enchantments.DENSITY) * fallDistance);
        }
        if (MeleeAttacks.wouldCrit(player, entity)) {
            f *= 1.5F;
        }
        int level;
        if ((level = level(stack, Enchantments.SHARPNESS)) > 0) {
            f += charge * (0.5F * level + 0.5F);
        }
        return afterArmor(entity, f, level(stack, Enchantments.BREACH));
    }

    private static double smashBonus(double d) {
        if (d <= 3.0) {
            return 4.0 * d;
        } else {
            return d <= 8.0 ? 12.0 + 2.0 * (d - 3.0) : 22.0 + d - 8.0;
        }
    }

    private static float afterArmor(LivingEntity entity, float f, int i) {
        float armor = entity.getArmor();
        float attributeValue = (float) entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
        float g =
                MathHelper.clamp(armor - f / (2.0F + attributeValue / 4.0F), armor * 0.2F, 20.0F)
                        / 25.0F;
        return f * (1.0F - MathHelper.clamp(g - 0.15F * i, 0.0F, 1.0F));
    }

    private static int level(ItemStack stack, RegistryKey<Enchantment> key) {
        for (Entry enchantmentEntry : stack.getEnchantments().getEnchantmentEntries()) {
            if (((RegistryEntry) enchantmentEntry.getKey()).matchesKey(key)) {
                return enchantmentEntry.getIntValue();
            }
        }
        return 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.isOnGround()) {
            this.smashed = false;
        }
        this.slots.tick(client, this.swapBack.get());
    }

    @EventHandler
    public void onSmashTick(TickEvent event) {
        if (this.smashAssist.get()) {
            this.trySmash(MinecraftClient.getInstance());
        }
    }

    private void trySmash(MinecraftClient client) {
        if (MeleeAttacks.canAct(client)
                && !CombatMacroLock.heldByOther(this)
                && !AutoInvTotemModule.wantsHotbar()) {
            ClientPlayerEntity player = client.player;
            if (!MeleeAttacks.actedThisTick(player)
                    && !MeleeAttacks.clickPending(client)
                    && !MeleeAttacks.rightClickComing(client)
                    && !ServerRotation.pending()
                    && client.attackCooldown <= 0
                    && MeleeAttacks.handsFree(client, player)) {
                if (!(player.getVelocity().y >= 0.0)) {
                    EntityHitResult entityHitResult = MeleeAttacks.crosshairHit(client);
                    LivingEntity entity;
                    Entity entity2;
                    if (entityHitResult != null
                            && (entity2 = entityHitResult.getEntity()) instanceof LivingEntity
                            && MeleeAttacks.isEnemy(player, entity = (LivingEntity) entity2)) {
                        if (entity instanceof PlayerEntity || this.mobs.get()) {
                            if (this.smashReady(player, entity)) {
                                if (MeleeAttacks.shieldBlocks(entity, player)
                                        && !MeleeAttacks.shieldBroken(entity)) {
                                    int bestAxe = ShieldBreakerModule.bestAxe(player);
                                    if (canHoldOneTick(player, entity)
                                            && bestAxe >= 0
                                            && (this.autoSwap.get() || this.swapBack.get())
                                            && this.slots.take(player, bestAxe)) {
                                        if (MeleeAttacks.attack(client, entityHitResult)) {
                                            MeleeAttacks.noteShieldBroken(entity);
                                            MeleeAttacks.claimTick(client);
                                        } else {
                                            this.slots.drop(client, true);
                                        }
                                    }
                                } else if (this.holdsSmash(player, entity)) {
                                    InputControl.dropSprint();
                                    client.attackCooldown = Math.max(client.attackCooldown, 1);
                                } else {
                                    if (MeleeAttacks.attack(client, entityHitResult)) {
                                        this.smashed = true;
                                        MeleeAttacks.claimTick(client);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void noteSmash() {
        this.smashed = true;
    }

    public boolean smashReady(ClientPlayerEntity player, LivingEntity entity) {
        return this.isEnabled()
                && !this.smashed
                && MaceItem.shouldDealAdditionalDamage(player)
                && player.fallDistance >= this.minFall.get()
                && (player.getMainHandStack().isOf(Items.MACE)
                        || this.autoSwap.get() && this.bestMace(player, entity, true) >= 0);
    }

    public boolean holdsSmash(ClientPlayerEntity player, LivingEntity entity) {
        boolean bl =
                MeleeAttacks.charge(player) <= 0.9F
                        && player.getAttackCooldownProgress(1.5F) > 0.9F;
        return canHoldOneTick(player, entity)
                && (this.smashTiming.is("Delayed")
                        || bl
                        || entity.hurtTime > 0
                        || player.isSprinting());
    }

    private static boolean canHoldOneTick(ClientPlayerEntity player, LivingEntity entity) {
        Vec3d velocity = player.getVelocity();
        if (!player.getEntityWorld()
                .isSpaceEmpty(player, player.getBoundingBox().offset(velocity))) {
            return false;
        } else {
            double d = player.getEntityInteractionRange() - 0.35;
            if (d <= 0.0) {
                return false;
            } else {
                Vec3d vec = player.getEyePos().add(velocity);
                Vec3d vec2 =
                        entity.getEntityPos().subtract(entity.lastX, entity.lastY, entity.lastZ);
                Box box = entity.getBoundingBox().offset(vec2).expand(entity.getTargetingMargin());
                if (box.contains(vec)) {
                    return true;
                } else {
                    Optional optional =
                            box.raycast(vec, vec.add(player.getRotationVec(1.0F).multiply(d)));
                    return optional.isPresent()
                            && MeleeAttacks.lineIsClear(player, vec, (Vec3d) optional.get());
                }
            }
        }
    }
}
