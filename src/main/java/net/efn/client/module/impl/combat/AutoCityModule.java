package net.efn.client.module.impl.combat;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class AutoCityModule extends ClientModule {

    private static final float CRYSTAL_POWER = 6.0F;

    private static final double FACE_INSET = 0.05;

    private static final int GIVE_UP_TICKS = 200;

    private static volatile AutoCityModule active;

    private final SlotReturn slots = new SlotReturn(this);

    private LivingEntity target;

    private BlockPos feet;

    private BlockPos block;

    private int ticks;

    private boolean broken;

    public AutoCityModule() {
        super(
                "auto-city",
                "Auto City",
                "Mines a surround block of the best enemy in a hole, then turns off so Crystal Aura"
                        + " can blow through it.",
                ModuleCategory.COMBAT);
        this.usesNotifications();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.target = null;
        this.feet = null;
        this.block = null;
        this.ticks = 0;
        this.broken = false;
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.broken
                && this.block != null
                && client.interactionManager != null
                && client.interactionManager.isBreakingBlock()) {
            client.interactionManager.cancelBlockBreaking();
        }
        this.slots.drop(client, true);
        CombatMacroLock.release(this);
        this.target = null;
        this.block = null;
    }

    public static boolean mining() {
        AutoCityModule module = active;
        return module != null && module.block != null && !module.broken;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null
                || client.world == null
                || client.interactionManager == null
                || player.isDead()
                || this.broken
                || userClicks(client)) {
            this.requestDisable();
        } else if (this.block == null && !this.pick(client, player)) {
            this.stop("No enemy in a hole within reach");
        } else {
            ClientWorld world = client.world;
            BlockState blockState = world.getBlockState(this.block);
            if (blockState.isReplaceable()) {
                this.opened();
            } else {
                this.ticks++;
                String problem = this.problem(player, blockState);
                if (problem != null) {
                    this.stop(problem);
                } else if (MeleeAttacks.canAct(client)
                        && !AutoInvTotemModule.wantsHotbar()
                        && !player.isUsingItem()
                        && !player.isRiding()) {
                    if (CombatMacroLock.tryAcquire(this)
                            && this.slots.take(
                                    player, toolSlot(player.getInventory(), blockState))) {
                        BlockHitResult blockHitResult = aim(world, player, this.block);
                        if (blockHitResult == null) {
                            this.stop("Lost sight of the block");
                        } else {
                            ServerRotation.request(
                                    RotationUtils.fromTo(
                                            player.getEyePos(), blockHitResult.getPos()));
                            BlockHitResult blockHitResult2 = held(world, player, this.block);
                            if (blockHitResult2 != null && !MeleeAttacks.actedThisTick(player)) {
                                if (client.interactionManager.updateBlockBreakingProgress(
                                        this.block, blockHitResult2.getSide())) {
                                    player.swingHand(Hand.MAIN_HAND);
                                }
                                MeleeAttacks.noteAction(player);
                                if (world.getBlockState(this.block).isReplaceable()) {
                                    this.opened();
                                    this.note(
                                            ModuleChat.body()
                                                    .text("Opened ")
                                                    .name(this.target.getName().getString())
                                                    .text("'s surround"));
                                }
                            }
                        }
                    }
                } else {
                    if (client.interactionManager.isBreakingBlock()) {
                        client.interactionManager.cancelBlockBreaking();
                    }
                    CombatMacroLock.release(this);
                }
            }
        }
    }

    private void opened() {
        this.broken = true;
        CombatMacroLock.release(this);
    }

    private void stop(String string) {
        this.note(ModuleChat.body().text(string));
        this.requestDisable();
    }

    private static boolean userClicks(MinecraftClient client) {
        return client.currentScreen == null
                && (Crystals.isPhysicallyDown(client, client.options.attackKey)
                        || MeleeAttacks.clickPending(client)
                        || Crystals.isPhysicallyDown(client, client.options.useKey));
    }

    private boolean pick(MinecraftClient client, ClientPlayerEntity player) {
        ClientWorld world = client.world;
        float f = 0.0F;
        for (LivingEntity entity : Combo.enemies(client, player)) {
            if (Combo.inHole(entity)) {
                BlockPos pos = entity.getBlockPos();
                for (Direction direction : Type.HORIZONTAL) {
                    BlockPos pos2 = pos.offset(direction);
                    BlockState blockState = world.getBlockState(pos2);
                    float g;
                    Vec3d vec;
                    float estimate;
                    if (!(blockState.getHardness(world, pos2) < 0.0F)
                            && Crystals.isBase(world.getBlockState(pos2.down()))
                            && player.canInteractWithBlockAt(pos2, 0.0)
                            && toolSlot(player.getInventory(), blockState) != -1
                            && aim(world, player, pos2) != null
                            && !CrystalDamage.endangers(
                                    player,
                                    estimate =
                                            CrystalDamage.estimate(
                                                    vec = Crystals.explosionAt(pos2.down()),
                                                    player,
                                                    6.0F,
                                                    pos2))
                            && !((g =
                                            CrystalDamage.estimate(vec, entity, 6.0F, pos2)
                                                    - 0.5F * estimate)
                                    <= f)) {
                        f = g;
                        this.target = entity;
                        this.feet = pos;
                        this.block = pos2;
                    }
                }
            }
        }
        return this.block != null;
    }

    private String problem(ClientPlayerEntity player, BlockState state) {
        BlockPos pos = this.target.getBlockPos();
        if (!this.target.isAlive()
                || pos.getX() != this.feet.getX()
                || pos.getZ() != this.feet.getZ()
                || Math.abs(pos.getY() - this.feet.getY()) > 1) {
            return "Target left the hole";
        } else if (!player.canInteractWithBlockAt(this.block, 0.0)) {
            return "Block went out of reach";
        } else if (toolSlot(player.getInventory(), state) == -1) {
            return "No pickaxe that can break it";
        } else if (!ServerRotation.canRotate(player)) {
            return "Can't aim while flying, swimming or riding";
        } else {
            return this.ticks > 200 ? "Gave up, the block took too long" : null;
        }
    }

    private void note(ModuleChat.Body body) {
        if (this.notificationsOn()) {
            ModuleChat.actionBar(this, body);
        }
    }

    private static int toolSlot(PlayerInventory playerInventory, BlockState state) {
        int i = -1;
        float f = 0.0F;
        for (int j = 0; j < PlayerInventory.getHotbarSize(); j++) {
            ItemStack stack = playerInventory.getStack(j);
            if (stack.isSuitableFor(state) && !(stack.getMiningSpeedMultiplier(state) <= f)) {
                i = j;
                f = stack.getMiningSpeedMultiplier(state);
            }
        }
        return i;
    }

    private static BlockHitResult aim(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        Vec3d eyePos = player.getEyePos();
        double blockInteractionRange = player.getBlockInteractionRange();
        BlockHitResult blockHitResult = null;
        double d = blockInteractionRange * blockInteractionRange;
        for (Direction direction : Direction.values()) {
            Vec3d vec = Vec3d.ofCenter(pos).offset(direction, 0.5);
            double squaredDistance = eyePos.squaredDistanceTo(vec);
            BlockHitResult blockHitResult2;
            if (!(squaredDistance > d)
                    && (blockHitResult2 =
                                            world.raycast(
                                                    new RaycastContext(
                                                            eyePos,
                                                            vec.offset(direction, -0.05),
                                                            ShapeType.OUTLINE,
                                                            FluidHandling.NONE,
                                                            player)))
                                    .getType()
                            == net.minecraft.util.hit.HitResult.Type.BLOCK
                    && blockHitResult2.getBlockPos().equals(pos)
                    && blockHitResult2.getSide() == direction) {
                blockHitResult = blockHitResult2;
                d = squaredDistance;
            }
        }
        return blockHitResult;
    }

    private static BlockHitResult held(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        Rotation rotation = ServerRotation.sent();
        if (rotation == null) {
            return null;
        } else {
            Vec3d eyePos = player.getEyePos();
            BlockHitResult blockHitResult =
                    world.raycast(
                            new RaycastContext(
                                    eyePos,
                                    eyePos.add(
                                            Vec3d.fromPolar(rotation.pitch(), rotation.yaw())
                                                    .multiply(player.getBlockInteractionRange())),
                                    ShapeType.OUTLINE,
                                    FluidHandling.NONE,
                                    player));
            return blockHitResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
                            && blockHitResult.getBlockPos().equals(pos)
                    ? blockHitResult
                    : null;
        }
    }
}
