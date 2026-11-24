package net.efn.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.pack.ModPackResources;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModDependency.Kind;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.resource.Resource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public final class TranslationGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String MOD_ID = "efn";

    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private static final ThreadLocal<TranslationGuard.Load> LOAD = new ThreadLocal<>();

    private static volatile int activeCount;

    private static volatile Set<String> hidden;

    private static volatile Set<String> ourMods;

    private static volatile TranslationGuard.Snapshot snapshot;

    private static Language wrapped;

    private static Set<String> guardedKeys;

    private static Language guarded;

    private TranslationGuard() {}

    public static boolean active() {
        return activeCount > 0 && DEPTH.get()[0] > 0;
    }

    public static String plain(Text text) {
        enter();
        String string;
        try {
            String string2 = text.getString();
            string = string2;
        } finally {
            exit();
        }
        return string;
    }

    public static void run(Runnable runnable) {
        enter();
        try {
            runnable.run();
        } finally {
            exit();
        }
    }

    public static synchronized Language language(Language language) {
        if (language == null) {
            return null;
        } else {
            Set set = hidden();
            if (language != wrapped || set != guardedKeys) {
                wrapped = language;
                guardedKeys = set;
                guarded = new TranslationGuard.GuardedLanguage(language, set);
            }
            return guarded;
        }
    }

    public static Text keybind(String string) {
        KeyBinding keyBinding = KeyBinding.byId(string);
        return (Text)
                (keyBinding != null && !hidden().contains(string)
                        ? keyBinding.getBoundKeyLocalizedText()
                        : Text.translatable(string));
    }

    public static void beginLoad() {
        LOAD.set(new TranslationGuard.Load());
    }

    public static BiConsumer<String, String> tap(
            Resource resource, BiConsumer<String, String> biConsumer) {
        TranslationGuard.Load load = LOAD.get();
        if (load == null) {
            return biConsumer;
        } else {
            load.seen = true;
            return ours(resource)
                    ? (value, value2) -> {
                        biConsumer.accept(value, value2);
                        load.provided.add(value);
                    }
                    : (value3, value4) -> {
                        biConsumer.accept(value3, value4);
                        load.clean.put(value3, value4);
                    };
        }
    }

    public static void endLoad() {
        TranslationGuard.Load load = LOAD.get();
        LOAD.remove();
        if (load != null && load.seen) {
            load.clean.keySet().retainAll(load.provided);
            snapshot =
                    new TranslationGuard.Snapshot(
                            Map.copyOf(load.clean), Set.copyOf(load.provided));
        }
    }

    private static void enter() {
        int[] ints = DEPTH.get();
        int i = ints[0]++;
        if (i == 0) {
            Class<TranslationGuard> classValue = TranslationGuard.class;
            synchronized (TranslationGuard.class) {
                activeCount++;
            }
        }
    }

    private static void exit() {
        int[] ints = DEPTH.get();
        if (ints[0] > 0 && --ints[0] == 0) {
            Class<TranslationGuard> classValue = TranslationGuard.class;
            synchronized (TranslationGuard.class) {
                activeCount--;
            }
        }
    }

    private static boolean ours(Resource resource) {
        return resource.getPack() instanceof ModPackResources modPackResources
                ? ours(modPackResources.getFabricModMetadata())
                : ourMods().contains(resource.getPackId());
    }

    private static Set<String> ourMods() {
        Set set = ourMods;
        if (set != null) {
            return set;
        } else {
            HashSet set2 = new HashSet();
            for (ModContainer allMod : FabricLoader.getInstance().getAllMods()) {
                if (ours(allMod.getMetadata())) {
                    set2.add(allMod.getMetadata().getId());
                }
            }
            ourMods = set = Set.copyOf(set2);
            return set;
        }
    }

    private static Set<String> hidden() {
        Set set = hidden;
        if (set != null) {
            return set;
        } else {
            Class<TranslationGuard> classValue = TranslationGuard.class;
            synchronized (TranslationGuard.class) {
                if (hidden != null) {
                    return hidden;
                } else {
                    HashSet set2 = new HashSet();
                    HashSet set3 = new HashSet();
                    set3.add("efn");
                    for (ModContainer allMod : FabricLoader.getInstance().getAllMods()) {
                        if (ours(allMod.getMetadata())) {
                            collect(allMod, set2, set3);
                        }
                    }
                    GameOptions options = MinecraftClient.getInstance().options;
                    if (options != null) {
                        for (KeyBinding allKey : options.allKeys) {
                            Identifier id = allKey.getCategory().id();
                            if (set3.contains(id.getNamespace())) {
                                set2.add(allKey.getId());
                                set2.add("key.category." + id.getNamespace() + "." + id.getPath());
                            }
                        }
                    }
                    Set set4 = Set.copyOf(set2);
                    if (options != null) {
                        hidden = set4;
                    }
                    return set4;
                }
            }
        }
    }

    private static boolean ours(ModMetadata modMetadata) {
        if ("efn".equals(modMetadata.getId())) {
            return true;
        } else {
            for (ModDependency dependency : modMetadata.getDependencies()) {
                if (dependency.getKind() == Kind.DEPENDS && "efn".equals(dependency.getModId())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void collect(ModContainer modContainer, Set<String> set, Set<String> set2) {
        for (Path rootPath : modContainer.getRootPaths()) {
            Path path = rootPath.resolve("assets");
            if (Files.isDirectory(path)) {
                for (Path path2 : list(path)) {
                    if (Files.isDirectory(path2)) {
                        set2.add(path2.getFileName().toString().replace("/", ""));
                        Path path3 = path2.resolve("lang");
                        if (Files.isDirectory(path3)) {
                            for (Path path4 : list(path3)) {
                                if (path4.getFileName().toString().endsWith(".json")) {
                                    try {
                                        BufferedReader bufferedReader =
                                                Files.newBufferedReader(
                                                        path4, StandardCharsets.UTF_8);
                                        try {
                                            JsonElement reader =
                                                    JsonParser.parseReader(bufferedReader);
                                            if (reader.isJsonObject()) {
                                                set.addAll(reader.getAsJsonObject().keySet());
                                            }
                                        } finally {
                                            if (bufferedReader != null) {
                                                bufferedReader.close();
                                            }
                                            continue;
                                        }
                                    } catch (RuntimeException | IOException exception) {
                                        LOGGER.warn("Could not read {}", path4, exception);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<Path> list(Path path) {
        try {
            List list;
            try (Stream stream = Files.list(path)) {
                list = stream.toList();
            }
            return list;
        } catch (IOException exception) {
            LOGGER.warn("Could not list {}", path, exception);
            return List.of();
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class GuardedLanguage extends Language {

        private final Language real;

        private final Set<String> hidden;

        GuardedLanguage(Language real2, Set<String> hidden2) {
            this.real = real2;
            this.hidden = hidden2;
        }

        @Override
        public String get(String key, String fallback) {
            TranslationGuard.Snapshot snapshot2 = TranslationGuard.snapshot;
            if (snapshot2 != null) {
                return snapshot2.provided().contains(key)
                        ? snapshot2.clean().getOrDefault(key, fallback)
                        : this.real.get(key, fallback);
            } else {
                return this.hidden.contains(key) ? fallback : this.real.get(key, fallback);
            }
        }

        @Override
        public boolean hasTranslation(String key) {
            TranslationGuard.Snapshot snapshot2 = TranslationGuard.snapshot;
            if (snapshot2 != null) {
                return snapshot2.provided().contains(key)
                        ? snapshot2.clean().containsKey(key)
                        : this.real.hasTranslation(key);
            } else {
                return !this.hidden.contains(key) && this.real.hasTranslation(key);
            }
        }

        @Override
        public boolean isRightToLeft() {
            return this.real.isRightToLeft();
        }

        @Override
        public OrderedText reorder(StringVisitable text) {
            return this.real.reorder(text);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Load {

        private final Map<String, String> clean = new HashMap<>();

        private final Set<String> provided = new HashSet<>();

        private boolean seen;

        private Load() {}
    }

    @Environment(EnvType.CLIENT)
    private record Snapshot(Map<String, String> clean, Set<String> provided) {}
}
