/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.easyplace.compat;

import net.efn.client.easyplace.EasyPlace;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(value = EnvType.CLIENT)
public final class BaritoneCompat {

    private static final String[] CANDIDATES =
            new String[] {"baritone.api.BaritoneAPI", "baritone.BaritoneAPI"};

    private static final List<String> BUILD_SETTINGS = List.of("allowBreak", "allowPlace");

    private static Boolean available;

    private static String resolvedClass;

    private static Object cachedBaritone;

    private static boolean commanded;

    private static Map<String, String> restore;

    private BaritoneCompat() {}

    public static boolean isAvailable() {
        if (available == null) {
            available = false;
            for (String string : CANDIDATES) {
                try {
                    Class.forName(string);
                    resolvedClass = string;
                    available = true;
                    break;
                } catch (ClassNotFoundException exception) {
                }
            }
        }
        return available;
    }

    public static boolean canWalk() {
        return BaritoneCompat.isAvailable() || BaritoneCompat.chatPrefix() != null;
    }

    public static void goTo(BlockPos pos) {
        if (BaritoneCompat.isAvailable()) {
            BaritoneCompat.setGoal("baritone.api.pathing.goals.GoalBlock", pos, -1);
            return;
        }
        BaritoneCompat.command("goto " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    public static void goNear(BlockPos pos, int i) {
        if (!BaritoneCompat.isAvailable()) {
            BaritoneCompat.goTo(pos);
            return;
        }
        BaritoneCompat.setGoal("baritone.api.pathing.goals.GoalNear", pos, i);
    }

    public static void cancel() {
        if (!BaritoneCompat.isAvailable()) {
            if (commanded) {
                BaritoneCompat.command("stop");
            }
            commanded = false;
            return;
        }
        try {
            Object baritone = BaritoneCompat.getBaritone();
            if (baritone == null) {
                return;
            }
            Method method = BaritoneCompat.findMethod(baritone, "getPathingBehavior");
            if (method == null) {
                return;
            }
            Object object = method.invoke(baritone, new Object[0]);
            if (object == null) {
                return;
            }
            Method methodByName = BaritoneCompat.findMethodByName(object, "cancelEverything");
            if (methodByName != null) {
                methodByName.invoke(object, new Object[0]);
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    public static void lockForBuild() {
        if (!restore.isEmpty()) {
            return;
        }
        HashMap<String, String> map = new HashMap<String, String>();
        Map<String, String> settingsFile = BaritoneCompat.readSettingsFile();
        for (String string : BUILD_SETTINGS) {
            map.put(string, settingsFile.getOrDefault(string.toLowerCase(), "true"));
            BaritoneCompat.setSetting(string, false);
        }
        restore = map;
    }

    public static void restoreSettings() {
        Map<String, String> map = restore;
        restore = Map.of();
        map.forEach(
                (value, value2) -> BaritoneCompat.setSetting(value, Boolean.parseBoolean(value2)));
    }

    private static void setSetting(String string, boolean bl) {
        if (BaritoneCompat.isAvailable()) {
            try {
                Class<?> classValue = Class.forName(resolvedClass);
                Object object =
                        classValue
                                .getMethod("getSettings", new Class[0])
                                .invoke(null, new Object[0]);
                Object object2 = object.getClass().getField(string).get(object);
                Field field = object2.getClass().getField("value");
                field.set(object2, bl);
            } catch (Exception exception) {
                BaritoneCompat.log(
                        "setting " + string + " failed: " + exception.getClass().getSimpleName());
            }
            return;
        }
        BaritoneCompat.command("set " + string + " " + bl);
    }

    private static void setGoal(String string, BlockPos pos, int i) {
        try {
            Object object;
            Object baritone = BaritoneCompat.getBaritone();
            if (baritone == null) {
                BaritoneCompat.log("getBaritone returned null");
                return;
            }
            Class<?> classValue = Class.forName(string);
            if (i >= 0) {
                try {
                    object =
                            classValue
                                    .getConstructor(BlockPos.class, Integer.TYPE)
                                    .newInstance(pos, i);
                } catch (NoSuchMethodException exception) {
                    object =
                            classValue
                                    .getConstructor(
                                            Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE)
                                    .newInstance(pos.getX(), pos.getY(), pos.getZ(), i);
                }
            } else {
                try {
                    object = classValue.getConstructor(BlockPos.class).newInstance(pos);
                } catch (NoSuchMethodException exception2) {
                    object =
                            classValue
                                    .getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE)
                                    .newInstance(pos.getX(), pos.getY(), pos.getZ());
                }
            }
            Method method = BaritoneCompat.findMethod(baritone, "getCustomGoalProcess");
            if (method == null) {
                BaritoneCompat.log("getCustomGoalProcess method not found");
                return;
            }
            Object object2 = method.invoke(baritone, new Object[0]);
            if (object2 == null) {
                BaritoneCompat.log("getCustomGoalProcess returned null");
                return;
            }
            Method methodByName = BaritoneCompat.findMethodByName(object2, "setGoalAndPath");
            if (methodByName == null) {
                BaritoneCompat.log("setGoalAndPath method not found");
                return;
            }
            methodByName.invoke(object2, object);
        } catch (Exception exception3) {
            BaritoneCompat.log(
                    "goal failed: "
                            + exception3.getClass().getSimpleName()
                            + ": "
                            + exception3.getMessage());
        }
    }

    private static void command(String string) {
        String chatPrefix = BaritoneCompat.chatPrefix();
        MinecraftClient client = MinecraftClient.getInstance();
        if (chatPrefix == null || client.getNetworkHandler() == null) {
            return;
        }
        client.getNetworkHandler().sendChatMessage(chatPrefix + string);
        commanded = !string.equals("stop");
    }

    private static String chatPrefix() {
        if (!FabricLoader.getInstance().isModLoaded("baritone")) {
            return null;
        }
        Map<String, String> settingsFile = BaritoneCompat.readSettingsFile();
        if ("false".equals(settingsFile.get("prefixcontrol"))) {
            return null;
        }
        String string = settingsFile.getOrDefault("prefix", "#");
        return string.isBlank() || string.startsWith("/") ? null : string;
    }

    private static Map<String, String> readSettingsFile() {
        HashMap<String, String> map = new HashMap<String, String>();
        Path path =
                MinecraftClient.getInstance()
                        .runDirectory
                        .toPath()
                        .resolve("baritone")
                        .resolve("settings.txt");
        try {
            if (!Files.isRegularFile(path, new LinkOption[0])) {
                return map;
            }
            for (String string : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String string2 = string.trim();
                int i = string2.indexOf(32);
                if (string2.isEmpty() || string2.startsWith("#") || i <= 0) continue;
                map.put(string2.substring(0, i).toLowerCase(), string2.substring(i + 1).trim());
            }
        } catch (Exception exception) {
            BaritoneCompat.log(
                    "could not read settings.txt: " + exception.getClass().getSimpleName());
        }
        return map;
    }

    private static void log(String string) {
        EasyPlace.LOG.warn("[Baritone] {}", (Object) string);
    }

    private static Object getBaritone() throws Exception {
        if (cachedBaritone != null) {
            return cachedBaritone;
        }
        Class<?> classValue = Class.forName(resolvedClass);
        Object object =
                classValue.getMethod("getProvider", new Class[0]).invoke(null, new Object[0]);
        if (object == null) {
            return null;
        }
        Method methodByName = BaritoneCompat.findMethodByName(object, "getPrimaryBaritone");
        if (methodByName == null) {
            return null;
        }
        cachedBaritone = methodByName.invoke(object, new Object[0]);
        return cachedBaritone;
    }

    private static Method findMethod(Object object, String string) {
        try {
            return object.getClass().getMethod(string, new Class[0]);
        } catch (Exception exception) {
            return null;
        }
    }

    private static Method findMethodByName(Object object, String string) {
        for (Class<?> classValue = object.getClass();
                classValue != null;
                classValue = classValue.getSuperclass()) {
            for (Method method : classValue.getMethods()) {
                if (!method.getName().equals(string)) continue;
                return method;
            }
        }
        return null;
    }

    static {
        restore = Map.of();
    }
}
