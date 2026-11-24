package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class CrystalOptimizerModule extends ClientModule {

    private static final int SAMPLE_CAPACITY = 64;

    private final ItemStack[] heldSamples = new ItemStack[64];

    private final int[] sampleTicks = new int[64];

    private int sampleCursor;

    private int sampleCount;

    private int ticks;

    private int trackedSince;

    private ClientPlayerEntity trackedPlayer;

    private ClientWorld trackedLevel;

    private boolean attackSent;

    private EndCrystalEntity pendingRemoval;

    private final CrystalOptimizerModule.AttackProbe attackProbe =
            new CrystalOptimizerModule.AttackProbe();

    private final Int2ObjectMap<CrystalOptimizerModule.Ghost> ghosts =
            new Int2ObjectOpenHashMap<>();

    private final IntSet stubborn = new IntOpenHashSet();

    private final IntSet serverRemoved = new IntOpenHashSet();

    public CrystalOptimizerModule() {
        super(
                "crystal-optimizer",
                "Crystal Optimizer",
                "Removes crystals client-side as soon as your hit breaks them.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.reset();
    }

    @Override
    protected void onDisable() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null && world == this.trackedLevel) {
            for (CrystalOptimizerModule.Ghost ghost : this.ghosts.values()) {
                revive(world, ghost.crystal());
            }
        }
        this.reset();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent event) {
        this.attackSent = false;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player != null && world != null) {
            if (player != this.trackedPlayer || world != this.trackedLevel) {
                this.reset();
                this.trackedPlayer = player;
                this.trackedLevel = world;
            }
            if (this.pendingRemoval != null && !this.pendingRemoval.isRemoved()) {
                world.removeEntity(this.pendingRemoval.getId(), RemovalReason.DISCARDED);
                this.ghosts.put(
                        this.pendingRemoval.getId(),
                        new CrystalOptimizerModule.Ghost(this.pendingRemoval, this.ticks));
            }
            this.pendingRemoval = null;
            this.ticks++;
            this.serverRemoved.removeIf(i -> world.getEntityById(i) == null);
            this.sample(player.getMainHandStack());
            this.restoreGhosts(client, player, world);
        } else {
            if (this.trackedPlayer != null || this.sampleCount > 0) {
                this.reset();
            }
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.packet instanceof EntitiesDestroyS2CPacket entitiesDestroyPacket) {
            IntListIterator intListIterator = entitiesDestroyPacket.getEntityIds().iterator();
            while (intListIterator.hasNext()) {
                int i = intListIterator.next();
                this.ghosts.remove(i);
                this.stubborn.remove(i);
                if (this.pendingRemoval != null && this.pendingRemoval.getId() == i) {
                    this.pendingRemoval = null;
                }
                if (this.trackedLevel != null
                        && this.trackedLevel.getEntityById(i) instanceof EndCrystalEntity) {
                    this.serverRemoved.add(i);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPacketSend(PacketSendEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            if (event.packet instanceof PlayerInteractEntityC2SPacket playerInteractEntityPacket) {
                this.attackSent = this.attackProbe.isAttack(playerInteractEntityPacket);
            } else if (event.packet
                    instanceof UpdateSelectedSlotC2SPacket updateSelectedSlotPacket) {
                ClientPlayerEntity player = client.player;
                if (player != null
                        && player == this.trackedPlayer
                        && PlayerInventory.isValidHotbarIndex(
                                updateSelectedSlotPacket.getSelectedSlot())) {
                    this.sample(
                            player.getInventory()
                                    .getStack(updateSelectedSlotPacket.getSelectedSlot()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(PlayerAttackEntityEvent event) {
        boolean attackSent2 = this.attackSent;
        this.attackSent = false;
        if (attackSent2) {
            Entity target = event.target;
            if (event.target instanceof EndCrystalEntity) {
                EndCrystalEntity endCrystalEntity = (EndCrystalEntity) target;
                MinecraftClient client = MinecraftClient.getInstance();
                ClientPlayerEntity player = client.player;
                ClientWorld world = client.world;
                if (player != null
                        && world != null
                        && player == this.trackedPlayer
                        && endCrystalEntity.getEntityWorld() == world) {
                    if (!this.stubborn.contains(endCrystalEntity.getId())
                            && !this.serverRemoved.contains(endCrystalEntity.getId())
                            && this.breaksOnServer(client, player, world, endCrystalEntity)) {
                        this.pendingRemoval = endCrystalEntity;
                        return;
                    }
                    return;
                }
                return;
            }
        }
    }

    private void restoreGhosts(
            MinecraftClient client, ClientPlayerEntity player, ClientWorld world) {
        if (!this.ghosts.isEmpty()) {
            int confirmWindow = Crystals.confirmWindow(client, player);
            ObjectIterator objectIterator = this.ghosts.values().iterator();
            while (objectIterator.hasNext()) {
                CrystalOptimizerModule.Ghost ghost =
                        (CrystalOptimizerModule.Ghost) objectIterator.next();
                if (this.ticks - ghost.removedAt() > confirmWindow) {
                    objectIterator.remove();
                    if (revive(world, ghost.crystal())) {
                        this.stubborn.add(ghost.crystal().getId());
                    }
                }
            }
        }
    }

    private static boolean revive(ClientWorld world, EndCrystalEntity endCrystalEntity) {
        if (world.getEntityById(endCrystalEntity.getId()) != null) {
            return false;
        } else {
            EndCrystalEntity endCrystalEntity2 =
                    new EndCrystalEntity(
                            world,
                            endCrystalEntity.getX(),
                            endCrystalEntity.getY(),
                            endCrystalEntity.getZ());
            endCrystalEntity2.setId(endCrystalEntity.getId());
            endCrystalEntity2.setUuid(endCrystalEntity.getUuid());
            endCrystalEntity2.setShowBottom(endCrystalEntity.shouldShowBottom());
            world.addEntity(endCrystalEntity2);
            return true;
        }
    }

    private boolean breaksOnServer(
            MinecraftClient client,
            ClientPlayerEntity player,
            ClientWorld world,
            EndCrystalEntity endCrystalEntity) {
        if (endCrystalEntity.isRemoved()) {
            return false;
        } else if (!player.isSpectator() && !player.isDead() && !player.isUsingRiptide()) {
            ItemStack mainHandStack = player.getMainHandStack();
            if (mainHandStack.isItemEnabled(world.getEnabledFeatures())
                    && !player.isBelowMinimumAttackCharge(mainHandStack, 0)) {
                if (!world.getWorldBorder().contains(endCrystalEntity.getBlockPos())) {
                    return false;
                } else {
                    return !player.canAttackEntityIn(endCrystalEntity.getBoundingBox(), 0.0)
                            ? false
                            : Crystals.enchantsAddDamage(mainHandStack)
                                    || this.recentHandsDealDamage(client, player, mainHandStack);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean recentHandsDealDamage(
            MinecraftClient client, ClientPlayerEntity player, ItemStack stack) {
        if (!Crystals.dealsDamage(player, stack)) {
            return false;
        } else {
            int confirmWindow = Crystals.confirmWindow(client, player);
            boolean bl = this.ticks - this.trackedSince >= confirmWindow;
            ItemStack stack2 = stack;
            int i;
            int j;
            for (j = 0;
                    j < this.sampleCount
                            && this.ticks
                                            - this.sampleTicks[
                                                    i =
                                                            Math.floorMod(
                                                                    this.sampleCursor - 1 - j, 64)]
                                    <= confirmWindow;
                    j++) {
                ItemStack heldSample = this.heldSamples[i];
                if (heldSample != stack2) {
                    if (!Crystals.dealsDamage(player, heldSample)) {
                        return false;
                    }
                    stack2 = heldSample;
                }
            }
            if (j == 64) {
                bl = false;
            }
            return bl || Crystals.dealsDamage(player, ItemStack.EMPTY);
        }
    }

    private void sample(ItemStack stack) {
        this.heldSamples[this.sampleCursor] = stack;
        this.sampleTicks[this.sampleCursor] = this.ticks;
        this.sampleCursor = (this.sampleCursor + 1) % 64;
        if (this.sampleCount < 64) {
            this.sampleCount++;
        }
    }

    private void reset() {
        Arrays.fill(this.heldSamples, null);
        this.sampleCursor = 0;
        this.sampleCount = 0;
        this.trackedSince = this.ticks;
        this.trackedPlayer = null;
        this.trackedLevel = null;
        this.attackSent = false;
        this.pendingRemoval = null;
        this.ghosts.clear();
        this.stubborn.clear();
        this.serverRemoved.clear();
    }

    @Environment(EnvType.CLIENT)
    private static final class AttackProbe implements Handler {

        private boolean attack;

        private AttackProbe() {}

        boolean isAttack(PlayerInteractEntityC2SPacket playerInteractEntityPacket) {
            this.attack = false;
            playerInteractEntityPacket.handle(this);
            return this.attack;
        }

        @Override
        public void interact(Hand hand) {}

        @Override
        public void interactAt(Hand hand, Vec3d pos) {}

        @Override
        public void attack() {
            this.attack = true;
        }
    }

    @Environment(EnvType.CLIENT)
    private record Ghost(EndCrystalEntity crystal, int removedAt) {}
}
