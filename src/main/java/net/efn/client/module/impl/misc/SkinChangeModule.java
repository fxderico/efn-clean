package net.efn.client.module.impl.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo.TextureAsset;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public final class SkinChangeModule extends ClientModule {

    private final StringSetting selfPlayerSetting =
            this.addSetting("self-player", new StringSetting("Self Player", ""));

    private final StringSetting othersPlayerSetting =
            this.addSetting("others-player", new StringSetting("Others Player", ""));

    private volatile SkinChangeModule.CustomSkin selfSkin;

    private volatile SkinChangeModule.CustomSkin othersSkin;

    private String loadedSelfName = "";

    private String loadedOthersName = "";

    private boolean selfLoading;

    private boolean othersLoading;

    private final SkinChangeModule.SwapCache selfSwaps = new SkinChangeModule.SwapCache();

    private final SkinChangeModule.SwapCache othersSwaps = new SkinChangeModule.SwapCache();

    public SkinChangeModule() {
        super(
                "skin-change",
                "Skin Change",
                "Wear any player's skin client-side.",
                ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.reloadIfChanged();
        this.on(TickEvent.TYPE, tickEvent -> this.reloadIfChanged());
    }

    @Override
    protected void onDisable() {
        this.selfSkin = null;
        this.othersSkin = null;
        this.loadedSelfName = "";
        this.loadedOthersName = "";
        this.selfSwaps.clear();
        this.othersSwaps.clear();
    }

    private void reloadIfChanged() {
        String string = this.selfPlayerSetting.get().trim();
        String string2 = this.othersPlayerSetting.get().trim();
        if (!string.equals(this.loadedSelfName)) {
            this.selfSkin = null;
            if (string.isEmpty()) {
                this.loadedSelfName = "";
            } else if (!this.selfLoading) {
                this.loadedSelfName = string;
                this.selfLoading = true;
                this.loadFromUsername(string, true);
            }
        }
        if (!string2.equals(this.loadedOthersName)) {
            this.othersSkin = null;
            if (string2.isEmpty()) {
                this.loadedOthersName = "";
            } else if (!this.othersLoading) {
                this.loadedOthersName = string2;
                this.othersLoading = true;
                this.loadFromUsername(string2, false);
            }
        }
    }

    private void loadFromUsername(String string, boolean bl) {
        MinecraftClient client = MinecraftClient.getInstance();
        CompletableFuture.runAsync(
                () -> {
                    try {
                        String uUID = fetchUUID(string);
                        if (uUID == null) {
                            client.execute(() -> this.finishLoad(string, bl, null));
                            return;
                        }
                        SkinChangeModule.SkinSource skin = fetchSkin(uUID);
                        if (skin == null) {
                            client.execute(() -> this.finishLoad(string, bl, null));
                            return;
                        }
                        Identifier id =
                                bl
                                        ? Identifier.of("efn", "skin/self")
                                        : Identifier.of("efn", "skin/others");
                        Path path = client.runDirectory.toPath().resolve("cache/efn_skins");
                        try {
                            Files.createDirectories(path);
                        } catch (IOException exception) {
                        }
                        Path path2 = path.resolve((bl ? "self_" : "others_") + uUID + ".png");
                        registerSkin(id, path2, skin.url())
                                .whenCompleteAsync(
                                        (textureAsset, throwable) ->
                                                this.finishLoad(
                                                        string,
                                                        bl,
                                                        textureAsset == null
                                                                ? null
                                                                : new SkinChangeModule.CustomSkin(
                                                                        textureAsset,
                                                                        skin.model())),
                                        client);
                    } catch (Exception exception2) {
                        client.execute(() -> this.finishLoad(string, bl, null));
                    }
                });
    }

    private void finishLoad(String string, boolean bl, SkinChangeModule.CustomSkin selfSkin2) {
        if (bl) {
            this.selfLoading = false;
        } else {
            this.othersLoading = false;
        }
        if (this.isEnabled()) {
            String string2 = (bl ? this.selfPlayerSetting : this.othersPlayerSetting).get().trim();
            if (selfSkin2 != null
                    && string.equals(string2)
                    && string.equals(bl ? this.loadedSelfName : this.loadedOthersName)) {
                if (bl) {
                    this.selfSkin = selfSkin2;
                } else {
                    this.othersSkin = selfSkin2;
                }
            }
            this.reloadIfChanged();
        }
    }

    private static CompletableFuture<TextureAsset> registerSkin(
            Identifier id, Path path, String string) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerSkinTextureDownloader playerSkinTextureDownloader =
                new PlayerSkinTextureDownloader(
                        client.getNetworkProxy(), client.getTextureManager(), client);
        return playerSkinTextureDownloader.downloadAndRegisterTexture(id, path, string, true);
    }

    private static String fetchUUID(String string) throws Exception {
        HttpURLConnection httpURLConnection =
                openGet("https://api.mojang.com/users/profiles/minecraft/" + string);
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode != 204 && responseCode != 404) {
            String string2;
            try (BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                JsonObject jsonObject = JsonParser.parseString(sb.toString()).getAsJsonObject();
                String string3 = jsonObject.get("id").getAsString();
                string2 = string3;
            }
            return string2;
        } else {
            return null;
        }
    }

    private static SkinChangeModule.SkinSource fetchSkin(String string) throws Exception {
        HttpURLConnection httpURLConnection =
                openGet(
                        "https://sessionserver.mojang.com/session/minecraft/profile/"
                                + string
                                + "?unsigned=false");
        if (httpURLConnection.getResponseCode() != 200) {
            return null;
        } else {
            String string2;
            try (BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                string2 = sb.toString();
            }
            JsonObject jsonObject = JsonParser.parseString(string2).getAsJsonObject();
            for (JsonElement json : jsonObject.getAsJsonArray("properties")) {
                JsonObject jsonObject2 = json.getAsJsonObject();
                if ("textures".equals(jsonObject2.get("name").getAsString())) {
                    String string3 =
                            new String(
                                    Base64.getDecoder()
                                            .decode(jsonObject2.get("value").getAsString()));
                    JsonObject jsonObject3 =
                            JsonParser.parseString(string3)
                                    .getAsJsonObject()
                                    .getAsJsonObject("textures");
                    if (!jsonObject3.has("SKIN")) {
                        return null;
                    }
                    JsonObject jsonObject4 = jsonObject3.getAsJsonObject("SKIN");
                    JsonObject json2 =
                            jsonObject4.has("metadata")
                                    ? jsonObject4.getAsJsonObject("metadata")
                                    : null;
                    String string4 =
                            json2 != null && json2.has("model")
                                    ? json2.get("model").getAsString()
                                    : null;
                    return new SkinChangeModule.SkinSource(
                            jsonObject4.get("url").getAsString(),
                            PlayerSkinType.byModelMetadata(string4));
                }
            }
            return null;
        }
    }

    private static HttpURLConnection openGet(String string) throws Exception {
        HttpURLConnection httpURLConnection =
                (HttpURLConnection) URI.create(string).toURL().openConnection();
        httpURLConnection.setConnectTimeout(5000);
        httpURLConnection.setReadTimeout(5000);
        httpURLConnection.setRequestProperty("User-Agent", "EFN/1.0");
        return httpURLConnection;
    }

    public SkinTextures skinFor(int i, SkinTextures skinTextures) {
        MinecraftClient client = MinecraftClient.getInstance();
        return this.skinFor(client.player != null && i == client.player.getId(), skinTextures);
    }

    public SkinTextures skinFor(UUID uuid, SkinTextures skinTextures) {
        MinecraftClient client = MinecraftClient.getInstance();
        return this.skinFor(
                client.player != null && client.player.getUuid().equals(uuid), skinTextures);
    }

    private SkinTextures skinFor(boolean bl, SkinTextures skinTextures) {
        SkinChangeModule.CustomSkin customSkin = bl ? this.selfSkin : this.othersSkin;
        return customSkin == null
                ? skinTextures
                : (bl ? this.selfSwaps : this.othersSwaps).swap(skinTextures, customSkin);
    }

    @Environment(EnvType.CLIENT)
    private record CustomSkin(TextureAsset body, PlayerSkinType model) {}

    @Environment(EnvType.CLIENT)
    private record SkinSource(String url, PlayerSkinType model) {}

    @Environment(EnvType.CLIENT)
    private static final class SwapCache {

        private static final int MAX = 256;

        private final Map<SkinTextures, SkinTextures> swapped = new IdentityHashMap<>();

        private SkinChangeModule.CustomSkin wearing;

        private SwapCache() {}

        SkinTextures swap(SkinTextures skinTextures, SkinChangeModule.CustomSkin wearing2) {
            if (wearing2 != this.wearing || this.swapped.size() >= 256) {
                this.swapped.clear();
                this.wearing = wearing2;
            }
            SkinTextures skinTextures2;
            if ((skinTextures2 = this.swapped.get(skinTextures)) == null) {
                skinTextures2 =
                        new SkinTextures(
                                wearing2.body(),
                                skinTextures.cape(),
                                skinTextures.elytra(),
                                wearing2.model(),
                                skinTextures.secure());
                this.swapped.put(skinTextures, skinTextures2);
            }
            return skinTextures2;
        }

        void clear() {
            this.swapped.clear();
            this.wearing = null;
        }
    }
}
