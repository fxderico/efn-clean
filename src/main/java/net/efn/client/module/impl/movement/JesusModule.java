/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.movement;

import com.example.client.mixin.AccessorJesusServerboundMovePlayerPacket;
import com.example.client.mixin.AccessorServerboundMovePlayerPacket;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.CollisionView;

import java.util.Iterator;

@Environment(value = EnvType.CLIENT)
public final class JesusModule extends ClientModule {

    public static volatile JesusModule active;

    private static final String SOLID = "Solid";

    private static final String IGNORE = "Ignore";

    private static final double ASCEND_SPEED = 0.11;

    private static final double NCP_STRAFE_SPEED = 0.2873;

    private static final int NCP_BOB_INTERVAL = 15;

    private static final int OVER_WATER = 1;

    private static final int OVER_LAVA = 2;

    private final BooleanSetting powderSnow =
            this.addSetting("powder-snow", new BooleanSetting("Powder Snow", true));

    private final BooleanSetting ncpBypass =
            this.addSetting("ncp-bypass", new BooleanSetting("NCP Bypass", false));

    private final BooleanSetting slowDown =
            this.addSetting("slow-down", new BooleanSetting("Slow Down", false));

    private final ModeSetting waterMode =
            this.addSetting(
                    "water-mode", new ModeSetting("Water Mode", "Solid", "Solid", "Ignore"));

    private final BooleanSetting dipIfBurning =
            this.addSetting("dip-if-burning", new BooleanSetting("Dip If Burning", true));

    private final BooleanSetting dipOnSneakWater =
            this.addSetting("water-dip-on-sneak", new BooleanSetting("Water Dip On Sneak", true));

    private final BooleanSetting dipOnFallWater =
            this.addSetting("water-dip-on-fall", new BooleanSetting("Water Dip On Fall", true));

    private final NumberSetting dipFallHeightWater =
            this.addSetting(
                    "water-dip-fall-height",
                    new NumberSetting("Water Dip Fall Height", 4.0f, 1.0f, 255.0f).step(1.0f));

    private final ModeSetting lavaMode =
            this.addSetting("lava-mode", new ModeSetting("Lava Mode", "Solid", "Solid", "Ignore"));

    private final BooleanSetting dipIfFireResistant =
            this.addSetting("dip-if-resistant", new BooleanSetting("Dip If Resistant", true));

    private final BooleanSetting dipOnSneakLava =
            this.addSetting("lava-dip-on-sneak", new BooleanSetting("Lava Dip On Sneak", true));

    private final BooleanSetting dipOnFallLava =
            this.addSetting("lava-dip-on-fall", new BooleanSetting("Lava Dip On Fall", true));

    private final NumberSetting dipFallHeightLava =
            this.addSetting(
                    "lava-dip-fall-height",
                    new NumberSetting("Lava Dip Fall Height", 4.0f, 1.0f, 255.0f).step(1.0f));

    private int ascending = 10;

    private int swimmingTicks;

    private boolean inBubbleColumn;

    private boolean moveIsVelocity;

    public JesusModule() {
        super(
                "jesus",
                "Jesus",
                "Walk on liquids and powder snow like Jesus.",
                ModuleCategory.MOVEMENT);
        this.slowDown.visibleWhen(this.ncpBypass::get);
        this.dipIfBurning.visibleWhen(() -> this.waterMode.is(SOLID));
        this.dipOnSneakWater.visibleWhen(() -> this.waterMode.is(SOLID));
        this.dipOnFallWater.visibleWhen(() -> this.waterMode.is(SOLID));
        this.dipFallHeightWater.visibleWhen(
                () -> this.waterMode.is(SOLID) && this.dipOnFallWater.get());
        this.dipIfFireResistant.visibleWhen(() -> this.lavaMode.is(SOLID));
        this.dipOnSneakLava.visibleWhen(() -> this.lavaMode.is(SOLID));
        this.dipOnFallLava.visibleWhen(() -> this.lavaMode.is(SOLID));
        this.dipFallHeightLava.visibleWhen(
                () -> this.lavaMode.is(SOLID) && this.dipOnFallLava.get());
    }

    @Override
    protected void onEnable() {
        this.swimmingTicks = 0;
        this.inBubbleColumn = false;
        this.moveIsVelocity = false;
        active = this;
        this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
        this.on(PlayerMoveEvent.TYPE, Stage.PRE, EventPriority.HIGHEST, this::captureMoveAliasing);
        this.on(PlayerMoveEvent.TYPE, this::onMove);
        this.on(PacketSendEvent.TYPE, this::onPacketSend);
    }

    @Override
    protected void onDisable() {
        active = null;
        this.inBubbleColumn = false;
        this.moveIsVelocity = false;
        this.swimmingTicks = 0;
    }

