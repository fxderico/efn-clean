package net.efn.client.module.impl.combat.bot;

import net.efn.client.module.impl.combat.crystal.Crystals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;

@Environment(EnvType.CLIENT)
final class BotPause {

    private static final int TOUCH_TICKS = 30;

    private static final int RESPAWN_TICKS = 30;

    private static final int WORLD_TICKS = 40;

    private static final int SETBACK_TICKS = 10;

    private static final int SETBACK_LIMIT = 6;

    private static final long SETBACK_SPAN_MS = 10000L;

    private static final double SETBACK_MAX_JUMP = 6.0;

    private static final double CURSOR_SLACK = 1.5;

    private final ArrayDeque<Long> setbacks = new ArrayDeque<>();

    private ClientPlayerEntity body;

    private ClientWorld world;

    private double cursorX = 0.0 / 0.0;

    private double cursorY = 0.0 / 0.0;

    private int tick;

    private int touchedUntil;

    private int heldUntil;

    private Vec3d lastPosition;

    private long timeAt;

    private long gameTimeAt = -1L;

    private float tps = 20.0F;

    BotPause() {}

    void reset() {
        this.setbacks.clear();
        this.body = null;
        this.world = null;
        this.cursorY = 0.0 / 0.0;
        this.cursorX = 0.0 / 0.0;
        this.heldUntil = 0;
        this.touchedUntil = 0;
        this.tick = 0;
        this.lastPosition = null;
        this.gameTimeAt = -1L;
        this.tps = 20.0F;
    }

    BotPause.State tick(MinecraftClient client, ClientPlayerEntity body2) {
        this.tick++;
        if (this.body != null && body2 != this.body) {
            this.heldUntil =
                    Math.max(this.heldUntil, this.tick + (client.world != this.world ? 40 : 30));
        }
        this.body = body2;
        this.world = client.world;
        if (this.touched(client)) {
            this.touchedUntil = this.tick + 30;
        }
        long l = System.currentTimeMillis();
        while (!this.setbacks.isEmpty() && l - this.setbacks.peekFirst() > 10000L) {
            this.setbacks.pollFirst();
        }
        this.lastPosition = body2.getEntityPos();
        if (this.setbacks.size() >= 6) {
            return BotPause.State.STOP;
        } else if (this.tick < this.heldUntil) {
            return BotPause.State.HOLD;
        } else {
            return this.tick < this.touchedUntil ? BotPause.State.YIELD : BotPause.State.RUN;
        }
    }

    void moved(Vec3d vec, int i) {
        if (this.lastPosition != null && i >= 60) {
            if (!(vec.distanceTo(this.lastPosition) > 6.0)) {
                this.setbacks.add(System.currentTimeMillis());
                this.heldUntil = Math.max(this.heldUntil, this.tick + 10);
            }
        }
    }

    void time(long gameTimeAt2) {
        long l = System.currentTimeMillis();
        if (this.gameTimeAt >= 0L && l > this.timeAt && gameTimeAt2 > this.gameTimeAt) {
            float f = (float) (gameTimeAt2 - this.gameTimeAt) * 1000.0F / (float) (l - this.timeAt);
            this.tps = Math.min(20.0F, this.tps + (f - this.tps) * 0.5F);
        }
        this.gameTimeAt = gameTimeAt2;
        this.timeAt = l;
    }

    boolean slowServer() {
        return this.tps < 15.0F;
    }

    int setbacksLately() {
        return this.setbacks.size();
    }

    private boolean touched(MinecraftClient client) {
        if (client.currentScreen == null && client.isWindowFocused()) {
            double d = client.mouse.getX();
            double e = client.mouse.getY();
            boolean bl =
                    !Double.isNaN(this.cursorX)
                            && (Math.abs(d - this.cursorX) > 1.5
                                    || Math.abs(e - this.cursorY) > 1.5);
            this.cursorX = d;
            this.cursorY = e;
            if (bl) {
                return true;
            } else {
                for (KeyBinding keyBinding :
                        new KeyBinding[] {
                            client.options.forwardKey,
                            client.options.backKey,
                            client.options.leftKey,
                            client.options.rightKey,
                            client.options.jumpKey,
                            client.options.sneakKey,
                            client.options.attackKey,
                            client.options.useKey
                        }) {
                    if (Crystals.isPhysicallyDown(client, keyBinding)) {
                        return true;
                    }
                }
                for (KeyBinding hotbarKey : client.options.hotbarKeys) {
                    if (Crystals.isPhysicallyDown(client, hotbarKey)) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            this.cursorY = 0.0 / 0.0;
            this.cursorX = 0.0 / 0.0;
            return false;
        }
    }

    @Environment(EnvType.CLIENT)
    static enum State {
        RUN,
        YIELD,
        HOLD,
        STOP;

        private State() {}
    }
}
