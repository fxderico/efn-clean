package net.efn.client.module.impl.combat;

import imgui.ImDrawList;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.anchor.AnchorGuard;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.HurtClock;
import net.efn.client.module.impl.combat.crystal.LootGuard;
import net.efn.client.module.impl.combat.crystal.ShotView;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class AnchorAuraModule extends ClientModule implements HudOverlayProvider {

    private static final double REACH_MARGIN = 0.01;

    private static final int ARM_TICKS = 2;

    private static final int HOLD_TICKS = 10;

    private static final int MAX_LEAD_TICKS = 5;

    private static final int AIM_TIMEOUT_TICKS = 12;

    private static final int SKIP_TICKS = 40;

    private final NumberSetting minDamage =
            this.addSetting(
                    "min-damage", new NumberSetting("Min Damage", 6.0F, 0.0F, 20.0F).step(0.5F));

    private final NumberSetting maxSelfDamage =
            this.addSetting(
                    "max-self-damage",
                    new NumberSetting("Max Self Damage", 6.0F, 0.0F, 20.0F).step(0.5F));

    private final BooleanSetting render =
            this.addSetting("render", new BooleanSetting("Render", true));

    private final ColorSetting renderColor =
            this.addSetting("render-color", new ColorSetting("Render Color", -26052));

    private final NumberSetting turnSpeed =
            this.addSetting(
                    "turn-speed", new NumberSetting("Turn Speed", 4.5F, 1.0F, 10.0F).step(0.5F));

    private final BooleanSetting protectLoot =
            this.addSetting("protect-loot", new BooleanSetting("Protect Loot", true));

    private final SlotReturn slots = new SlotReturn(this);

    private final ShotView view = new ShotView();

    private int tick;

    private int holdUntil;

    private BlockPos spent;

    private int spentUntil;

    private float aimTurn;

    private float placedTurn;

    private BlockPos aimingAt;

    private int aimingTicks;

    private BlockPos skipped;

    private int skippedUntil;

    private List<CrystalDamage.Victim> lastTargets;

    private List<CrystalDamage.Victim> lastSpared;

    private int lastRtt;

    private float steerMin;

    private float steerSelf;

    private int steerUntil = -1;

    private float tickMin;

    private float tickSelf;

    public AnchorAuraModule() {
        super(
                "anchor-aura",
                "Anchor Aura",
                "Places, charges and detonates respawn anchors on nearby enemies.",
                ModuleCategory.COMBAT);
        this.renderColor.visibleWhen(this.render::get);
    }

    @Override
    protected void onDisable() {
        this.slots.drop(MinecraftClient.getInstance(), true);
        this.spent = null;
        this.view.clear();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        this.tick++;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            LootGuard.update(client.world, player);
        }
        CrystalAuraModule module;
        if (((module = ModuleRegistry.get(CrystalAuraModule.class)) == null || !module.isEnabled())
                && MeleeAttacks.canAct(client)
                && !player.isSpectator()
                && !AutoInvTotemModule.wantsHotbar()
                && !AutoCityModule.mining()
                && !CombatMacroLock.heldByOther(this)
                && client.world.getRegistryKey() != World.NETHER) {
            HurtClock.tick(player);
            if (this.tick > this.holdUntil) {
                this.slots.tick(client, true);
            }
            if (!MeleeAttacks.actedThisTick(player)
                    && MeleeAttacks.handsFree(client, player)
                    && !player.shouldCancelInteraction()
                    && !MeleeAttacks.rightClickComing(client)) {
                Combo.Shot shot =
                        Combo.pick(
                                this.shots(
                                        client,
                                        player,
                                        Combo.enemies(client, player),
                                        Combo.friends(client, player),
                                        Crystals.rttTicks(client, player),
                                        0.0F));
                if (shot == null) {
                    if (this.spent != null
                            && this.tick < this.spentUntil
                            && client.world.getBlockState(this.spent).isOf(Blocks.RESPAWN_ANCHOR)) {
                        ServerRotation.request(ServerRotation.sent());
                    }
                    if (this.tick > this.holdUntil) {
                        this.release();
                    }
                } else {
                    this.show(shot);
                    if (CombatMacroLock.tryAcquire(this)) {
                        if (!this.ready(client, player, shot)) {
                            BlockPos pos = cellOf(shot);
                            this.aimingTicks = pos.equals(this.aimingAt) ? this.aimingTicks + 1 : 0;
                            this.aimingAt = pos;
                            if (this.aimingTicks >= 12) {
                                this.skipped = pos;
                                this.skippedUntil = this.tick + 40;
                                this.aimingAt = null;
                            } else {
                                ServerRotation.request(
                                        RotationUtils.fromTo(player.getEyePos(), shot.aim()),
                                        this.turnSpeed.get());
                            }
                        } else {
                            this.aimingAt = null;
                            if (shot.route() == -2 || this.slots.take(player, shot.route())) {
                                this.fire(client, player, shot);
                                this.holdUntil = this.tick + 10;
                                ServerRotation.request(ServerRotation.sent());
                            }
                        }
                    }
                }
            }
        } else {
            this.slots.tick(client, true);
            this.release();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            HurtClock.note(player, event.packet);
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (this.render.get()) {
            this.view.draw();
        }
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.render.get() && this.view.showing();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        this.view.label(imDrawList, f, g);
    }

    private static BlockPos cellOf(Combo.Shot shot) {
        BlockPos pos;
        if (shot.plan() instanceof AnchorGuard.Spot spot) {
            pos = spot.pos();
        } else {
            pos = (BlockPos) shot.plan();
        }
        return pos;
    }

    private boolean ruledOut(BlockPos pos) {
        return pos.equals(this.skipped) && this.tick < this.skippedUntil
                || this.protectLoot.get() && LootGuard.near(Vec3d.ofCenter(pos));
    }

    public void show(Combo.Shot shot) {
        if (this.render.get()) {
            BlockPos pos = cellOf(shot);
            Vec3d vec = Vec3d.ofCenter(pos);
            float f = 0.0F;
            for (CrystalDamage.Victim lastTarget : this.lastTargets) {
                f = Math.max(f, lastTarget.damage(vec, 5.0F, pos));
            }
            float damage = this.lastSpared.get(0).damage(vec, 5.0F, pos);
            this.view.show(new Box(pos), this.renderColor.argb(), f, damage);
        }
    }

    public void steer(ClientPlayerEntity player, float steerMin2, float steerSelf2) {
        this.steerMin = steerMin2;
        this.steerSelf = steerSelf2;
        this.steerUntil = player.age + 2;
    }

    public List<Combo.Shot> shots(
            MinecraftClient client,
            ClientPlayerEntity player,
            List<LivingEntity> list,
            List<LivingEntity> list2,
            int lastRtt2,
            float f) {
        ClientWorld world = client.world;
        boolean bl = player.age <= this.steerUntil;
        this.tickMin = bl ? this.steerMin : this.minDamage.get();
        this.tickSelf = bl ? this.steerSelf : this.maxSelfDamage.get();
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHandStack = player.getMainHandStack();
        int detonatorRoute = AnchorGuard.detonatorRoute(inventory, mainHandStack);
        if (world.getRegistryKey() != World.NETHER
                && ServerRotation.canRotate(player)
                && !player.getOffHandStack().isOf(Items.GLOWSTONE)
                && !player.shouldCancelInteraction()
                && !list.isEmpty()
                && detonatorRoute != -1) {
            ArrayList list3 = new ArrayList();
            list3.add(player);
            list3.addAll(list2);
            this.lastRtt = lastRtt2;
            this.lastTargets = victims(list, lastRtt2);
            this.lastSpared = victims(list3, lastRtt2);
            ArrayList list4 = new ArrayList();
            int route = routeOf(inventory, mainHandStack, Items.GLOWSTONE);
            boolean bl2 = false;
            for (BlockPos pos : this.anchors(world, player, list)) {
                boolean bl3;
                int i;
                AnchorAuraModule.Blast blast;
                Vec3d vec;
                if (!this.ruledOut(pos)
                        && (vec = AnchorGuard.anchorAim(world, player, pos)) != null
                        && this.safe(this.lastSpared, pos)
                        && ((blast =
                                                this.blast(
                                                        pos,
                                                        i =
                                                                (bl3 =
                                                                                world.getBlockState(
                                                                                                        pos)
                                                                                                .get(
                                                                                                        RespawnAnchorBlock
                                                                                                                .CHARGES)
                                                                                        > 0)
                                                                        ? 0
                                                                        : 1,
                                                        false))
                                        != null
                                || this.blast(pos, i, true) != null)) {
                    bl2 = true;
                    if (blast != null) {
                        if (bl3) {
                            list4.add(
                                    new Combo.Shot(
                                            Combo.Kind.DETONATE,
                                            vec,
                                            detonatorRoute,
                                            blast.value(),
                                            blast.kills(),
                                            0,
                                            pos));
                        } else if (route != -1
                                && HurtClock.opensIn(blast.source(), lastRtt2) <= 1
                                && (f <= 0.0F || blast.value() > f + 1.0F)) {
                            list4.add(
                                    new Combo.Shot(
                                            Combo.Kind.CHARGE,
                                            vec,
                                            route,
                                            blast.value(),
                                            blast.kills(),
                                            1,
                                            pos));
                        }
                    }
                }
            }
            int route2 = routeOf(inventory, mainHandStack, Items.RESPAWN_ANCHOR);
            Combo.Shot shot;
            if (!bl2
                    && route2 != -1
                    && route != -1
                    && (shot =
                                    this.place(
                                            world,
                                            player,
                                            route2,
                                            Crystals.reservedBoxes(
                                                    player,
                                                    Crystals.confirmWindow(client, player))))
                            != null) {
                list4.add(shot);
            }
            return list4;
        } else {
            return List.of();
        }
    }

    public boolean ready(MinecraftClient client, ClientPlayerEntity player, Combo.Shot shot) {
        ClientWorld world = client.world;
        if (shot.kind() == Combo.Kind.ANCHOR) {
            AnchorGuard.Spot spot = (AnchorGuard.Spot) shot.plan();
            Rotation rotation = ServerRotation.sent();
            if (rotation != null && AnchorGuard.hitAlong(world, player, rotation, spot) != null) {
                return true;
            } else {
                float turn = turnTo(player, spot);
                if (turn > 0.0F) {
                    this.aimTurn = turn;
                }
                return false;
            }
        } else {
            BlockPos pos = (BlockPos) shot.plan();
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isOf(Blocks.RESPAWN_ANCHOR)
                    && AnchorGuard.anchorHit(world, player, pos) != null) {
                boolean bl = blockState.get(RespawnAnchorBlock.CHARGES) > 0;
                return shot.kind() == Combo.Kind.CHARGE
                        ? !bl
                        : bl
                                && this.safe(this.lastSpared, pos)
                                && this.blast(pos, 0, false) != null;
            } else {
                return false;
            }
        }
    }

    public void fire(MinecraftClient client, ClientPlayerEntity player, Combo.Shot shot) {
        ClientWorld world = client.world;
        switch (shot.kind()) {
            case ANCHOR:
                AnchorGuard.Spot spot = (AnchorGuard.Spot) shot.plan();
                AnchorGuard.click(
                        client,
                        player,
                        AnchorGuard.hitAlong(world, player, ServerRotation.sent(), spot));
                if (this.aimTurn > 0.0F) {
                    this.placedTurn = this.aimTurn;
                }
                break;
            case CHARGE:
                AnchorGuard.click(
                        client,
                        player,
                        AnchorGuard.anchorHit(world, player, (BlockPos) shot.plan()));
                break;
            case DETONATE:
                BlockPos pos = (BlockPos) shot.plan();
                AnchorGuard.click(client, player, AnchorGuard.anchorHit(world, player, pos));
                this.spent = pos;
                this.spentUntil = this.tick + Crystals.confirmWindow(client, player);
                Vec3d vec = Vec3d.ofCenter(pos);
                for (CrystalDamage.Victim lastTarget : this.lastTargets) {
                    float raw = lastTarget.raw(vec, 5.0F, pos);
                    if (raw > 0.0F) {
                        HurtClock.shot(player, lastTarget.entity(), raw, vec, this.lastRtt);
                    }
                }
        }
    }

    private Combo.Shot place(ClientWorld world, ClientPlayerEntity player, int i, List<Box> list) {
        LinkedHashMap map = new LinkedHashMap();
        for (CrystalDamage.Victim lastTarget : this.lastTargets) {
            BlockPos pos = lastTarget.entity().getBlockPos();
            for (BlockPos pos2 : BlockPos.iterate(pos.add(-2, -1, -2), pos.add(2, 2, 2))) {
                float upperValue;
                if (!map.containsKey(pos2)
                        && world.getBlockState(pos2).isAir()
                        && player.canInteractWithBlockAt(pos2, -0.01)
                        && !reservedAt(list, pos2)
                        && !this.ruledOut(pos2)
                        && !this.walkedInto(world, pos2)
                        && (upperValue = this.upperValue(Vec3d.ofCenter(pos2))) >= 0.0F) {
                    map.put(pos2.toImmutable(), upperValue);
                }
            }
        }
        ArrayList list2 = new ArrayList(map.keySet());
        list2.sort(
                (blockPos, blockPos2) ->
                        Float.compare((Float) map.get(blockPos2), (Float) map.get(blockPos)));
        Vec3d eyePos = player.getEyePos();
        Combo.Shot shot = null;
        for (BlockPos pos3 : (Iterable<BlockPos>) list2) {
            if (shot != null && (Float) map.get(pos3) <= shot.value()) {
                break;
            }
            AnchorAuraModule.Blast blast = this.blast(pos3, 2, true);
            Rotation rotation;
            AnchorGuard.Spot spot;
            float turn;
            if (blast != null
                    && (shot == null || !(blast.value() <= shot.value()))
                    && this.safe(this.lastSpared, pos3)
                    && (spot = AnchorGuard.supportFor(world, player, eyePos, pos3)) != null
                    && !new Box(pos3)
                            .raycast(
                                    eyePos,
                                    eyePos.add(
                                            Vec3d.fromPolar(
                                                            (rotation =
                                                                            ServerRotation.next(
                                                                                    player,
                                                                                    RotationUtils
                                                                                            .fromTo(
                                                                                                    eyePos,
                                                                                                    spot
                                                                                                            .aim())))
                                                                    .pitch(),
                                                            rotation.yaw())
                                                    .multiply(player.getBlockInteractionRange())))
                            .isEmpty()
                    && (!((turn = turnTo(player, spot)) > 2.0F)
                            || !(Math.abs(turn - this.placedTurn) < 1.0E-4F))) {
                shot =
                        new Combo.Shot(
                                Combo.Kind.ANCHOR,
                                spot.aim(),
                                i,
                                blast.value(),
                                blast.kills(),
                                2,
                                spot);
            }
        }
        return shot;
    }

    private Set<BlockPos> anchors(
            ClientWorld world, ClientPlayerEntity player, List<LivingEntity> list) {
        LinkedHashSet set = new LinkedHashSet();
        for (LivingEntity entity : list) {
            BlockPos pos = entity.getBlockPos();
            for (BlockPos pos2 : BlockPos.iterate(pos.add(-4, -3, -4), pos.add(4, 4, 4))) {
                if (world.getBlockState(pos2).isOf(Blocks.RESPAWN_ANCHOR)
                        && player.canInteractWithBlockAt(pos2, -0.01)
                        && (!pos2.equals(this.spent) || this.tick >= this.spentUntil)) {
                    set.add(pos2.toImmutable());
                }
            }
        }
        return set;
    }

    private AnchorAuraModule.Blast blast(BlockPos pos, int i, boolean bl) {
        Vec3d vec = Vec3d.ofCenter(pos);
        LivingEntity entity = null;
        float f = 0.0F;
        boolean bl2 = false;
        for (CrystalDamage.Victim lastTarget : this.lastTargets) {
            LivingEntity entity2 = lastTarget.entity();
            int j = bl ? Math.max(i, HurtClock.opensIn(entity2, this.lastRtt)) : i;
            float effective =
                    HurtClock.effective(
                            lastTarget, lastTarget.raw(vec, 5.0F, pos), j, this.lastRtt);
            if (!(effective < this.needed(entity2))) {
                bl2 = bl2 || Combo.kills(effective, entity2);
                float value2 = Combo.value(effective, entity2);
                if (entity == null || value2 > f) {
                    entity = entity2;
                    f = value2;
                }
            }
        }
        if (entity == null) {
            return null;
        } else {
            float damage = this.lastSpared.get(0).damage(vec, 5.0F, pos);
            return new AnchorAuraModule.Blast(f - 0.5F * damage, bl2, entity);
        }
    }

    private float upperValue(Vec3d vec) {
        float f = -1.0F;
        for (CrystalDamage.Victim lastTarget : this.lastTargets) {
            float upperBound = lastTarget.upperBound(vec, 5.0F);
            if (upperBound >= this.needed(lastTarget.entity())) {
                f = Math.max(f, Combo.value(upperBound, lastTarget.entity()));
            }
        }
        return f;
    }

    private boolean safe(List<CrystalDamage.Victim> list, BlockPos pos) {
        Vec3d vec = Vec3d.ofCenter(pos);
        for (CrystalDamage.Victim victim : list) {
            float damage = victim.damage(vec, 5.0F, pos);
            if (damage > this.tickSelf
                    || CrystalDamage.endangers((PlayerEntity) victim.entity(), damage)) {
                return false;
            }
        }
        return true;
    }

    private float needed(LivingEntity entity) {
        return Math.min(this.tickMin, CrystalDamage.health(entity));
    }

    private static List<CrystalDamage.Victim> victims(List<LivingEntity> list, int i) {
        ArrayList list2 = new ArrayList(list.size());
        for (LivingEntity entity : list) {
            int j = entity instanceof ClientPlayerEntity ? 0 : Math.min(i, 5);
            list2.add(new CrystalDamage.Victim(entity, Combo.lead(entity, j)));
        }
        return list2;
    }

    private boolean walkedInto(ClientWorld world, BlockPos pos) {
        Box box = new Box(pos);
        int i = Math.min(this.lastRtt, 5);
        for (PlayerEntity player : world.getPlayers()) {
            Box boundingBox;
            if (!player.isSpectator()
                    && !(player instanceof ClientPlayerEntity)
                    && ((boundingBox = player.getBoundingBox()).intersects(box)
                            || boundingBox.offset(Combo.lead(player, i)).intersects(box))) {
                return true;
            }
        }
        return false;
    }

    private static boolean reservedAt(List<Box> list, BlockPos pos) {
        for (Box box : list) {
            if (box.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static int routeOf(PlayerInventory playerInventory, ItemStack stack, Item item) {
        if (stack.isOf(item)) {
            return -2;
        } else {
            int hotbarSlot = Crystals.hotbarSlotOf(playerInventory, item);
            return hotbarSlot == -1 ? -1 : hotbarSlot;
        }
    }

    private static float turnTo(ClientPlayerEntity player, AnchorGuard.Spot spot) {
        Rotation rotation = ServerRotation.sent();
        Rotation rotation2 = rotation != null ? rotation : RotationUtils.entityRotation(player);
        Rotation rotation3 =
                ServerRotation.next(player, RotationUtils.fromTo(player.getEyePos(), spot.aim()));
        return Math.abs(rotation3.yaw() - rotation2.yaw());
    }

    private void release() {
        if (!this.slots.holding()) {
            CombatMacroLock.release(this);
        }
    }

    @Environment(EnvType.CLIENT)
    private record Blast(float value, boolean kills, LivingEntity source) {}
}