    private void onPostTick(TickEvent event) {
        Entity entity;
        boolean inBubbleColumn2 = this.inBubbleColumn;
        this.inBubbleColumn = false;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        if (player.isInSwimmingPose()) {
            return;
        }
        if (player.isTouchingWater() && !this.waterShouldBeSolid(player)) {
            return;
        }
        if (player.isInLava() && !this.lavaShouldBeSolid(player)) {
            return;
        }
        Entity entity2 = entity = player.hasVehicle() ? player.getVehicle() : player;
        if (entity == null) {
            return;
        }
        if (inBubbleColumn2) {
            if (client.options.jumpKey.isPressed() && entity.getVelocity().y < 0.11) {
                JesusModule.setVelocityY(entity, 0.11);
            }
            return;
        }
        if (entity.isTouchingWater() || entity.isInLava()) {
            JesusModule.setVelocityY(entity, 0.11);
            this.ascending = 0;
            return;
        }
        BlockState blockState = client.world.getBlockState(entity.getBlockPos().down());
        boolean bl = blockState.get(Properties.WATERLOGGED, false);
        if (this.ascending == 0) {
            JesusModule.setVelocityY(entity, 0.11);
        } else if (this.ascending == 1
                && (blockState.isOf(Blocks.WATER) || blockState.isOf(Blocks.LAVA) || bl)) {
            JesusModule.setVelocityY(entity, 0.0);
        }
        ++this.ascending;
    }

    private static void setVelocityY(Entity entity, double d) {
        Vec3d velocity = entity.getVelocity();
        entity.setVelocity(velocity.x, d, velocity.z);
    }

