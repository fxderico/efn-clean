package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public final class HandsModule extends ClientModule {

    public static volatile HandsModule live;

    private static final String MAIN = "Main Hand";

    private static final String OFF = "Offhand";

    private static final float PIVOT_X = 0.56F;

    private static final float PIVOT_Y = -0.52F;

    private static final float PIVOT_Z = -0.72F;

    private static final float[] REST = new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};

    private static final int X = 0;

    private static final int Y = 1;

    private static final int Z = 2;

    private static final int PITCH = 3;

    private static final int YAW = 4;

    private static final int ROLL = 5;

    private static final int SCALE = 6;

    private static final float SETTLED = 0.001F;

    private static final float MAX_STEP = 0.1F;

    private static final float SLOWEST_RATE = 3.0F;

    private static final float FASTEST_RATE = 30.0F;

    private final BooleanSetting link =
            this.addSetting("link-hands", new BooleanSetting("Link Hands", false));

    private final ModeSetting editing =
            this.addSetting(
                    "editing", new ModeSetting("Editing", "Main Hand", "Main Hand", "Offhand"));

    private final NumberSetting[] main = this.axes("main", true);

    private final NumberSetting[] off = this.axes("off", false);

    private final NumberSetting glide =
            this.addSetting("glide", new NumberSetting("Glide", 0.4F, 0.0F, 1.0F).step(0.05F));

    private final HandsModule.HandState mainState = new HandsModule.HandState();

    private final HandsModule.HandState offState = new HandsModule.HandState();

    public HandsModule() {
        super(
                "hand-pose",
                "Hand Pose",
                "Moves, rotates and resizes your first-person hands.",
                ModuleCategory.VISUALS);
        this.editing.visibleWhen(() -> !this.link.get());
        this.addSetting("reset-hand", new ActionSetting("Reset Hand", this::resetEdited));
    }

    private NumberSetting[] axes(String string, boolean bl) {
        NumberSetting[] numberSettings;
        for (NumberSetting setting :
                numberSettings =
                        new NumberSetting[] {
                            this.addSetting(
                                    string + "-x",
                                    new NumberSetting("Offset X", REST[0], -1.0F, 1.0F)
                                            .step(0.01F)),
                            this.addSetting(
                                    string + "-y",
                                    new NumberSetting("Offset Y", REST[1], -1.0F, 1.0F)
                                            .step(0.01F)),
                            this.addSetting(
                                    string + "-z",
                                    new NumberSetting("Offset Z", REST[2], -1.0F, 1.0F)
                                            .step(0.01F)),
                            this.addSetting(
                                    string + "-pitch",
                                    new NumberSetting("Pitch", REST[3], -180.0F, 180.0F)
                                            .step(1.0F)),
                            this.addSetting(
                                    string + "-yaw",
                                    new NumberSetting("Yaw", REST[4], -180.0F, 180.0F).step(1.0F)),
                            this.addSetting(
                                    string + "-roll",
                                    new NumberSetting("Roll", REST[5], -180.0F, 180.0F).step(1.0F)),
                            this.addSetting(
                                    string + "-scale",
                                    new NumberSetting("Scale", REST[6], 0.3F, 2.0F).step(0.05F))
                        }) {
            setting.visibleWhen(
                    bl
                            ? () -> this.link.get() || this.editing.is("Main Hand")
                            : () -> !this.link.get() && this.editing.is("Offhand"));
        }
        return numberSettings;
    }

    @Override
    protected void onEnable() {
        this.mainState.stamp = 0L;
        this.offState.stamp = 0L;
        live = this;
    }

    private void resetEdited() {
        NumberSetting[] numberSettings =
                !this.link.get() && !this.editing.is("Main Hand") ? this.off : this.main;
        for (int i = 0; i < numberSettings.length; i++) {
            numberSettings[i].set(REST[i]);
        }
    }

    public static void shape(MatrixStack matrices, boolean bl, int i) {
        HandsModule module = live;
        if (module != null) {
            module.shapeHand(matrices, bl, i);
        }
    }

    private void shapeHand(MatrixStack matrices, boolean bl, int i) {
        boolean enabled = this.isEnabled();
        HandsModule.HandState handState = bl ? this.mainState : this.offState;
        handState.step(!bl && !this.link.get() ? this.off : this.main, enabled, this.follow());
        if (!enabled && this.mainState.rested && this.offState.rested) {
            live = null;
        } else if (!handState.rested) {
            float[] value2 = handState.value;
            matrices.translate(i * (0.56F + value2[0]), -0.52F + value2[1], -0.72F + value2[2]);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * value2[4]));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(value2[3]));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * value2[5]));
            matrices.scale(value2[6], value2[6], value2[6]);
            matrices.translate(-i * 0.56F, 0.52F, 0.72F);
        }
    }

    private float follow() {
        float glide2 = this.glide.get();
        return glide2 <= 0.0F ? -1.0F : 30.0F + -27.0F * glide2;
    }

    @Environment(EnvType.CLIENT)
    private static final class HandState {

        final float[] value = (float[]) HandsModule.REST.clone();

        long stamp;

        boolean rested = true;

        private HandState() {}

        void step(NumberSetting[] numberSettings, boolean bl, float f) {
            long l = System.nanoTime();
            float g = this.stamp == 0L ? 0.0F : Math.min(0.1F, (float) (l - this.stamp) / 1.0E9F);
            this.stamp = l;
            float h = f < 0.0F ? 1.0F : 1.0F - (float) Math.exp(-f * g);
            boolean bl2 = true;
            for (int i = 0; i < this.value.length; i++) {
                float f2 = bl ? numberSettings[i].get() : HandsModule.REST[i];
                float g2;
                if (Math.abs(f2 - (g2 = this.value[i] + (f2 - this.value[i]) * h)) < 0.001F) {
                    g2 = f2;
                }
                this.value[i] = g2;
                if (g2 != HandsModule.REST[i]) {
                    bl2 = false;
                }
            }
            this.rested = bl2;
        }
    }
}
