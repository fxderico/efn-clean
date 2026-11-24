package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.LootLatch;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;

import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class AutoClickerModule extends ClientModule {

    private static final String CLICK = "Click";

    private static final String HOLD = "Hold";

    private static final String OFF = "Off";

    private static final double TICK_MS = 50.0;

    private static final double PACE_MIN_MS = 500.0;

    private static final double PACE_MAX_MS = 1500.0;

    private static final double PACE_EASE = 0.3;

    private static final double HOLD_MEAN_MS = 55.0;

    private static final double HOLD_SPREAD_MS = 18.0;

    private static final double HOLD_MIN_MS = 20.0;

    private static final double HOLD_MAX_MS = 110.0;

    private static final double HOLD_MAX_GAP = 0.7;

    private static final double GAP_MIN_FACTOR = 0.6;

    private static final double GAP_MAX_FACTOR = 2.2;

    private final ModeSetting left =
            this.addSetting("left", new ModeSetting("Left", "Click", "Click", "Hold", "Off"));

    private final NumberSetting leftMin =
            this.addSetting(
                    "left-min-cps",
                    new NumberSetting("Left Min CPS", 9.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting leftMax =
            this.addSetting(
                    "left-max-cps",
                    new NumberSetting("Left Max CPS", 13.0F, 1.0F, 20.0F).step(1.0F));

    private final ModeSetting right =
            this.addSetting("right", new ModeSetting("Right", "Off", "Click", "Hold", "Off"));

    private final NumberSetting rightMin =
            this.addSetting(
                    "right-min-cps",
                    new NumberSetting("Right Min CPS", 8.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting rightMax =
            this.addSetting(
                    "right-max-cps",
                    new NumberSetting("Right Max CPS", 12.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting randomization =
            this.addSetting(
                    "randomization",
                    new NumberSetting("Randomization", 15.0F, 0.0F, 40.0F).step(1.0F));

    private final BooleanSetting onlyWhileHeld =
            this.addSetting("only-while-held", new BooleanSetting("Only While Held", false));

    private final BooleanSetting breakBlocks =
            this.addSetting("break-blocks", new BooleanSetting("Break Blocks", true));

    private final AutoClickerModule.Button leftButton =
            new AutoClickerModule.Button(
                    true,
                    this.left,
                    this.leftMin,
                    this.leftMax,
                    minecraftClient -> minecraftClient.options.attackKey);

    private final AutoClickerModule.Button rightButton =
            new AutoClickerModule.Button(
                    false,
                    this.right,
                    this.rightMin,
                    this.rightMax,
                    minecraftClient -> minecraftClient.options.useKey);

    public AutoClickerModule() {
        super(
                "auto-clicker",
                "Auto Clicker",
                "Clicks the left and/or right mouse button at a randomised, human CPS.",
                ModuleCategory.MISC);
        this.leftMin.visibleWhen(() -> this.left.is("Click"));
        this.leftMax.visibleWhen(() -> this.left.is("Click"));
        this.rightMin.visibleWhen(() -> this.right.is("Click"));
        this.rightMax.visibleWhen(() -> this.right.is("Click"));
        this.randomization.visibleWhen(this::anyClicking);
        this.onlyWhileHeld.visibleWhen(this::anyClicking);
        this.breakBlocks.visibleWhen(() -> this.left.is("Click"));
    }

    @Override
    public int settingsVersion() {
        return 1;
    }

    @Override
    public void migrateSettings(int i) {
        if (i < 1) {
            this.onlyWhileHeld.set(false);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        this.leftButton.handBack(client);
        this.rightButton.handBack(client);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean bl =
                MeleeAttacks.canAct(client)
                        && client.getOverlay() == null
                        && !CombatMacroLock.heldByOther(this)
                        && !MeleeAttacks.actedThisTick(client.player);
        this.leftButton.tick(client, bl);
        this.rightButton.tick(client, bl && !LootLatch.active());
    }

    private boolean anyClicking() {
        return this.left.is("Click") || this.right.is("Click");
    }

    private static boolean aimsAtBlock(MinecraftClient client) {
        HitResult crosshairTarget = client.crosshairTarget;
        BlockHitResult blockHitResult;
        return crosshairTarget instanceof BlockHitResult
                && (blockHitResult = (BlockHitResult) crosshairTarget).getType() == Type.BLOCK
                && client.world != null
                && !client.world.getBlockState(blockHitResult.getBlockPos()).isAir();
    }

    private static boolean physicallyDown(MinecraftClient client, KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        } else {
            Key boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding);
            int code = boundKey.getCode();
            if (code < 0) {
                return false;
            } else {
                return switch (boundKey.getCategory()) {
                    case MOUSE ->
                            GLFW.glfwGetMouseButton(client.getWindow().getHandle(), code) == 1;
                    case KEYSYM -> EfnKeyPolling.isKeyDown(client.getWindow(), code);
                    default -> keyBinding.isPressed();
                };
            }
        }
    }

    private static double clamp(double d, double e, double d2) {
        return Math.max(e, Math.min(d2, d));
    }

    @Environment(EnvType.CLIENT)
    private final class Button {

        private final boolean isLeft;

        private final ModeSetting mode;

        private final NumberSetting minCps;

        private final NumberSetting maxCps;

        private final Function<MinecraftClient, KeyBinding> mapping;

        private boolean driving;

        private boolean running;

        private double clock;

        private double nextPress;

        private double releaseAt;

        private double rate;

        private double pace;

        private double repaceAt;

        Button(
                boolean isLeft2,
                ModeSetting mode2,
                NumberSetting minCps2,
                NumberSetting maxCps2,
                Function<MinecraftClient, KeyBinding> mapping2) {
            this.isLeft = isLeft2;
            this.mode = mode2;
            this.minCps = minCps2;
            this.maxCps = maxCps2;
            this.mapping = mapping2;
        }

        void tick(MinecraftClient client, boolean bl) {
            if (bl && !this.mode.is("Off")) {
                KeyBinding keyBinding = this.mapping.apply(client);
                if (this.mode.is("Hold")) {
                    this.running = false;
                    this.driving = true;
                    keyBinding.setPressed(true);
                } else if (!keyBinding.isUnbound()
                        && (!AutoClickerModule.this.onlyWhileHeld.get()
                                || AutoClickerModule.physicallyDown(client, keyBinding))
                        && (!this.isLeft
                                || !AutoClickerModule.this.breakBlocks.get()
                                || !AutoClickerModule.aimsAtBlock(client))
                        && (this.isLeft || !client.player.isUsingItem())) {
                    if (!this.running) {
                        this.start();
                    }
                    this.clock += 50.0;
                    if (this.isLeft) {
                        client.attackCooldown = 0;
                    }
                    Key boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding);
                    while (this.nextPress <= this.clock) {
                        KeyBinding.onKeyPressed(boundKey);
                        double nextGap = this.nextGap();
                        this.releaseAt = this.nextPress + this.holdFor(nextGap);
                        this.nextPress += nextGap;
                    }
                    this.driving = true;
                    keyBinding.setPressed(this.clock < this.releaseAt);
                } else {
                    this.handBack(client);
                }
            } else {
                this.handBack(client);
            }
        }

        private void start() {
            double d = Math.min(this.minCps.get(), this.maxCps.get());
            double e = Math.max(this.minCps.get(), this.maxCps.get());
            ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
            this.running = true;
            this.clock = 0.0;
            this.releaseAt = 0.0;
            this.rate = this.pace = d + threadLocalRandom.nextDouble() * (e - d);
            this.repaceAt = 500.0 + threadLocalRandom.nextDouble() * 1000.0;
            this.nextPress = this.nextGap();
        }

        private double nextGap() {
            double d = Math.min(this.minCps.get(), this.maxCps.get());
            double e = Math.max(this.minCps.get(), this.maxCps.get());
            ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
            if (this.clock >= this.repaceAt) {
                this.pace = d + threadLocalRandom.nextDouble() * (e - d);
                this.repaceAt = this.clock + 500.0 + threadLocalRandom.nextDouble() * 1000.0;
            }
            this.rate = AutoClickerModule.clamp(this.rate + (this.pace - this.rate) * 0.3, d, e);
            double d2 = 1000.0 / this.rate;
            double e2 = AutoClickerModule.this.randomization.get() / 100.0;
            if (e2 <= 0.0) {
                return d2;
            } else {
                double d3 = 1.0 + threadLocalRandom.nextGaussian() * e2;
                if (threadLocalRandom.nextDouble() < e2 * 0.25) {
                    d3 += 0.4 + threadLocalRandom.nextDouble() * 0.6;
                }
                return d2 * AutoClickerModule.clamp(d3, 0.6, 2.2);
            }
        }

        private double holdFor(double d) {
            if (AutoClickerModule.this.randomization.get() <= 0.0F) {
                return d * 0.5;
            } else {
                double e = 55.0 + ThreadLocalRandom.current().nextGaussian() * 18.0;
                return AutoClickerModule.clamp(e, 20.0, Math.min(110.0, d * 0.7));
            }
        }

        void handBack(MinecraftClient client) {
            this.running = false;
            if (this.driving) {
                this.driving = false;
                KeyBinding keyBinding = this.mapping.apply(client);
                keyBinding.setPressed(
                        client.currentScreen == null
                                && AutoClickerModule.physicallyDown(client, keyBinding));
            }
        }
    }
}
