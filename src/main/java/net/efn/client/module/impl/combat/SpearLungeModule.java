package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class SpearLungeModule extends ClientModule {

    private static final int BUFFER_TICKS = 10;

    private static final double POKE_RANGE = 4.4;

    private static final double POKE_MARGIN = 0.125;

    private final KeySetting key = this.addSetting("lunge-key", new KeySetting("Lunge Key", -1));

    private final BooleanSetting reachPoke =
            this.addSetting("reach-poke", new BooleanSetting("Reach Poke", false));

    private final BooleanSetting switchBack =
            this.addSetting("switch-back", new BooleanSetting("Switch Back", true));

    private final SlotReturn slots = new SlotReturn(this);

    private boolean pending;

    private int pendingTicks;

    private ClientPlayerEntity owner;

    public SpearLungeModule() {
        super(
                "spear-swap",
                "Spear Swap",
                "Swaps to your Lunge spear and lunges. Reach Poke stabs with a spear past sword"
                        + " reach.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.pending = false;
        this.owner = MinecraftClient.getInstance().player;
        this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
        this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
        this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onTick);
    }

    @Override
    protected void onDisable() {
        this.pending = false;
        this.owner = null;
        this.slots.drop(MinecraftClient.getInstance(), this.switchBack.get());
    }

    private void onKey(KeyInputEvent event) {
        if (event.action == 1) {
            this.onPress(event.key);
        }
    }

    private void onMouse(MouseInputEvent event) {
        if (event.action == 1) {
            this.onPress(1000 + event.button);
        }
    }

    private void onPress(int i) {
        int key2 = this.key.value();
        if (ModuleKeybinds.isBindable(key2) && i == key2) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null && client.player != null && client.world != null) {
                if (bestSpear(client.player.getInventory(), true) < 0) {
                    ModuleChat.actionBar(
                            this, ModuleChat.body().warn("No Lunge spear").text(" in your hotbar"));
                } else {
                    this.pending = true;
                    this.pendingTicks = 0;
                }
            }
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.slots.tick(client, this.switchBack.get());
        ClientPlayerEntity player = client.player;
        if (player != this.owner) {
            this.owner = player;
            this.pending = false;
        }
        if (player != null && client.world != null && client.interactionManager != null) {
            if (this.pending) {
                this.tryLunge(client, player);
            }
            if (this.reachPoke.get() && MeleeAttacks.clickPending(client)) {
                this.tryPoke(client, player);
            }
        }
    }

    private void tryLunge(MinecraftClient client, ClientPlayerEntity player) {
        if (++this.pendingTicks > 10) {
            this.pending = false;
        } else if (this.canStab(client, player)
                && !player.isTouchingWater()
                && !player.isGliding()
                && !player.hasVehicle()) {
            if (player.getHungerManager().canSprint() || player.getAbilities().allowFlying) {
                PlayerInventory inventory = player.getInventory();
                int bestSpear = bestSpear(inventory, true);
                if (bestSpear < 0) {
                    this.pending = false;
                } else {
                    ItemStack stack = inventory.getStack(bestSpear);
                    if (canJab(client, player, stack) && this.slots.take(player, bestSpear)) {
                        this.pending = false;
                        stab(client, player, stack.get(DataComponentTypes.PIERCING_WEAPON));
                    }
                }
            }
        }
    }

    private void tryPoke(MinecraftClient client, ClientPlayerEntity player) {
        if (this.canStab(client, player)
                && !player.getMainHandStack().contains(DataComponentTypes.PIERCING_WEAPON)) {
            if (MeleeAttacks.crosshairHit(client) == null) {
                PlayerInventory inventory = player.getInventory();
                int bestSpear = bestSpear(inventory, false);
                if (bestSpear >= 0) {
                    ItemStack stack = inventory.getStack(bestSpear);
                    if (canJab(client, player, stack)
                            && enemyPastReach(player)
                            && this.slots.take(player, bestSpear)) {
                        stab(client, player, stack.get(DataComponentTypes.PIERCING_WEAPON));
                    }
                }
            }
        }
    }

    private static void stab(
            MinecraftClient client,
            ClientPlayerEntity player,
            PiercingWeaponComponent piercingWeaponComponent) {
        client.interactionManager.attackWithPiercingWeapon(piercingWeaponComponent);
        if (MeleeAttacks.actedThisTick(player)) {
            player.swingHand(Hand.MAIN_HAND);
            MeleeAttacks.claimTick(client);
        }
    }

    private boolean canStab(MinecraftClient client, ClientPlayerEntity player) {
        return MeleeAttacks.canAct(client)
                        && MeleeAttacks.handsFree(client, player)
                        && !MeleeAttacks.actedThisTick(player)
                ? !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()
                : false;
    }

    private static boolean canJab(
            MinecraftClient client, ClientPlayerEntity player, ItemStack stack) {
        return client.attackCooldown <= 0 && !client.interactionManager.isFlyingLocked()
                ? stack.isItemEnabled(client.world.getEnabledFeatures())
                        && !player.isBelowMinimumAttackCharge(stack, 0)
                : false;
    }

    private static boolean enemyPastReach(ClientPlayerEntity player) {
        Rotation rotation = ServerRotation.sent();
        Vec3d vec =
                rotation != null
                        ? Vec3d.fromPolar(rotation.pitch(), rotation.yaw())
                        : player.getRotationVec(1.0F);
        Vec3d eyePos = player.getEyePos();
        Vec3d vec2 = eyePos.add(vec.multiply(4.4));
        double entityInteractionRange = player.getEntityInteractionRange();
        Box box = player.getBoundingBox().stretch(vec.multiply(4.4)).expand(1.0);
        for (Entity entity : player.getEntityWorld().getOtherEntities(player, box)) {
            Optional optional;
            if (MeleeAttacks.isEnemy(player, entity)
                    && (optional = entity.getBoundingBox().expand(0.125).raycast(eyePos, vec2))
                            .isPresent()
                    && eyePos.distanceTo((Vec3d) optional.get()) > entityInteractionRange
                    && MeleeAttacks.lineIsClear(player, eyePos, (Vec3d) optional.get())) {
                return true;
            }
        }
        return false;
    }

    private static int bestSpear(PlayerInventory playerInventory, boolean bl) {
        int selectedSlot = playerInventory.getSelectedSlot();
        int i = -1;
        int j = 0;
        for (int m = 0; m < PlayerInventory.getHotbarSize(); m++) {
            ItemStack stack = playerInventory.getStack(m);
            if (stack.contains(DataComponentTypes.PIERCING_WEAPON) && !stack.willBreakNextUse()) {
                int lungeLevel = lungeLevel(stack);
                boolean bl2 =
                        !bl
                                ? i < 0 || lungeLevel < j
                                : lungeLevel > j
                                        || lungeLevel > 0 && lungeLevel == j && m == selectedSlot;
                if (bl2) {
                    i = m;
                    j = lungeLevel;
                }
            }
        }
        return i;
    }

    private static int lungeLevel(ItemStack stack) {
        for (Entry enchantmentEntry : stack.getEnchantments().getEnchantmentEntries()) {
            if (((RegistryEntry) enchantmentEntry.getKey()).matchesKey(Enchantments.LUNGE)) {
                return enchantmentEntry.getIntValue();
            }
        }
        return 0;
    }
}
