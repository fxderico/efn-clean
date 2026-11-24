package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.efn.client.module.impl.movement.entitycontrol.VehicleAntiKick;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class BoatNoclipModule extends ClientModule {

    public static volatile BoatNoclipModule active;

    private static final double MAX_MISS = 0.24;

    private static final double MAX_PACKET_LENGTH = 1.0;

    private static final int MAX_PACKETS_PER_TICK = 16;

    private static final double MAX_SERVER_LEAD = 19.0;

    private static final double MAX_UNCONFIRMED = 28.5;

    private static final int STALE_SERVER_TICKS = 40;

    private static final double MIN_BURST = 2.0;

    private static final double MOUNT_BURST = 4.0;

    private static final double BURST_RECOVERY = 0.025;

    private static final double CAUTION_STEP = 0.5;

    private static final double CAUTION_GROWTH = 1.25;

    private static final double LAVA_MARGIN = 3.0;

    private final NumberSetting speed =
            this.addSetting("speed", new NumberSetting("Speed", 10.0F, 0.0F, 320.0F).step(0.5F));

    private final NumberSetting speedInsideBlocks =
            this.addSetting(
                    "speed-inside-blocks",
                    new NumberSetting("Speed Inside Blocks", 5.0F, 0.0F, 320.0F).step(0.5F));

    private final NumberSetting verticalSpeed =
            this.addSetting(
                    "vertical-speed",
                    new NumberSetting("Vertical Speed", 6.0F, 0.0F, 320.0F).step(0.5F));

    private final NumberSetting fallSpeed =
            this.addSetting(
                    "fall-speed", new NumberSetting("Fall Speed", 0.0F, 0.0F, 10.0F).step(0.1F));

    private final BooleanSetting lavaProtection =
            this.addSetting("lava-protection", new BooleanSetting("Lava Protection", true));

    private final BooleanSetting antiKick =
            this.addSetting("anti-fly-kick", new BooleanSetting("Anti Fly Kick", true));

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 40.0F, 1.0F, 80.0F).step(1.0F));

    private final VehicleAntiKick antiKicker = new VehicleAntiKick();

    private AbstractBoatEntity noclipBoat;

    private Vec3d serverPos;

    private Vec3d lastBase;

    private int staleTicks;

    private double cautionCap = 1.0 / 0.0;

    private boolean sendingStops;

    private int sinceBase;

    private double burst = 4.0;

    private double lastMoveLength;

    public BoatNoclipModule() {
        super(
                "boat-noclip",
                "Boat Noclip",
                "Fly your boat through blocks and entities.",
                ModuleCategory.MOVEMENT);
        this.delay.visibleWhen(this.antiKick::get);
        this.antiKick.onChange(this.antiKicker::forget);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        BoatFlyModule module = ModuleRegistry.get(BoatFlyModule.class);
        if (module != null) {
            module.setEnabled(false);
        }
        this.antiKicker.start(this.delay.getInt());
        active = this;
        EntityControlVehicleHook.refresh();
        this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
        this.on(PacketSendEvent.TYPE, this::onPacketSend);
    }

    @Override
    protected void onDisable() {
        active = null;
        EntityControlVehicleHook.refresh();
        if (FreecamModule.active == null) {
            MinecraftClient.getInstance().chunkCullingEnabled = true;
        }
        this.antiKicker.stop();
        this.releaseBoat();
    }

    private void onPreTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (this.noclipBoat != null && (player == null || player.getVehicle() != this.noclipBoat)) {
            this.releaseBoat();
        }
        this.antiKicker.tick();
        this.updateCulling(client);
    }

    private void updateCulling(MinecraftClient client) {
        if (FreecamModule.active == null && client.world != null) {
            boolean bl =
                    this.noclipBoat != null
                            && client.world
                                    .getBlockState(client.gameRenderer.getCamera().getBlockPos())
                                    .isOpaqueFullCube();
            client.chunkCullingEnabled = !bl;
        }
    }

    private void onPacketSend(PacketSendEvent event) {
        if (this.antiKick.get()) {
            Packet packet = event.packet;
            if (event.packet instanceof VehicleMoveC2SPacket) {
                VehicleMoveC2SPacket vehicleMovePacket = (VehicleMoveC2SPacket) packet;
                if (this.sendingStops) {
                    this.antiKicker.observe(vehicleMovePacket);
                } else if (this.antiKicker.onPacketSend(vehicleMovePacket, this.delay.getInt())) {
                    event.setCancelled(true);
                }
                return;
            }
        }
    }

    public void onVehicleSetback() {
        this.antiKicker.forget();
        if (this.noclipBoat != null) {
            this.serverPos = this.noclipBoat.getEntityPos();
        }
        this.cautionCap = 0.5;
    }

    private void releaseBoat() {
        if (this.noclipBoat != null) {
            this.noclipBoat.noClip = false;
        }
        this.noclipBoat = null;
        this.serverPos = null;
        this.lastBase = null;
        this.sinceBase = 0;
        this.burst = 4.0;
    }

    public Vec3d boatMovement(AbstractBoatEntity noclipBoat2, ClientPlayerEntity player) {
        if (this.noclipBoat != noclipBoat2) {
            this.releaseBoat();
            this.noclipBoat = noclipBoat2;
        }
        noclipBoat2.noClip = true;
        forceServerBroadcast(noclipBoat2);
        double serverBudget = this.serverBudget(noclipBoat2);
        if (this.antiKick.get() && this.antiKicker.isHolding()) {
            return Vec3d.ZERO;
        } else {
            serverBudget = Math.min(serverBudget, this.cautionCap);
            this.cautionCap *= 1.25;
            double d =
                    isInsideBlock(noclipBoat2, player)
                            ? this.speedInsideBlocks.get()
                            : this.speed.get();
            Vec3d vec = EntityControlVehicleHook.horizontalVelocity(player, d);
            MinecraftClient client = MinecraftClient.getInstance();
            double e = 0.0;
            if (EntityControlVehicleHook.isPhysicallyDown(client.options.jumpKey)) {
                e += this.verticalSpeed.get() / 20.0;
            }
            double d2;
            double e2;
            e =
                    EntityControlVehicleHook.isPhysicallyDown(client.options.sprintKey)
                            ? (d2 = e - this.verticalSpeed.get() / 20.0)
                            : (e2 = e - this.fallSpeed.get() / 20.0);
            Vec3d vec2 = new Vec3d(vec.x, e, vec.z);
            double d3 = vec2.length();
            if (d3 > serverBudget) {
                vec2 = vec2.multiply(serverBudget / d3);
            }
            Vec3d vec3 = this.planMove(noclipBoat2, vec2);
            this.lastMoveLength = vec3.length();
            return vec3;
        }
    }

    private static void forceServerBroadcast(AbstractBoatEntity abstractBoatEntity) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler != null) {
            networkHandler.sendPacket(
                    new BoatPaddleStateC2SPacket(
                            !abstractBoatEntity.isPaddleMoving(0),
                            abstractBoatEntity.isPaddleMoving(1)));
        }
    }

    private double serverBudget(AbstractBoatEntity abstractBoatEntity) {
        Vec3d vec = abstractBoatEntity.getTrackedPosition().getPos();
        if (!vec.equals(this.lastBase)) {
            this.burst = Math.max(this.burst, (double) (this.sinceBase + 1));
            this.sinceBase = 0;
            this.lastBase = vec;
            this.serverPos = vec;
            this.staleTicks = 0;
        } else {
            if (this.lastMoveLength > 0.01) {
                this.sinceBase++;
            }
            if (++this.staleTicks >= 40) {
                this.serverPos = abstractBoatEntity.getEntityPos();
            }
        }
        this.burst = Math.max(2.0, this.burst - 0.025);
        if (this.serverPos == null) {
            this.serverPos = abstractBoatEntity.getEntityPos();
        }
        double distance = abstractBoatEntity.getEntityPos().distanceTo(this.serverPos);
        return Math.max(0.0, Math.min(19.0 / this.burst, 28.5 - distance));
    }

    private Vec3d planMove(AbstractBoatEntity abstractBoatEntity, Vec3d vec) {
        Vec3d entityPos;
        Vec3d vec2 = entityPos = abstractBoatEntity.getEntityPos();
        Vec3d vec3 = vec;
        List list = this.lavaProtection.get() ? nearbyLava(abstractBoatEntity, vec) : List.of();
        ArrayList list2 = new ArrayList();
        Vec3d vec4;
        Vec3d vec5;
        double allowedFraction;
        while (list2.size() < 16
                && vec3.lengthSquared() > 1.0E-8
                && !((allowedFraction =
                                allowedFraction(
                                        abstractBoatEntity,
                                        vec2,
                                        vec5 = vec3.multiply(Math.min(1.0, 1.0 / vec3.length()))))
                        <= 0.0)
                && !reachesUnloadedChunk(
                        abstractBoatEntity, vec2, vec4 = vec5.multiply(allowedFraction))
                && (list.isEmpty() || !approachesLava(abstractBoatEntity, vec2, vec4, list))) {
            vec2 = vec2.add(vec4);
            vec3 = vec3.subtract(vec4);
            list2.add(vec2);
        }
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler != null) {
            this.sendingStops = true;
            try {
                for (int i = 0; i < list2.size() - 1; i++) {
                    networkHandler.sendPacket(
                            new VehicleMoveC2SPacket(
                                    (Vec3d) list2.get(i),
                                    abstractBoatEntity.getYaw(),
                                    abstractBoatEntity.getPitch(),
                                    false));
                }
            } finally {
                this.sendingStops = false;
            }
        }
        return vec2.subtract(entityPos);
    }

    private static List<Box> nearbyLava(AbstractBoatEntity abstractBoatEntity, Vec3d vec) {
        Box box = abstractBoatEntity.getBoundingBox().stretch(vec).expand(4.0);
        ArrayList list = new ArrayList();
        for (BlockPos pos :
                BlockPos.iterate(
                        MathHelper.floor(box.minX),
                        MathHelper.floor(box.minY),
                        MathHelper.floor(box.minZ),
                        MathHelper.floor(box.maxX),
                        MathHelper.floor(box.maxY),
                        MathHelper.floor(box.maxZ))) {
            if (abstractBoatEntity.getEntityWorld().getFluidState(pos).isIn(FluidTags.LAVA)) {
                list.add(new Box(pos));
            }
        }
        return list;
    }

    private static boolean approachesLava(
            AbstractBoatEntity abstractBoatEntity, Vec3d vec, Vec3d vec2, List<Box> list) {
        Box box =
                abstractBoatEntity
                        .getBoundingBox()
                        .offset(vec.subtract(abstractBoatEntity.getEntityPos()));
        double distance = distanceTo(box.offset(vec2), list);
        return distance < 3.0 && distance < distanceTo(box, list);
    }

    private static double distanceTo(Box box, List<Box> list) {
        double d = 1.7976931348623157E308;
        for (Box box2 : list) {
            double e = Math.max(0.0, Math.max(box2.minX - box.maxX, box.minX - box2.maxX));
            double d2 = Math.max(0.0, Math.max(box2.minY - box.maxY, box.minY - box2.maxY));
            double e2 = Math.max(0.0, Math.max(box2.minZ - box.maxZ, box.minZ - box2.maxZ));
            d = Math.min(d, e * e + d2 * d2 + e2 * e2);
        }
        return Math.sqrt(d);
    }

    private static boolean reachesUnloadedChunk(
            AbstractBoatEntity abstractBoatEntity, Vec3d vec, Vec3d vec2) {
        Box box =
                abstractBoatEntity
                        .getBoundingBox()
                        .offset(vec.subtract(abstractBoatEntity.getEntityPos()))
                        .stretch(vec2);
        for (int i = ChunkSectionPos.getSectionCoordFloored(box.minX);
                i <= ChunkSectionPos.getSectionCoordFloored(box.maxX);
                i++) {
            for (int j = ChunkSectionPos.getSectionCoordFloored(box.minZ);
                    j <= ChunkSectionPos.getSectionCoordFloored(box.maxZ);
                    j++) {
                if (!abstractBoatEntity.getEntityWorld().getChunkManager().isChunkLoaded(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double allowedFraction(
            AbstractBoatEntity abstractBoatEntity, Vec3d vec, Vec3d vec2) {
        Box box =
                abstractBoatEntity
                        .getBoundingBox()
                        .offset(vec.subtract(abstractBoatEntity.getEntityPos()));
        List list;
        if (replayMiss(
                        abstractBoatEntity,
                        box,
                        vec2,
                        list = serverEntityShapes(abstractBoatEntity, box.stretch(vec2)))
                <= 0.24) {
            return 1.0;
        } else {
            double d = 0.0;
            double e = 1.0;
            for (int i = 0; i < 12; i++) {
                double d2 = (d + e) / 2.0;
                if (replayMiss(abstractBoatEntity, box, vec2.multiply(d2), list) <= 0.24) {
                    d = d2;
                } else {
                    e = d2;
                }
            }
            return d;
        }
    }

    private static double replayMiss(
            AbstractBoatEntity abstractBoatEntity, Box box, Vec3d vec, List<VoxelShape> list) {
        Vec3d vec2 =
                Entity.adjustMovementForCollisions(
                        abstractBoatEntity, vec, box, abstractBoatEntity.getEntityWorld(), list);
        return Math.hypot(vec.x - vec2.x, vec.z - vec2.z);
    }

    private static List<VoxelShape> serverEntityShapes(
            AbstractBoatEntity abstractBoatEntity, Box box) {
        ArrayList list = new ArrayList();
        for (Entity entity :
                abstractBoatEntity
                        .getEntityWorld()
                        .getOtherEntities(
                                abstractBoatEntity,
                                box.expand(1.0E-7),
                                EntityPredicates.EXCEPT_SPECTATOR)) {
            if (AbstractBoatEntity.canCollide(abstractBoatEntity, entity)) {
                list.add(VoxelShapes.cuboid(entity.getBoundingBox()));
            }
        }
        return list;
    }

    public static boolean hidesWallOverlay(PlayerEntity player) {
        return active != null && player.getVehicle() instanceof AbstractBoatEntity;
    }

    private static boolean isInsideBlock(
            AbstractBoatEntity abstractBoatEntity, ClientPlayerEntity player) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        } else {
            Box boundingBox = abstractBoatEntity.getBoundingBox();
            Box box = boundingBox.shrink(0.0, 0.05, 0.0).expand(0.5, 0.0, 0.5);
            int i = MathHelper.floor(box.maxX);
            int j = MathHelper.floor(box.maxY);
            int k = MathHelper.floor(box.maxZ);
            Mutable mutable = new Mutable();
            for (int m = MathHelper.floor(box.minX); m <= i; m++) {
                for (int n = MathHelper.floor(box.minY); n <= j; n++) {
                    for (int o = MathHelper.floor(box.minZ); o <= k; o++) {
                        BlockState blockState = world.getBlockState(mutable.set(m, n, o));
                        VoxelShape collisionShape;
                        if (!blockState.isAir()
                                && !(collisionShape =
                                                blockState.getCollisionShape(
                                                        world, mutable, ShapeContext.absent()))
                                        .isEmpty()
                                && boundingBox.intersects(
                                        collisionShape.getBoundingBox().offset(mutable))) {
                            return true;
                        }
                    }
                }
            }
            for (Entity entity : world.getOtherEntities(abstractBoatEntity, box)) {
                if (entity != player
                        && !entity.hasPassenger(abstractBoatEntity)
                        && !abstractBoatEntity.hasPassenger(entity)
                        && entity.isAlive()
                        && entity.getBoundingBox().intersects(box)) {
                    return true;
                }
            }
            return false;
        }
    }
}
