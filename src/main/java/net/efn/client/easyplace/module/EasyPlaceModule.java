package net.efn.client.easyplace.module;

import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.builder.SchematicBuilderModule;
import net.efn.client.easyplace.compat.BaritoneCompat;
import net.efn.client.easyplace.inventory.InventoryPickSourceMode;
import net.efn.client.easyplace.placement.PlacementConfig;
import net.efn.client.easyplace.placement.PlacementManager;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.FoglessLines;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class EasyPlaceModule extends ClientModule {

    private static final int FOV_COLOR = -1275016961;

    private static final double FOV_LENGTH = 6.0;

    private final BooleanSetting allBlocks =
            this.addSetting("all-blocks", new BooleanSetting("All Blocks", true));

    private final BooleanSetting rotationSpoof =
            this.addSetting("rotation-spoof", new BooleanSetting("Rotation Spoof", true));

    private final NumberSetting maxYawChange =
            this.addSetting(
                    "max-yaw-change",
                    new NumberSetting("Max Yaw Change", 120.0F, 30.0F, 180.0F).step(1.0F));

    private final ModeSetting inventorySource =
            this.addSetting(
                    "inventory-source",
                    new ModeSetting(
                            "Inventory Source",
                            InventoryPickSourceMode.HOTBAR_ONLY.title(),
                            InventoryPickSourceMode.titles()));

    private final BooleanSetting schematicOnly =
            this.addSetting("schematic-only", new BooleanSetting("Schematic Only", true));

    private final NumberSetting tickDelay =
            this.addSetting(
                    "tick-delay", new NumberSetting("Tick Delay", 0.0F, 0.0F, 10.0F).step(1.0F));

    private final BooleanSetting fovFilter =
            this.addSetting("fov-filter", new BooleanSetting("FOV Filter", false));

    private final BooleanSetting drawFov =
            this.addSetting("draw-fov", new BooleanSetting("Draw FOV", false));

    private final NumberSetting fovDegrees =
            this.addSetting(
                    "fov-degrees",
                    new NumberSetting("FOV Degrees", 60.0F, 10.0F, 180.0F).step(1.0F));

    private final BooleanSetting schematicAura =
            this.addSetting("schematic-aura", new BooleanSetting("Schematic Aura", true));

    private final NumberSetting auraRange =
            this.addSetting(
                    "aura-range", new NumberSetting("Aura Range", 4.0F, 1.0F, 8.0F).step(0.1F));

    private final BooleanSetting hopperForceFacing =
            this.addSetting("hopper-force-facing", new BooleanSetting("Hopper Force Facing", true));

    private final BooleanSetting baritoneWalk =
            this.addSetting("baritone-walk", new BooleanSetting("Baritone Walk", false));

    private final BooleanSetting chatFeedback =
            this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));

    private final BooleanSetting debug =
            this.addSetting("debug", new BooleanSetting("Debug Log", false));

    private final PlacementManager placementManager =
            new PlacementManager(MinecraftClient.getInstance(), this::feedback);

    private final Map<String, Long> lastFeedbackTick = new HashMap<>();

    private long moduleTick;

    public EasyPlaceModule() {
        super(
                "easy-place",
                "Easy Place",
                "Places missing blocks from active Litematica placements and waits for server"
                        + " confirmation.",
                ModuleCategory.DONUT);
        this.drawFov.visibleWhen(this.fovFilter::get);
        this.fovDegrees.visibleWhen(this.fovFilter::get);
    }

    @Override
    protected void onEnable() {
        SchematicBuilderModule module = ModuleRegistry.get(SchematicBuilderModule.class);
        if (module != null && module.isEnabled()) {
            module.setEnabled(false);
        }
        this.placementManager.start();
        if (this.baritoneWalk.get()) {
            ModuleChat.send(
                    this,
                    BaritoneCompat.isAvailable()
                            ? "Baritone detected, walking is on"
                            : "Baritone not found, walking is off");
        }
        this.on(
                TickEvent.TYPE,
                Stage.PRE,
                tickEvent -> this.placementManager.tickPre(this.config()));
        this.on(
                TickEvent.TYPE,
                Stage.POST,
                tickEvent -> {
                    this.moduleTick++;
                    this.placementManager.tick(this.config());
                });
        this.on(Render3DEvent.TYPE, Stage.POST, this::renderFov);
    }

    @Override
    protected void onDisable() {
        this.placementManager.reset();
        this.lastFeedbackTick.clear();
        BaritoneCompat.cancel();
    }

    private void renderFov(Render3DEvent event) {
        if (this.fovFilter.get() && this.drawFov.get()) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                Vec3d cameraPosVec = player.getCameraPosVec(event.partialTick);
                double d = Math.toRadians(player.getYaw(event.partialTick));
                double e = Math.toRadians(player.getPitch(event.partialTick));
                double d2 = Math.tan(Math.toRadians(this.fovDegrees.get() * 0.5));
                Vec3d vec =
                        new Vec3d(
                                -Math.sin(d) * Math.cos(e),
                                -Math.sin(e),
                                Math.cos(d) * Math.cos(e));
                Vec3d vec2 = new Vec3d(Math.cos(d), 0.0, Math.sin(d)).multiply(d2);
                Vec3d vec3 =
                        new Vec3d(
                                        -Math.sin(d) * Math.sin(e),
                                        Math.cos(e),
                                        Math.cos(d) * Math.sin(e))
                                .multiply(d2);
                Vec3d[] vecs =
                        new Vec3d[] {
                            vec.add(vec2).add(vec3),
                            vec.subtract(vec2).add(vec3),
                            vec.subtract(vec2).subtract(vec3),
                            vec.add(vec2).subtract(vec3)
                        };
                FoglessLines foglessLines = FoglessLines.forFrame(event.partialTick);
                for (int i = 0; i < vecs.length; i++) {
                    Vec3d vec4 = cameraPosVec.add(vecs[i].multiply(6.0));
                    Vec3d vec5 = cameraPosVec.add(vecs[(i + 1) % vecs.length].multiply(6.0));
                    foglessLines.line(cameraPosVec, vec4, -1275016961, 1.5F);
                    foglessLines.line(vec4, vec5, -1275016961, 1.5F);
                }
            }
        }
    }

    private PlacementConfig config() {
        return new PlacementConfig(
                this.allBlocks.get(),
                this.rotationSpoof.get(),
                this.maxYawChange.get(),
                InventoryPickSourceMode.of(this.inventorySource.get()),
                this.schematicOnly.get(),
                this.tickDelay.getInt(),
                this.schematicAura.get(),
                this.auraRange.get(),
                this.hopperForceFacing.get(),
                this.baritoneWalk.get(),
                this.debug.get(),
                this.fovFilter.get(),
                this.fovDegrees.get());
    }

    private void feedback(String string, String string2, int i) {
        if (this.chatFeedback.get()) {
            long l = this.lastFeedbackTick.getOrDefault(string, -4611686018427387904L);
            if (this.moduleTick - l >= i) {
                this.lastFeedbackTick.put(string, this.moduleTick);
                ModuleChat.send(this, string2);
            }
        }
    }
}
