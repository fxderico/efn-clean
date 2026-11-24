package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.CharacterBuilder;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Detail;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Environment(EnvType.CLIENT)
final class CharacterCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int KEEP = 4;

    private static final ForkJoinPool POOL =
            new ForkJoinPool(
                    Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2)),
                    forkJoinPool -> {
                        ForkJoinWorkerThread thread =
                                ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(
                                        forkJoinPool);
                        thread.setName("EFN Character Builder " + thread.getPoolIndex());
                        thread.setDaemon(true);
                        thread.setPriority(4);
                        return thread;
                    },
                    null,
                    false);

    private static final Map<String, CharacterCache.Entry> ENTRIES =
            new LinkedHashMap<>(8, 0.75F, true);

    private static boolean listening;

    private static CharacterCache.Entry shown;

    private CharacterCache() {}

    static void listen() {
        if (!listening) {
            listening = true;
            ResourceLoader.get(ResourceType.CLIENT_RESOURCES)
                    .registerReloader(
                            Identifier.of("efn", "character_textures"),
                            (SynchronousResourceReloader) resourceManager -> releaseTextures());
        }
    }

    static void prepare(CharacterDesign characterDesign, Detail detail2) {
        String string = characterDesign.id() + "/" + detail2.name();
        if (!ENTRIES.containsKey(string)) {
            CharacterCache.Entry entry = new CharacterCache.Entry(characterDesign, detail2);
            ENTRIES.put(string, entry);
            trim();
            CompletableFuture.runAsync(() -> build(entry), POOL);
        }
    }

    static WornCharacter ready(CharacterDesign characterDesign, Detail detail2) {
        String string = characterDesign.id() + "/" + detail2.name();
        CharacterCache.Entry entry = ENTRIES.get(string);
        if (entry == null) {
            CharacterCache.Entry entry2 = new CharacterCache.Entry(characterDesign, detail2);
            ENTRIES.put(string, entry2);
            trim();
            CompletableFuture.runAsync(() -> build(entry2), POOL);
            return previous();
        } else if (entry.worn != null) {
            shown = entry;
            return entry.worn;
        } else {
            CharacterModel model2 = entry.model;
            if (entry.failed) {
                return null;
            } else if (model2 == null) {
                return previous();
            } else {
                MinecraftClient.getInstance()
                        .getTextureManager()
                        .registerTexture(
                                entry.texture,
                                new CharacterTexture(
                                        entry.texture.toString(),
                                        model2.atlasWidth,
                                        model2.atlasHeight,
                                        model2.atlasMips));
                entry.worn = new WornCharacter(model2, entry.texture);
                shown = entry;
                return entry.worn;
            }
        }
    }

    private static WornCharacter previous() {
        CharacterCache.Entry entry = shown;
        return entry != null && ENTRIES.containsValue(entry) ? entry.worn : null;
    }

    private static void build(CharacterCache.Entry entry) {
        try {
            CharacterModel characterModel = CharacterBuilder.build(entry.design, entry.detail);
            LOGGER.info(
                    "[EFN] 3D Character '{}' ({}) built: {} triangles in {} ms",
                    entry.design.name(),
                    entry.detail.label,
                    characterModel.triangles(),
                    characterModel.buildMillis);
            entry.model = characterModel;
        } catch (Throwable exception) {
            entry.failed = true;
            LOGGER.error(
                    "[EFN] 3D Character '{}' ({}) failed to build",
                    entry.design.name(),
                    entry.detail.label,
                    exception);
            CharacterModule.failed(entry.design, exception);
        }
    }

    private static void trim() {
        Iterator iterator = ENTRIES.values().iterator();
        while (ENTRIES.size() > 4 && iterator.hasNext()) {
            CharacterCache.Entry entry = (CharacterCache.Entry) iterator.next();
            release(entry);
            iterator.remove();
        }
    }

    static void releaseTextures() {
        for (CharacterCache.Entry entry : ENTRIES.values()) {
            release(entry);
        }
    }

    static void clear() {
        releaseTextures();
        ENTRIES.clear();
        shown = null;
    }

    private static void release(CharacterCache.Entry entry) {
        if (entry.worn != null) {
            entry.worn = null;
            MinecraftClient.getInstance().getTextureManager().destroyTexture(entry.texture);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Entry {

        final CharacterDesign design;

        final Detail detail;

        final Identifier texture;

        volatile CharacterModel model;

        volatile boolean failed;

        WornCharacter worn;

        Entry(CharacterDesign design2, Detail detail2) {
            this.design = design2;
            this.detail = detail2;
            this.texture =
                    Identifier.of(
                            "efn",
                            "character/"
                                    + design2.id()
                                    + "_"
                                    + detail2.name().toLowerCase(Locale.ROOT));
        }
    }
}
