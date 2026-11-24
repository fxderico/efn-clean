/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.render;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.joml.Matrix3x2fStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Environment(value = EnvType.CLIENT)
public final class WaypointsModule extends ClientModule {

    private static final Pattern SPLIT = Pattern.compile("[\\s,]+");

    private static final Pattern DEATH_NAME = Pattern.compile("Death \\d{2}:\\d{2}");

    private static final DateTimeFormatter DEATH_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final double PROJECT_DISTANCE = 64.0;

    private static final double NETHER_SCALE = 8.0;

    private static final float FOCUS_RADIUS = 40.0f;

    private static final float FADE_BLOCKS = 4.0f;

    private static final int PADDING = 3;

    private static final int MARKER = 5;

    private static final int PANEL_RGB = 0x101014;

    private static final int TEXT_LABEL = -4934466;

    private final StringListSetting points =
            this.addSetting("points", new StringListSetting("Waypoints", new String[0]));

    private final KeySetting addKey =
            this.addSetting("add-key", new KeySetting("Add Here Key", -1));

    private final ActionSetting addHere =
            this.addSetting("add-here", new ActionSetting("Add Here", this::addHere));

    private final BooleanSetting deaths =
            this.addSetting("deaths", new BooleanSetting("Death Waypoints", true));

    private final NumberSetting deathMemory =
            this.addSetting(
                    "death-memory",
                    new NumberSetting("Death Memory", 3.0f, 1.0f, 10.0f).step(1.0f));

    private final BooleanSetting otherDimension =
            this.addSetting("other-dimension", new BooleanSetting("Show In Other Dimension", true));

    private final BooleanSetting showDistance =
            this.addSetting("show-distance", new BooleanSetting("Show Distance", true));

    private final NumberSetting maxDistance =
            this.addSetting(
                    "max-distance",
                    new NumberSetting("Max Distance", 0.0f, 0.0f, 30000.0f).step(100.0f));

    private final NumberSetting hideWithin =
            this.addSetting(
                    "hide-within", new NumberSetting("Hide Within", 3.0f, 0.0f, 32.0f).step(1.0f));

    private final NumberSetting scale =
            this.addSetting("scale", new NumberSetting("Scale", 1.0f, 0.5f, 2.0f).step(0.1f));

    private final ColorSetting color = this.addSetting("color", new ColorSetting("Color", -14006));

    private final ColorSetting deathColor =
            this.addSetting("death-color", new ColorSetting("Death Color", -47814));

    private List<Point> parsed;

    private boolean wasDead;

    public WaypointsModule() {
        super(
                "waypoints",
                "Waypoints",
                "Saved places shown on screen with distance. Entries are \"Name x y z\", optionally"
                        + " ending in nether or end.",
                ModuleCategory.RENDER);
        this.points.onChange(
                () -> {
                    this.parsed = null;
                });
        this.deathMemory.visibleWhen(this.deaths::get);
        this.deathColor.visibleWhen(this.deaths::get);
    }

    @Override
    protected void onEnable() {
        this.wasDead = false;
        this.on(
                KeyInputEvent.TYPE,
                Stage.PRE,
                EventPriority.NORMAL,
                keyInputEvent -> {
                    if (keyInputEvent.action == 1) {
                        this.pressed(keyInputEvent.key);
                    }
                });
        this.on(
                MouseInputEvent.TYPE,
                Stage.PRE,
                EventPriority.NORMAL,
                mouseInputEvent -> {
                    if (mouseInputEvent.action == 1) {
                        this.pressed(1000 + mouseInputEvent.button);
                    }
                });
    }

