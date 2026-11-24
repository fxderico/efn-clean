package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class FreelookModule extends ClientModule {

    private static final String PLAYER = "Player";

    private static final String CAMERA = "Camera";

    private static final float MIN_SENSITIVITY = 0.1F;

    private final ModeSetting mode =
            this.addSetting("mode", new ModeSetting("Mode", "Player", "Player", "Camera"));

    private final BooleanSetting togglePerspective =
            this.addSetting("toggle-perspective", new BooleanSetting("Toggle Perspective", true));

    private final NumberSetting sensitivity =
            this.addSetting(
                    "camera-sensitivity",
                    new NumberSetting("Camera Sensitivity", 8.0F, 0.0F, 10.0F).step(0.1F));

    private final BooleanSetting arrows =
            this.addSetting(
                    "arrows-control-opposite", new BooleanSetting("Arrows Control Opposite", true));

    private final NumberSetting arrowSpeed =
            this.addSetting(
                    "arrow-speed", new NumberSetting("Arrow Speed", 4.0F, 0.0F, 10.0F).step(0.5F));

    private float cameraYaw;

    private float cameraPitch;

    private Perspective previousView;

    public FreelookModule() {
        super(
                "freelook",
                "Freelook",
                "Turns the camera and the player separately.",
                ModuleCategory.RENDER);
        this.sensitivity.visibleWhen(() -> this.mode.is("Camera"));
        this.arrowSpeed.visibleWhen(this.arrows::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            this.requestDisable();
        } else {
            this.cameraYaw = client.player.getYaw();
            this.cameraPitch = client.player.getPitch();
            this.previousView = client.options.getPerspective();
            if (this.previousView != Perspective.THIRD_PERSON_BACK
                    && this.togglePerspective.get()) {
                client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
            this.on(TickEvent.TYPE, Stage.POST, this::onTick);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.previousView != null
                && client.options.getPerspective() != this.previousView
                && this.togglePerspective.get()) {
            client.options.setPerspective(this.previousView);
        }
        this.previousView = null;
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null) {
            if (this.arrows.get() && client.currentScreen == null) {
                float arrowSpeed2 = this.arrowSpeed.get();
                float f =
                        (held(client, 262) ? arrowSpeed2 : 0.0F)
                                - (held(client, 263) ? arrowSpeed2 : 0.0F);
                float g =
                        (held(client, 264) ? arrowSpeed2 : 0.0F)
                                - (held(client, 265) ? arrowSpeed2 : 0.0F);
                if (this.mode.is("Player")) {
                    this.cameraYaw += f;
                    this.cameraPitch += g;
                } else if (f != 0.0F || g != 0.0F) {
                    player.setYaw(player.getYaw() + f);
                    player.setPitch(player.getPitch() + g);
                }
            }
            this.cameraPitch = MathHelper.clamp(this.cameraPitch, -90.0F, 90.0F);
        }
    }

    private static boolean held(MinecraftClient client, int i) {
        return EfnKeyPolling.isKeyDown(client.getWindow(), i);
    }

    public boolean cameraMode() {
        return this.isEnabled() && this.mode.is("Camera");
    }

    public static boolean throughBlocks() {
        FreelookModule module = ModuleRegistry.get(FreelookModule.class);
        return module != null && module.isEnabled() && FreecamModule.active == null;
    }

    public void turnCamera(double d, double e) {
        float f = Math.max(this.sensitivity.get(), 0.1F);
        this.cameraYaw += (float) (d / f);
        this.cameraPitch += (float) (e / f);
        if (Math.abs(this.cameraPitch) > 90.0F) {
            this.cameraPitch = this.cameraPitch > 0.0F ? 90.0F : -90.0F;
        }
    }

    public float cameraYaw() {
        return this.cameraYaw;
    }

    public float cameraPitch() {
        return this.cameraPitch;
    }
}
