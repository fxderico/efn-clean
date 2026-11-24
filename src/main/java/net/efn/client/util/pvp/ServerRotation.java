package net.efn.client.util.pvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public final class ServerRotation {

    private static Rotation requested;

    private static Rotation committed;

    private static Rotation server;

    private static boolean silent;

    private static boolean steered;

    private static ClientPlayerEntity owner;

    private static float ownYaw;

    private static float ownPitch;

    private static boolean swapped;

    private static Rotation walked;

    private static final float MAX_STEP = 25.0F;

    private static final int EASE_TICKS = 10;

    private static final float EASE_SPEED = 5.0F;

    private static final float EASE_DONE = 0.5F;

    private static final float EASE_GIVE_UP = 150.0F;

    private static float requestedSpeed;

    private static Rotation stepped;

    private static boolean requestedLastTick;

    private static int easeLeft;

    private ServerRotation() {}

    public static void request(Rotation rotation) {
        request(rotation, 0.0F);
    }

    public static void request(Rotation rotation, float f) {
        if (requested == null && committed == null) {
            requested = rotation;
            requestedSpeed = f;
        }
    }

    public static boolean pending() {
        return requested != null || committed != null;
    }

    public static Rotation nextStep(ClientPlayerEntity player, Rotation rotation) {
        return step(player, rotation, 0.0F);
    }

    public static Rotation sent() {
        return server;
    }

    public static boolean desynced() {
        return silent;
    }

    public static boolean canRotate(ClientPlayerEntity player) {
        return !player.isGliding()
                && !player.isUsingRiptide()
                && !player.hasVehicle()
                && !player.isSwimming()
                && player.networkHandler.isLoaded();
    }

    public static Rotation next(ClientPlayerEntity player, Rotation rotation) {
        Rotation rotation2 =
                server != null ? server : new Rotation(player.getYaw(), player.getPitch());
        double mouseScale = RotationUtils.mouseScale(MinecraftClient.getInstance(), player);
        float f = (float) mouseScale * 0.15F;
        long l = Math.round(MathHelper.wrapDegrees(rotation.yaw() - rotation2.yaw()) / f);
        long l2 = Math.round((rotation.pitch() - rotation2.pitch()) / f);
        float g = rotation2.yaw() + (float) (l * mouseScale) * 0.15F;
        float h =
                MathHelper.clamp(
                        rotation2.pitch() + (float) (l2 * mouseScale) * 0.15F, -90.0F, 90.0F);
        return new Rotation(g, h);
    }

    public static void startTick() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != owner) {
            owner = player;
            server = null;
        }
        requestedLastTick = requested != null;
        requested = null;
        committed = null;
        stepped = null;
        if (requestedLastTick) {
            easeLeft = 10;
        } else if (easeLeft > 0) {
            easeLeft--;
        }
        if (easeLeft > 0 && (player == null || server == null || closeToCamera(player))) {
            easeLeft = 0;
        }
        if (player != null
                && server != null
                && steered
                && !(Math.abs(player.getYaw() - server.yaw()) <= 180.0F)) {
            float f =
                    server.yaw()
                            + MathHelper.wrapDegrees(player.getYaw() - server.yaw())
                            - player.getYaw();
            player.setYaw(player.getYaw() + f);
            player.lastYaw += f;
            player.renderYaw += f;
            player.lastRenderYaw += f;
            player.headYaw += f;
            player.lastHeadYaw += f;
            player.bodyYaw += f;
            player.lastBodyYaw += f;
        }
    }

    public static void noteSent(Packet<?> packet) {
        PlayerMoveC2SPacket playerMovePacket;
        if (packet instanceof PlayerMoveC2SPacket
                && (playerMovePacket = (PlayerMoveC2SPacket) packet).changesLook()
                && MinecraftClient.getInstance().isOnThread()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            server = new Rotation(playerMovePacket.getYaw(0.0F), playerMovePacket.getPitch(0.0F));
            silent = swapped;
            steered =
                    swapped
                            || player != null
                                    && (server.yaw() != player.getYaw()
                                            || server.pitch() != player.getPitch());
            owner = player;
        }
    }

    public static Rotation commit(ClientPlayerEntity player) {
        if (committed == null) {
            Rotation rotation = canRotate(player) ? target(player) : null;
            committed =
                    rotation != null ? rotation : new Rotation(player.getYaw(), player.getPitch());
        }
        return committed;
    }

    public static boolean isSwapped() {
        return swapped;
    }

    public static void applyForMovement(ClientPlayerEntity player) {
        if (!swapped && canRotate(player)) {
            Rotation rotation = target(player);
            if (rotation != null
                    && (rotation.yaw() != player.getYaw()
                            || rotation.pitch() != player.getPitch())) {
                ownYaw = player.getYaw();
                ownPitch = player.getPitch();
                player.setYaw(rotation.yaw());
                player.setPitch(rotation.pitch());
                walked = rotation;
                swapped = true;
            }
        }
    }

    public static PlayerInput correctInput(PlayerInput playerInput) {
        if (walked == null) {
            return playerInput;
        } else {
            int impulse = impulse(playerInput.forward(), playerInput.backward());
            int impulse2 = impulse(playerInput.left(), playerInput.right());
            if (impulse == 0 && impulse2 == 0) {
                return playerInput;
            } else {
                double[] doubles = heading(ownYaw, impulse2, impulse);
                int i = impulse;
                int j = impulse2;
                double d = -1.0 / 0.0;
                for (int n = -1; n <= 1; n++) {
                    for (int o = -1; o <= 1; o++) {
                        double[] doubles2;
                        double e;
                        if ((n != 0 || o != 0)
                                && (e =
                                                (doubles2 = heading(walked.yaw(), o, n))[0]
                                                                * doubles[0]
                                                        + doubles2[1] * doubles[1])
                                        > d) {
                            d = e;
                            i = n;
                            j = o;
                        }
                    }
                }
                return i == impulse && j == impulse2
                        ? playerInput
                        : new PlayerInput(
                                i > 0,
                                i < 0,
                                j > 0,
                                j < 0,
                                playerInput.jump(),
                                playerInput.sneak(),
                                playerInput.sprint());
            }
        }
    }

    public static boolean misses(ClientPlayerEntity player, Box box, double d) {
        if (server == null || !canRotate(player)) {
            return false;
        } else if (!pending()) {
            endEase();
            return false;
        } else {
            Vec3d eyePos = player.getEyePos();
            if (box.contains(eyePos)) {
                return false;
            } else {
                Rotation rotation = target(player);
                if (rotation == null) {
                    rotation = new Rotation(player.getYaw(), player.getPitch());
                }
                return !hits(eyePos, box, d, rotation.yaw(), rotation.pitch())
                        && !hits(eyePos, box, d, server.yaw(), rotation.pitch())
                        && !hits(eyePos, box, d, server.yaw(), server.pitch());
            }
        }
    }

    public static boolean missesBlock(ClientPlayerEntity player, Box box, double d) {
        if (server == null) {
            return false;
        } else {
            if (!pending()) {
                endEase();
            }
            if (!silent) {
                if (!pending() || !canRotate(player)) {
                    return false;
                }
                Rotation rotation = target(player);
                if (rotation == null
                        || rotation.yaw() == player.getYaw()
                                && rotation.pitch() == player.getPitch()) {
                    return false;
                }
            }
            Vec3d eyePos;
            return !box.contains(eyePos = player.getEyePos())
                    && !hits(eyePos, box, d, server.yaw(), server.pitch());
        }
    }

    public static void afterSend(ClientPlayerEntity player) {
        if (swapped) {
            restore(player);
        }
    }

    public static void finishTick(ClientPlayerEntity player) {
        if (swapped) {
            restore(player);
        }
    }

    private static Rotation target(ClientPlayerEntity player) {
        if (committed != null) {
            return committed;
        } else if (stepped != null) {
            return stepped;
        } else {
            if (requested != null) {
                stepped = step(player, requested, requestedSpeed);
            } else if (easeLeft > 0 && server != null) {
                stepped = step(player, new Rotation(player.getYaw(), player.getPitch()), 5.0F);
            }
            return stepped;
        }
    }

    private static Rotation step(ClientPlayerEntity player, Rotation rotation, float f) {
        Rotation rotation2 =
                server != null ? server : new Rotation(player.getYaw(), player.getPitch());
        double d = RotationUtils.mouseScale(MinecraftClient.getInstance(), player) * 0.15;
        double e = MathHelper.wrapDegrees(rotation.yaw() - rotation2.yaw());
        double d2;
        double e2 = Math.hypot(e, d2 = rotation.pitch() - rotation2.pitch());
        if (e2 < 1.0E-4) {
            return next(player, rotation);
        } else {
            double d3 = 1.0;
            if (f > 0.0F) {
                double e3 = Math.min(e2, 0.313 * e2 + 1.56 * f);
                if (e3 < e2) {
                    e3 *= 0.85 + 0.15 * ThreadLocalRandom.current().nextDouble();
                }
                d3 = e3 / e2;
            }
            double d4;
            if ((d4 = Math.max(Math.abs(e), Math.abs(d2)) * d3) > 25.0) {
                d3 *= 25.0 / d4;
            }
            long l = Math.max(1L, (long) Math.floor(25.0 / d));
            long l2 = MathHelper.clamp(Math.round(e * d3 / d), -l, l);
            long l3 = MathHelper.clamp(Math.round(d2 * d3 / d), -l, l);
            return new Rotation(
                    rotation2.yaw() + (float) (l2 * d),
                    MathHelper.clamp(rotation2.pitch() + (float) (l3 * d), -90.0F, 90.0F));
        }
    }

    private static void endEase() {
        if (easeLeft != 0 && committed == null) {
            easeLeft = 0;
            stepped = null;
        }
    }

    private static boolean closeToCamera(ClientPlayerEntity player) {
        double d = RotationUtils.mouseScale(MinecraftClient.getInstance(), player) * 0.15;
        double e = Math.abs(MathHelper.wrapDegrees(player.getYaw() - server.yaw()));
        double d2 = Math.max(e, (double) Math.abs(player.getPitch() - server.pitch()));
        return d2 <= Math.max(0.5, 2.0 * d) || d2 > 150.0;
    }

    private static int impulse(boolean bl, boolean bl2) {
        return bl == bl2 ? 0 : (bl ? 1 : -1);
    }

    private static double[] heading(float f, int i, int j) {
        double d = MathHelper.sin(f * 0.017453292F);
        double e = MathHelper.cos(f * 0.017453292F);
        double d2 = i * e - j * d;
        double e2 = j * e + i * d;
        double d3 = Math.sqrt(d2 * d2 + e2 * e2);
        return new double[] {d2 / d3, e2 / d3};
    }

    private static boolean hits(Vec3d vec, Box box, double d, float f, float g) {
        Vec3d vec2 = Vec3d.fromPolar(g, f);
        return box.raycast(vec, vec.add(vec2.multiply(d))).isPresent();
    }

    private static void restore(ClientPlayerEntity player) {
        player.renderYaw = player.renderYaw + (ownYaw - walked.yaw()) * 0.5F;
        player.renderPitch = player.renderPitch + (ownPitch - walked.pitch()) * 0.5F;
        walked = null;
        player.setYaw(ownYaw);
        player.setPitch(ownPitch);
        swapped = false;
    }
}