    private void pressed(int i) {
        if (!ModuleKeybinds.isBindable(i)
                || i != this.addKey.value()
                || MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        this.addHere();
    }

    private void addHere() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            this.say(ModuleChat.body().warn("Join a world first."));
            return;
        }
        String freeName = this.freeName();
        this.save(
                freeName,
                player.getBlockX(),
                player.getBlockY(),
                player.getBlockZ(),
                client.world.getRegistryKey());
    }

    private void save(String string, int i, int j, int k, RegistryKey<World> key) {
        String string2 = string + " " + i + " " + j + " " + k + WaypointsModule.suffix(key);
        if (!this.points.addValue(string2)) {
            this.say(
                    ModuleChat.body()
                            .warn("The waypoint list is full")
                            .text(" (64), remove one first."));
            return;
        }
        this.say(
                ModuleChat.body()
                        .good("Saved ")
                        .name(string)
                        .text(" at ")
                        .coords(i, j, k)
                        .text("."));
    }

    private String freeName() {
        List<Point> list = this.parsed();
        int i = list.size() + 1;
        while (true) {
            String string = "Waypoint " + i;
            if (list.stream().noneMatch(point -> point.name().equalsIgnoreCase(string))) {
                return string;
            }
            ++i;
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            this.wasDead = false;
            return;
        }
        boolean dead = player.isDead();
        if (dead && !this.wasDead && this.deaths.get()) {
            this.forgetOldDeaths(Math.max(0, this.deathMemory.getInt() - 1));
            this.save(
                    "Death " + LocalTime.now().format(DEATH_TIME),
                    player.getBlockX(),
                    player.getBlockY(),
                    player.getBlockZ(),
                    client.world.getRegistryKey());
        }
        this.wasDead = dead;
    }

    private void forgetOldDeaths(int i) {
        int j;
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (j = 0; j < this.points.size(); ++j) {
            Point point2 = WaypointsModule.parse(this.points.entry(j).get());
            if (point2 == null || !point2.death()) continue;
            list.add(j);
        }
        for (j = list.size() - i - 1; j >= 0; --j) {
            this.points.remove((Integer) list.get(j));
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || client.options.hudHidden) {
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) {
            return;
        }
        Vec3d cameraPos = camera.getCameraPos();
        RegistryKey<World> registryKey = client.world.getRegistryKey();
        double maxDistance2 = this.maxDistance.get();
        ArrayList<Shown> list = new ArrayList<Shown>();
        for (Point point2 : this.parsed()) {
            Shown shown = this.place(point2, registryKey, cameraPos);
            if (shown == null || maxDistance2 > 0.0 && shown.distance() > maxDistance2) continue;
            list.add(shown);
        }
        list.sort(Comparator.comparingDouble(Shown::distance).reversed());
        DrawContext graphics = event.graphics;
        for (Shown shown2 : list) {
            this.draw(graphics, client, cameraPos, shown2);
        }
    }

    private Shown place(Point point2, RegistryKey<World> key, Vec3d vec) {
        double d = (double) point2.x() + 0.5;
        double e = (double) point2.z() + 0.5;
        boolean bl = false;
        if (point2.dimension() != key) {
            if (!this.otherDimension.get()) {
                return null;
            }
            if (point2.dimension() == World.OVERWORLD && key == World.NETHER) {
                d /= 8.0;
                e /= 8.0;
            } else if (point2.dimension() == World.NETHER && key == World.OVERWORLD) {
                d *= 8.0;
                e *= 8.0;
            } else {
                return null;
            }
            bl = true;
        }
        double d2 = point2.y() == null || bl ? vec.y : (double) point2.y().intValue() + 0.5;
        Vec3d vec2 = new Vec3d(d, d2, e);
        return new Shown(point2, vec2, vec2.distanceTo(vec), bl);
    }

    private void draw(DrawContext context, MinecraftClient client, Vec3d vec, Shown shown) {
        int i;
        float f;
        float g;
        float h;
        float f2;
        float g2 =
                Math.min(
                        1.0f,
                        Math.max(
                                0.0f,
                                (float) (shown.distance() - (double) this.hideWithin.get())
                                        / 4.0f));
        if (g2 <= 0.02f) {
            return;
        }
        Vec3d vec2 = shown.at().subtract(vec);
        Vec3d vec3 =
                shown.distance() > 64.0
                        ? vec.add(vec2.multiply(64.0 / shown.distance()))
                        : shown.at();
        float[] floats =
                WorldLabel.project(
                        client,
                        vec3,
                        context.getScaledWindowWidth(),
                        context.getScaledWindowHeight());
        if (floats == null) {
            return;
        }
        Point point2 = shown.point();
        TextRenderer textRenderer = client.textRenderer;
        int j = point2.death() ? this.deathColor.argb() : this.color.argb();
        String string = point2.name();
        Object object = this.showDistance.get() ? WaypointsModule.distance(shown.distance()) : null;
        String string2 = (String) object;
        if (shown.converted()) {
            object =
                    (String) (object == null ? "" : (String) object + " ")
                            + "("
                            + WaypointsModule.dimensionName(point2.dimension())
                            + ")";
        }
        String string3 =
                (h = floats[0] - (float) context.getScaledWindowWidth() * 0.5f) * h
                                        + (f =
                                                        floats[1]
                                                                - (float)
                                                                                context
                                                                                        .getScaledWindowHeight()
                                                                        * 0.5f)
                                                * f
                                <= 1600.0f
                        ? point2.x()
                                + ", "
                                + String.valueOf(point2.y() == null ? "~" : point2.y())
                                + ", "
                                + point2.z()
                        : null;
        int k =
                textRenderer.getWidth(string)
                        + (object != null ? 4 + textRenderer.getWidth((String) object) : 0);
        int m = Math.max(k, string3 != null ? textRenderer.getWidth(string3) : 0) + 6;
        Objects.requireNonNull(textRenderer);
        if (string3 != null) {
            Objects.requireNonNull(textRenderer);
            i = 10;
        } else {
            i = 0;
        }
        int n = 15 + i;
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(floats[0], floats[1]);
        float scale2 = this.scale.get();
        matrices.scale(scale2, scale2);
        int fade = WaypointsModule.fade(j, g2);
        context.fill(-2, -2, 3, 3, WaypointsModule.fade(-16777216, g2 * 0.6f));
        context.fill(-1, -1, 2, 2, fade);
        int o = -m / 2;
        int p = -7 - n;
        context.fill(o, p, o + m, p + n, WaypointsModule.fade(-15724524, g2 * 0.55f));
        context.fill(o, p + n - 1, o + m, p + n, fade);
        int q = o + 3;
        int r = p + 3;
        context.drawText(textRenderer, string, q, r, WaypointsModule.fade(j, g2), true);
        if (object != null) {
            context.drawText(
                    textRenderer,
                    (String) object,
                    q + textRenderer.getWidth(string) + 4,
                    r,
                    WaypointsModule.fade(-4934466, g2),
                    true);
        }
        if (string3 != null) {
            Objects.requireNonNull(textRenderer);
            context.drawText(
                    textRenderer, string3, q, r + 9 + 1, WaypointsModule.fade(-1, g2), true);
        }
        matrices.popMatrix();
    }

    private List<Point> parsed() {
        if (this.parsed == null) {
            ArrayList<Point> list = new ArrayList<Point>();
            for (String string : this.points.get()) {
                Point point2 = WaypointsModule.parse(string);
                if (point2 == null) continue;
                list.add(point2);
            }
            this.parsed = List.copyOf(list);
        }
        return this.parsed;
    }

    private static Point parse(String string) {
        Integer integer;
        RegistryKey<World> key;
        ArrayList<String> list = new ArrayList<String>(List.of(SPLIT.split(string.trim())));
        list.removeIf(String::isEmpty);
        RegistryKey<World> oVERWORLD = World.OVERWORLD;
        if (!list.isEmpty()
                && (key = WaypointsModule.dimension(list.get(list.size() - 1))) != null) {
            oVERWORLD = key;
            list.remove(list.size() - 1);
        }
        ArrayList<Integer> list2 = new ArrayList<Integer>();
        while (!list.isEmpty()
                && list2.size() < 3
                && (integer = WaypointsModule.number(list.get(list.size() - 1))) != null) {
            list2.add(0, integer);
            list.remove(list.size() - 1);
        }
        if (list2.size() < 2) {
            return null;
        }
        String string2 = String.join((CharSequence) " ", list).trim();
        if (string2.isEmpty()) {
            string2 = "Waypoint";
        }
        boolean bl = DEATH_NAME.matcher(string2).matches();
        return list2.size() == 3
                ? new Point(
                        string2,
                        (Integer) list2.get(0),
                        (Integer) list2.get(1),
                        (Integer) list2.get(2),
                        oVERWORLD,
                        bl)
                : new Point(
                        string2,
                        (Integer) list2.get(0),
                        null,
                        (Integer) list2.get(1),
                        oVERWORLD,
                        bl);
    }

    private static Integer number(String string) {
        try {
            return (int) Math.floor(Double.parseDouble(string));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static RegistryKey<World> dimension(String string) {
        return switch (string.toLowerCase(Locale.ROOT)) {
            case "nether", "the_nether", "minecraft:the_nether" -> World.NETHER;
            case "end", "the_end", "minecraft:the_end" -> World.END;
            case "overworld", "minecraft:overworld" -> World.OVERWORLD;
            default -> null;
        };
    }

    private static String suffix(RegistryKey<World> key) {
        return key == World.NETHER ? " nether" : (key == World.END ? " end" : "");
    }

    private static String dimensionName(RegistryKey<World> key) {
        return key == World.NETHER ? "Nether" : (key == World.END ? "End" : "Overworld");
    }

    private static String distance(double d) {
        if (d >= 10000.0) {
            return String.format(Locale.ROOT, "%.1fk m", d / 1000.0);
        }
        return Math.round(d) + "m";
    }

    private static int fade(int i, float f) {
        int j = Math.round((float) (i >>> 24 & 0xFF) * Math.max(0.0f, Math.min(1.0f, f)));
        return j << 24 | i & 0xFFFFFF;
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send((ClientModule) this, body);
    }

    @Environment(value = EnvType.CLIENT)
    private record Point(
            String name, int x, Integer y, int z, RegistryKey<World> dimension, boolean death) {}

    @Environment(value = EnvType.CLIENT)
    private record Shown(Point point, Vec3d at, double distance, boolean converted) {}
}
