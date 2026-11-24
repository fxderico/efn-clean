package net.efn.client.module.impl.render;

import imgui.ImDrawList;

import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import net.efn.client.config.FriendStore;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.gui.GuiPalette;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class TracersModule extends ClientModule implements HudOverlayProvider {

    private static final double DISTANCE_COLOR_RANGE = 60.0;

    private static final float ARROW_TIP = 0.73205F;

    private static final int WHITE = 16777215;

    private final BooleanSetting players =
            this.addSetting("players", new BooleanSetting("Players", true));

    private final BooleanSetting animals =
            this.addSetting("animals", new BooleanSetting("Animals", false));

    private final BooleanSetting waterAnimals =
            this.addSetting("water-animals", new BooleanSetting("Water Animals", false));

    private final BooleanSetting monsters =
            this.addSetting("monsters", new BooleanSetting("Monsters", false));

    private final BooleanSetting ambient =
            this.addSetting("ambient", new BooleanSetting("Ambient", false));

    private final BooleanSetting misc = this.addSetting("misc", new BooleanSetting("Misc", false));

    private final BooleanSetting ignoreSelf =
            this.addSetting("ignore-self", new BooleanSetting("Ignore Self", false));

    private final BooleanSetting ignoreFriends =
            this.addSetting("ignore-friends", new BooleanSetting("Ignore Friends", false));

    private final BooleanSetting showInvisible =
            this.addSetting("show-invisible", new BooleanSetting("Show Invisible", true));

    private final ModeSetting style =
            this.addSetting("style", new ModeSetting("Style", "Lines", "Lines", "Offscreen"));

    private final ModeSetting target =
            this.addSetting("target", new ModeSetting("Target", "Body", "Head", "Body", "Feet"));

    private final BooleanSetting stem = this.addSetting("stem", new BooleanSetting("Stem", true));

    private final NumberSetting maxDistance =
            this.addSetting(
                    "max-distance",
                    new NumberSetting("Max Distance", 256.0F, 0.0F, 256.0F).step(1.0F));

    private final NumberSetting distanceOffscreen =
            this.addSetting(
                    "distance-offscreen",
                    new NumberSetting("Distance Offscreen", 200.0F, 0.0F, 500.0F).step(1.0F));

    private final NumberSetting sizeOffscreen =
            this.addSetting(
                    "size-offscreen",
                    new NumberSetting("Size Offscreen", 10.0F, 2.0F, 50.0F).step(1.0F));

    private final BooleanSetting blinkOffscreen =
            this.addSetting("blink-offscreen", new BooleanSetting("Blink Offscreen", true));

    private final NumberSetting blinkOffscreenSpeed =
            this.addSetting(
                    "blink-offscreen-speed",
                    new NumberSetting("Blink Offscreen Speed", 4.0F, 1.0F, 15.0F).step(1.0F));

    private final BooleanSetting distanceColors =
            this.addSetting("distance-colors", new BooleanSetting("Distance Colors", false));

    private final BooleanSetting friendOverride =
            this.addSetting("show-friend-colors", new BooleanSetting("Show Friend Colors", true));

    private final BooleanSetting teamColors =
            this.addSetting("team-colors", new BooleanSetting("Team Colors", true));

    private final ColorSetting friendColor =
            this.addSetting("friend-color", new ColorSetting("Friend Color", -16711756));

    private final ColorSetting playersColor =
            this.addSetting("players-colors", new ColorSetting("Players Color", 2144193997));

    private final ColorSetting animalsColor =
            this.addSetting("animals-color", new ColorSetting("Animals Color", 2140274577));

    private final ColorSetting waterAnimalsColor =
            this.addSetting(
                    "water-animals-color", new ColorSetting("Water Animals Color", 2140246527));

    private final ColorSetting monstersColor =
            this.addSetting("monsters-color", new ColorSetting("Monsters Color", 2147455377));

    private final ColorSetting ambientColor =
            this.addSetting("ambient-color", new ColorSetting("Ambient Color", 2135640907));

    private final ColorSetting miscColor =
            this.addSetting("misc-color", new ColorSetting("Misc Color", 2140246417));

    public TracersModule() {
        super(
                "tracers",
                "Tracers",
                "Displays tracer lines to specified entities.",
                ModuleCategory.RENDER);
        this.target.visibleWhen(() -> this.style.is("Lines"));
        this.stem.visibleWhen(() -> this.style.is("Lines"));
        this.distanceOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
        this.sizeOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
        this.blinkOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
        this.blinkOffscreenSpeed.visibleWhen(
                () -> this.style.is("Offscreen") && this.blinkOffscreen.get());
        this.friendOverride.visibleWhen(
                () -> this.distanceColors.get() && !this.ignoreFriends.get());
        this.teamColors.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
        this.friendColor.visibleWhen(
                () ->
                        !this.ignoreFriends.get()
                                && this.players.get()
                                && (!this.distanceColors.get() || this.friendOverride.get()));
        this.playersColor.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
        this.animalsColor.visibleWhen(() -> !this.distanceColors.get() && this.animals.get());
        this.waterAnimalsColor.visibleWhen(
                () -> !this.distanceColors.get() && this.waterAnimals.get());
        this.monstersColor.visibleWhen(() -> !this.distanceColors.get() && this.monsters.get());
        this.ambientColor.visibleWhen(() -> !this.distanceColors.get() && this.ambient.get());
        this.miscColor.visibleWhen(() -> !this.distanceColors.get() && this.misc.get());
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.options.hudHidden && this.style.is("Lines")) {
            for (Entity entity : this.targets(client)) {
                int color = this.colorOf(client, entity);
                Vec3d lerpedPos = entity.getLerpedPos(event.partialTick);
                double lengthY = entity.getBoundingBox().getLengthY();
                String target2 = this.target.get();
                double d =
                        switch (target2) {
                            case "Head" -> lengthY;
                            case "Body" -> lengthY / 2.0;
                            default -> 0.0;
                        };
                TracerEngine.draw(lerpedPos.add(0.0, d, 0.0), color);
                if (this.stem.get()) {
                    TracerEngine.stem(lerpedPos, lengthY, color);
                }
            }
        }
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled()
                && this.style.is("Offscreen")
                && !MinecraftClient.getInstance().options.hudHidden;
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickProgress = client.getRenderTickCounter().getTickProgress(false);
        float h = this.blinkOffscreen.get() ? this.blinkAlpha() : 1.0F;
        float distanceOffscreen2 = this.distanceOffscreen.get();
        float sizeOffscreen2 = this.sizeOffscreen.get();
        for (Entity entity : this.targets(client)) {
            Vector4f vector4f = TracerEngine.clip(entity.getLerpedPos(tickProgress));
            if (vector4f == null) {
                return;
            }
            if (!(vector4f.w > 0.0F)
                    || !(Math.abs(vector4f.x) < vector4f.w)
                    || !(Math.abs(vector4f.y) < vector4f.w)) {
                double d = Math.atan2(-vector4f.y * g, vector4f.x * f);
                int color = this.colorOf(client, entity);
                int i = (int) ((color >>> 24) * h);
                arrow(
                        imDrawList,
                        f * 0.5F + distanceOffscreen2 * (float) Math.cos(d),
                        g * 0.5F + distanceOffscreen2 * (float) Math.sin(d),
                        sizeOffscreen2,
                        (float) d,
                        GuiPalette.abgr(i << 24 | color & 16777215));
            }
        }
    }

    private List<Entity> targets(MinecraftClient client) {
        if (client.world != null && client.player != null) {
            FreecamModule module = ModuleRegistry.get(FreecamModule.class);
            boolean bl = this.ignoreSelf.get() || module == null || !module.isEnabled();
            double d = (double) this.maxDistance.get() * this.maxDistance.get();
            Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
            double e = (client.options.getViewDistance().getValue() + 1) * 16.0;
            ArrayList list = new ArrayList();
            for (Entity entity : client.world.getEntities()) {
                if ((entity != client.player || !bl)
                        && this.groupColor(entity) != null
                        && !(entity.squaredDistanceTo(client.player) > d)
                        && (!this.ignoreFriends.get() || !isFriend(entity))
                        && (this.showInvisible.get() || !entity.isInvisible())
                        && !(Math.abs(cameraPos.x - entity.getX()) >= e)
                        && !(Math.abs(cameraPos.z - entity.getZ()) >= e)) {
                    list.add(entity);
                }
            }
            return list;
        } else {
            return List.of();
        }
    }

    private ColorSetting groupColor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return this.players.get() ? this.playersColor : null;
        } else {
            return switch (entity.getType().getSpawnGroup()) {
                case CREATURE -> this.animals.get() ? this.animalsColor : null;
                case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS ->
                        this.waterAnimals.get() ? this.waterAnimalsColor : null;
                case MONSTER -> this.monsters.get() ? this.monstersColor : null;
                case AMBIENT -> this.ambient.get() ? this.ambientColor : null;
                default -> this.misc.get() ? this.miscColor : null;
            };
        }
    }

    private static boolean isFriend(Entity entity) {
        return !(entity instanceof PlayerEntity player)
                ? false
                : FriendStore.getInstance().isFriend(player.getGameProfile().name());
    }

    private int colorOf(MinecraftClient client, Entity entity) {
        boolean friend = isFriend(entity);
        if (!this.distanceColors.get()) {
            if (entity instanceof PlayerEntity player) {
                int i = this.playersColor.argb();
                int j = i & 0xFF000000;
                if (friend) {
                    return j | this.friendColor.rgb();
                } else {
                    int k = this.teamColors.get() ? teamColor(player) : 16777215;
                    return k == 16777215 ? i : j | k;
                }
            } else {
                return this.groupColor(entity).argb();
            }
        } else {
            return friend && this.friendOverride.get()
                    ? this.friendColor.argb()
                    : distanceColor(client, entity);
        }
    }

    private static int distanceColor(MinecraftClient client, Entity entity) {
        double d =
                client.gameRenderer.getCamera().getCameraPos().distanceTo(entity.getEyePos())
                        / 60.0;
        if (d > 1.0) {
            return -16711936;
        } else {
            int i = d < 0.5 ? 255 : 255 - (int) (255.0 * (d - 0.5) / 0.5);
            int j = d < 0.5 ? (int) (255.0 * d / 0.5) : 255;
            return 0xFF000000 | i << 16 | j << 8;
        }
    }

    private static int teamColor(PlayerEntity player) {
        Int2IntOpenHashMap int2IntOpenHashMap = new Int2IntOpenHashMap();
        player.getDisplayName()
                .visit(
                        (style, value) -> {
                            TextColor color = style.getColor();
                            int2IntOpenHashMap.addTo(
                                    color == null ? 16777215 : color.getRgb(), value.length());
                            return Optional.empty();
                        },
                        Style.EMPTY);
        int i = 16777215;
        int j = 0;
        for (Entry int2IntEntry : int2IntOpenHashMap.int2IntEntrySet()) {
            if (int2IntEntry.getIntValue() > j) {
                i = int2IntEntry.getIntKey();
                j = int2IntEntry.getIntValue();
            }
        }
        return i;
    }

    private float blinkAlpha() {
        double d = Util.getMeasuringTimeMs() * (this.blinkOffscreenSpeed.get() / 4.0);
        return (float) Math.abs(d % 1000.0 - 500.0) / 500.0F;
    }

    private static void arrow(ImDrawList imDrawList, float f, float g, float h, float f2, int i) {
        float g2 = (float) Math.cos(f2);
        float h2 = (float) Math.sin(f2);
        float f3 = f - h * 1.26795F / 3.0F;
        float[] floats = new float[] {f - h, f + h * 0.73205F, f - h};
        float[] floats2 = new float[] {g - h, g, g + h};
        for (int k = 0; k < 3; k++) {
            float g3 = floats[k] - f3;
            float h3 = floats2[k] - g;
            floats[k] = f3 + g3 * g2 - h3 * h2;
            floats2[k] = g + g3 * h2 + h3 * g2;
        }
        imDrawList.addTriangleFilled(
                floats[0], floats2[0], floats[1], floats2[1], floats[2], floats2[2], i);
    }
}