    private void captureMoveAliasing(PlayerMoveEvent event) {
        this.moveIsVelocity = false;
        if (!this.ncpBypass.get()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        Vec3d velocity = player.getVelocity();
        this.moveIsVelocity =
                velocity.x == event.x && velocity.y == event.y && velocity.z == event.z;
    }

    private void onMove(PlayerMoveEvent event) {
        if (!this.ncpBypass.get()) {
            return;
        }
        boolean moveIsVelocity2 = this.moveIsVelocity;
        this.moveIsVelocity = false;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        int overLiquid = JesusModule.overLiquid(player);
        boolean bl = (overLiquid & 1) != 0 && this.waterShouldBeSolid(player);
        boolean bl2 = (overLiquid & 2) != 0 && this.lavaShouldBeSolid(player);
        boolean bl3 = bl2;
        if (!bl && !bl2) {
            this.swimmingTicks = 0;
            return;
        }
        if (++this.swimmingTicks < 15) {
            if (player.isOnGround()) {
                JesusModule.applyStrafe(
                        client, player, event, JesusModule.isMoving(player) ? 0.2873 : 0.0);
                if (moveIsVelocity2) {
                    JesusModule.setVelocityXZ(player, event.x, event.z);
                }
            }
            return;
        }
        this.swimmingTicks = 0;
        if (this.slowDown.get()) {
            event.x = 0.0;
            event.z = 0.0;
            if (moveIsVelocity2) {
                JesusModule.setVelocityXZ(player, 0.0, 0.0);
            }
        }
        JesusModule.setVelocityY(player, 0.08);
        if (moveIsVelocity2) {
            event.y = 0.08;
        }
    }

    private static void setVelocityXZ(Entity entity, double d, double e) {
        entity.setVelocity(d, entity.getVelocity().y, e);
    }

    private void onPacketSend(PacketSendEvent event) {
        Packet<?> packet = event.packet;
        if (!(packet instanceof PlayerMoveC2SPacket)) {
            return;
        }
        PlayerMoveC2SPacket playerMovePacket = (PlayerMoveC2SPacket) packet;
        if (!this.ncpBypass.get()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        if (player.isTouchingWater() && !this.waterShouldBeSolid(player)) {
            return;
        }
        if (player.isInLava() && !this.lavaShouldBeSolid(player)) {
            return;
        }
        int overLiquid = JesusModule.overLiquid(player);
        boolean bl =
                (overLiquid & 1) != 0 && this.waterShouldBeSolid(player)
                        || (overLiquid & 2) != 0 && this.lavaShouldBeSolid(player);
        boolean bl2 = bl;
        if (player.isTouchingWater() || player.isInLava() || player.fallDistance > 3.0 || !bl) {
            return;
        }
        ((AccessorJesusServerboundMovePlayerPacket) ((Object) playerMovePacket))
                .efn$jesusSetOnGround(false);
        if (!player.isOnGround() || !playerMovePacket.changesPosition()) {
            return;
        }
        ((AccessorServerboundMovePlayerPacket) ((Object) playerMovePacket))
                .efn$setY(
                        playerMovePacket.getY(0.0) - (0.02 + 1.0E-4 * (double) this.swimmingTicks));
    }

    private static boolean isMoving(ClientPlayerEntity player) {
        return player.forwardSpeed != 0.0f || player.sidewaysSpeed != 0.0f;
    }

    private static void applyStrafe(
            MinecraftClient client, ClientPlayerEntity player, PlayerMoveEvent event, double d) {
        Vec2f movementInput = player.input.getMovementInput();
        float f = Math.signum(movementInput.y);
        float g = Math.signum(movementInput.x);
        float lerpedYaw = player.getLerpedYaw(client.getRenderTickCounter().getTickProgress(true));
        if (f == 0.0f && g == 0.0f) {
            event.x = 0.0;
            event.z = 0.0;
            return;
        }
        float h = 90.0f * g;
        if (f != 0.0f) {
            h *= f * 0.5f;
        }
        lerpedYaw -= h;
        if (f < 0.0f) {
            lerpedYaw -= 180.0f;
        }
        double e = Math.toRadians(lerpedYaw);
        event.x = -Math.sin(e) * d;
        event.z = Math.cos(e) * d;
    }

    private static int overLiquid(ClientPlayerEntity player) {
        Entity vehicle = player.getVehicle();
        Box box =
                vehicle != null
                        ? player.getBoundingBox().union(vehicle.getBoundingBox())
                        : player.getBoundingBox();
        boolean bl = false;
        boolean bl2 = false;
        Iterator iterator =
                player.getEntityWorld().getStatesInBox(box.offset(0.0, -0.01, 0.0)).iterator();
        while (iterator.hasNext()) {
            BlockState state = (BlockState) iterator.next();
            if (state.isOf(Blocks.WATER) || state.getFluidState().getFluid() == Fluids.WATER) {
                bl = true;
                continue;
            }
            if (state.isOf(Blocks.LAVA)) {
                bl2 = true;
                continue;
            }
            if (state.isAir()) continue;
            return 0;
        }
        return (bl ? 1 : 0) | (bl2 ? 2 : 0);
    }

    private boolean waterShouldBeSolid(ClientPlayerEntity player) {
        if (player.isSpectator() || player.getAbilities().flying) {
            return false;
        }
        if (player.getVehicle() instanceof AbstractBoatEntity) {
            return false;
        }
        if (FlyModule.active != null) {
            return false;
        }
        if (this.dipIfBurning.get() && player.isOnFire()) {
            return false;
        }
        if (this.dipOnSneakWater.get()
                && MinecraftClient.getInstance().options.sneakKey.isPressed()) {
            return false;
        }
        if (this.dipOnFallWater.get()
                && player.fallDistance > (double) this.dipFallHeightWater.getInt()) {
            return false;
        }
        return this.waterMode.is(SOLID);
    }

    private boolean lavaShouldBeSolid(ClientPlayerEntity player) {
        if (player.isSpectator() || player.getAbilities().flying) {
            return false;
        }
        if (player.getVehicle() instanceof StriderEntity) {
            return false;
        }
        if (this.isLavaDangerous(player) && this.lavaMode.is(SOLID)) {
            return true;
        }
        if (this.dipOnSneakLava.get()
                && MinecraftClient.getInstance().options.sneakKey.isPressed()) {
            return false;
        }
        if (this.dipOnFallLava.get()
                && player.fallDistance > (double) this.dipFallHeightLava.getInt()) {
            return false;
        }
        return this.lavaMode.is(SOLID);
    }

    private boolean isLavaDangerous(ClientPlayerEntity player) {
        if (!this.dipIfFireResistant.get()) {
            return true;
        }
        StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
        if (statusEffect == null) {
            return true;
        }
        return !((double) statusEffect.getDuration()
                > 300.0 * player.getAttributeValue(EntityAttributes.BURNING_TIME));
    }

    public boolean canWalkOnFluid(FluidState fluidState) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isSwimming()) {
            return false;
        }
        Fluid fluid = fluidState.getFluid();
        if ((fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER)
                && this.waterShouldBeSolid(player)) {
            return true;
        }
        return (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA)
                && this.lavaShouldBeSolid(player);
    }

    public VoxelShape collisionShape(
            CollisionView collisionView, BlockState state, BlockPos pos, VoxelShape voxelShape) {
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty()) {
            return voxelShape;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (collisionView != client.world || !client.isOnThread()) {
            return voxelShape;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return voxelShape;
        }
        if ((state.isOf(Blocks.WATER) || fluidState.getFluid() == Fluids.WATER)
                && !player.isTouchingWater()
                && this.waterShouldBeSolid(player)
                && (double) pos.getY() <= player.getY() - 1.0) {
            return VoxelShapes.fullCube();
        }
        if (state.isOf(Blocks.LAVA)
                && !player.isInLava()
                && this.lavaShouldBeSolid(player)
                && (this.isLavaDangerous(player) || (double) pos.getY() <= player.getY() - 1.0)) {
            return VoxelShapes.fullCube();
        }
        return voxelShape;
    }

    public boolean canWalkOnPowderSnow() {
        return this.powderSnow.get();
    }

    public void markInBubbleColumn() {
        this.inBubbleColumn = true;
    }
}
