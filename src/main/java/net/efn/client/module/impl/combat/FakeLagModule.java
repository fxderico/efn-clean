package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayDeque;
import java.util.Deque;

@Environment(EnvType.CLIENT)
public final class FakeLagModule extends ClientModule {

    private static final String DYNAMIC = "Dynamic";

    private static final String ALWAYS = "Always";

    private static final double AIM_CONE_DEGREES = 30.0;

    private static final double TOWARDS = 0.02;

    private static final int BOX_COLOR = -43691;

    private final ModeSetting mode =
            this.addSetting("mode", new ModeSetting("Mode", "Dynamic", "Dynamic", "Always"));

    private final NumberSetting delay =
            this.addSetting(
                    "delay", new NumberSetting("Delay (ms)", 100.0F, 10.0F, 1000.0F).step(10.0F));

    private final NumberSetting range =
            this.addSetting("range", new NumberSetting("Range", 6.0F, 3.0F, 12.0F).step(0.5F));

    private final BooleanSetting render =
            this.addSetting("render", new BooleanSetting("Show Server Position", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final Deque<FakeLagModule.Held> held = new ArrayDeque<>();

    private boolean releasing;

    private ClientPlayNetworkHandler heldFor;

    private volatile Vec3d serverFeet;

    private volatile boolean lagNow;

    public FakeLagModule() {
        super(
                "fake-lag",
                "Fake Lag",
                "Holds your outgoing packets for a moment so the server sees you a little behind."
                        + " Dynamic: only while you walk at the player you aim at.",
                ModuleCategory.COMBAT);
        this.range.visibleWhen(() -> this.mode.is("Dynamic"));
    }

    @Override
    protected void onEnable() {
        Deque held2 = this.held;
        synchronized (held2) {
            this.held.clear();
        }
        this.serverFeet = null;
        this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
        this.on(Render3DEvent.TYPE, Stage.POST, render3DEvent -> this.render());
    }

    @Override
    protected void onDisable() {
        this.lagNow = false;
        this.releaseAll();
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        if (!this.releasing) {
            Packet packet2 = event.packet;
            if (this.lagNow && !acts(packet2)) {
                Deque held2 = this.held;
                synchronized (held2) {
                    if (this.held.isEmpty()) {
                        this.heldFor = this.mc.getNetworkHandler();
                    }
                    this.held.addLast(new FakeLagModule.Held(packet2, System.currentTimeMillis()));
                }
                event.setCancelled(true);
            } else {
                this.releaseAll();
                this.track(packet2);
            }
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveNettyEvent event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            this.mc.execute(this::releaseAll);
        }
    }

    private void tick() {
        if (this.mc.getNetworkHandler() != null && this.mc.player != null) {
            this.lagNow = this.lagging();
            if (!this.lagNow) {
                this.releaseAll();
            } else {
                long l = System.currentTimeMillis() - Math.round(this.delay.get());
                while (true) {
                    Deque held2 = this.held;
                    FakeLagModule.Held held3;
                    synchronized (held2) {
                        held3 = this.held.peekFirst();
                        if (held3 == null || held3.at() > l) {
                            return;
                        }
                        this.held.pollFirst();
                    }
                    this.send(held3.packet());
                }
            }
        } else {
            this.lagNow = false;
            this.drop();
        }
    }

    private boolean lagging() {
        ClientPlayerEntity player = this.mc.player;
        return player != null && this.mc.getNetworkHandler() != null && player.isAlive()
                ? this.mode.is("Always") || this.walkingAtTarget(player)
                : false;
    }

    private boolean walkingAtTarget(ClientPlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        Vec3d rotationVec = player.getRotationVec(1.0F);
        Vec3d velocity = player.getVelocity();
        double range2 = this.range.get();
        for (PlayerEntity player2 : this.mc.world.getPlayers()) {
            Vec3d vec;
            if (player2 != player
                    && MeleeAttacks.isEnemy(player, player2)
                    && !(player2.distanceTo(player) > range2)
                    && !(Math.toDegrees(
                                    Math.acos(
                                            Math.max(
                                                    -1.0,
                                                    Math.min(
                                                            1.0,
                                                            rotationVec.dotProduct(
                                                                    vec =
                                                                            player2.getBoundingBox()
                                                                                    .getCenter()
                                                                                    .subtract(
                                                                                            eyePos)
                                                                                    .normalize())))))
                            > 30.0)
                    && velocity.x * vec.x + velocity.z * vec.z > 0.02) {
                return true;
            }
        }
        return false;
    }

    private static boolean acts(Packet<?> packet2) {
        return packet2 instanceof PlayerInteractEntityC2SPacket
                || packet2 instanceof PlayerInteractBlockC2SPacket
                || packet2 instanceof PlayerInteractItemC2SPacket
                || packet2 instanceof PlayerActionC2SPacket
                || packet2 instanceof HandSwingC2SPacket
                || packet2 instanceof ClickSlotC2SPacket
                || packet2 instanceof ButtonClickC2SPacket
                || packet2 instanceof CloseHandledScreenC2SPacket
                || packet2 instanceof ChatMessageC2SPacket
                || packet2 instanceof CommandExecutionC2SPacket
                || packet2 instanceof ChatCommandSignedC2SPacket
                || packet2 instanceof RequestCommandCompletionsC2SPacket
                || packet2 instanceof UpdateSignC2SPacket
                || packet2 instanceof BookUpdateC2SPacket
                || packet2 instanceof CraftRequestC2SPacket
                || packet2 instanceof SelectMerchantTradeC2SPacket
                || packet2 instanceof CreativeInventoryActionC2SPacket
                || packet2 instanceof PickItemFromBlockC2SPacket
                || packet2 instanceof PickItemFromEntityC2SPacket
                || packet2 instanceof TeleportConfirmC2SPacket;
    }

    private void releaseAll() {
        while (true) {
            Deque held2 = this.held;
            FakeLagModule.Held held3;
            synchronized (held2) {
                held3 = this.held.pollFirst();
            }
            if (held3 == null) {
                return;
            }
            this.send(held3.packet());
        }
    }

    private void drop() {
        Deque held2 = this.held;
        synchronized (held2) {
            this.held.clear();
        }
        this.serverFeet = null;
    }

    private void send(Packet<?> packet2) {
        ClientPlayNetworkHandler networkHandler = this.mc.getNetworkHandler();
        if (networkHandler != null && (networkHandler == this.heldFor || this.heldFor == null)) {
            this.releasing = true;
            try {
                networkHandler.sendPacket(packet2);
            } finally {
                this.releasing = false;
            }
            this.track(packet2);
        } else {
            this.drop();
        }
    }

    private void track(Packet<?> packet2) {
        PlayerMoveC2SPacket playerMovePacket;
        if (packet2 instanceof PlayerMoveC2SPacket
                && (playerMovePacket = (PlayerMoveC2SPacket) packet2).changesPosition()
                && this.mc.player != null) {
            this.serverFeet =
                    new Vec3d(
                            playerMovePacket.getX(this.mc.player.getX()),
                            playerMovePacket.getY(this.mc.player.getY()),
                            playerMovePacket.getZ(this.mc.player.getZ()));
        }
    }

    private void render() {
        ClientPlayerEntity player = this.mc.player;
        Vec3d serverFeet2 = this.serverFeet;
        Deque held2 = this.held;
        boolean bl;
        synchronized (held2) {
            bl = !this.held.isEmpty();
        }
        if (this.render.get() && player != null && serverFeet2 != null && bl) {
            double d = player.getWidth() / 2.0;
            GizmoDrawing.box(
                    new Box(
                            serverFeet2.x - d,
                            serverFeet2.y,
                            serverFeet2.z - d,
                            serverFeet2.x + d,
                            serverFeet2.y + player.getHeight(),
                            serverFeet2.z + d),
                    DrawStyle.stroked(-43691, 2.0F));
        }
    }

    @Environment(EnvType.CLIENT)
    private record Held(Packet<?> packet, long at) {}
}
