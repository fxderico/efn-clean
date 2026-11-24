package net.efn.client.module.impl.render.character;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

import net.efn.client.chat.ModuleChat;
import net.efn.client.config.FriendStore;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.CrownLayer;
import net.efn.client.module.impl.render.CrownModule;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Detail;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.ShoulderParrotFeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckObjectsFeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class CharacterModule extends ClientModule {

    private static final long FRIEND_REFRESH_NANOS = 1000000000L;

    private static final float MODEL_PIXEL = 0.05859375F;

    private static volatile CharacterModule active;

    private final ModeSetting character = this.addSetting("character", characterSetting());

    private final BooleanSetting friends =
            this.addSetting("friends", new BooleanSetting("On Friends", false));

    private final ModeSetting detail =
            this.addSetting(
                    "detail",
                    new ModeSetting(
                            "Detail",
                            Detail.MEDIUM.label,
                            Detail.LOW.label,
                            Detail.MEDIUM.label,
                            Detail.HIGH.label));

    private final BooleanSetting heldItems =
            this.addSetting("held-items", new BooleanSetting("Held Items", false));

    private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();

    private long friendsCheckedAt;

    private int scanTicks;

    private WeakReference<ClientWorld> level = new WeakReference<>(null);

    public CharacterModule() {
        super(
                "character-3d",
                "3D Character",
                "Replaces your player model with a 3D character.",
                ModuleCategory.VISUALS);
        this.addSetting(
                "characters-folder", new ActionSetting("Open Characters Folder", this::openFolder));
        this.character.onChange(this::prepare);
        this.detail.onChange(this::prepare);
    }

    private static ModeSetting characterSetting() {
        CharacterCatalog.refresh();
        List list = CharacterCatalog.names();
        return new ModeSetting(
                "Character", (String) list.get(0), ((String[]) list.toArray(new String[0])));
    }

    private void refreshCharacters() {
        if (CharacterCatalog.refresh()) {
            this.character.setModes(CharacterCatalog.names());
        }
    }

    private void openFolder() {
        Path path = CustomCharacters.folder();
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            ModuleChat.send(
                    this, ModuleChat.body().danger("Couldn't create ").text(path.toString()));
            return;
        }
        this.refreshCharacters();
        Util.getOperatingSystem().open(path);
    }

    private void prepare() {
        if (this.isEnabled()) {
            CharacterCache.prepare(
                    CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
        }
    }

    private Detail effectiveDetail() {
        return Detail.byLabel(this.detail.get());
    }

    @Override
    protected void onEnable() {
        CrownModule module = ModuleRegistry.get(CrownModule.class);
        if (module != null) {
            module.setEnabled(false);
        }
        CharacterCache.listen();
        this.reset();
        this.refreshCharacters();
        this.prepare();
        active = this;
        this.on(
                TickEvent.TYPE,
                Stage.POST,
                tickEvent -> {
                    this.watchLevel();
                    if (++this.scanTicks % 40 == 0) {
                        this.refreshCharacters();
                    }
                });
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
        this.reset();
        CharacterCache.clear();
    }

    private void reset() {
        this.friendCache.clear();
        CharacterMotion.clear();
        this.level = new WeakReference<>(MinecraftClient.getInstance().world);
    }

    private void watchLevel() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (this.level.get() != world) {
            this.reset();
            CharacterCache.releaseTextures();
        }
    }

    public static void extract(Entity entity, PlayerEntityRenderState playerEntityRenderState) {
        CharacterModule module = active;
        WornCharacter wornCharacter =
                module == null ? null : module.resolve(playerEntityRenderState);
        CharacterHolder characterHolder = (CharacterHolder) playerEntityRenderState;
        characterHolder.efn$setCharacter(wornCharacter);
        if (wornCharacter != null) {
            Entity vehicle = entity.getVehicle();
            characterHolder.efn$setSeat(
                    vehicle == null ? 0.0F : (vehicle instanceof LivingEntity ? 1.35F : 0.3F));
            if (playerEntityRenderState.nameLabelPos != null) {
                CharacterModel model = wornCharacter.model;
                float f = model.maxY * model.scale - 32.0F;
                if (f > 0.0F) {
                    playerEntityRenderState.nameLabelPos =
                            playerEntityRenderState.nameLabelPos.add(
                                    0.0, f * 0.05859375F * playerEntityRenderState.baseScale, 0.0);
                }
            }
        }
    }

    static void baking(String string) {
        CharacterModule module = active;
        if (module != null) {
            ModuleChat.send(
                    module,
                    ModuleChat.body()
                            .text("Preparing ")
                            .name(string)
                            .text(", big models take a few seconds the first time"));
        }
    }

    static void notice(String string, List<String> list) {
        CharacterModule module = active;
        if (module != null) {
            ModuleChat.Body body = ModuleChat.body().name(string);
            if (list.isEmpty()) {
                body.text(" has no skeleton the client recognises, so it moves as one piece");
            } else {
                body.text(" only moves its ")
                        .value(String.join(" and ", list))
                        .text(", the rest of its skeleton wasn't recognised");
            }
            ModuleChat.send(module, body);
        }
    }

    static void failed(CharacterDesign characterDesign, Throwable throwable) {
        CharacterModule module = active;
        if (module != null) {
            Throwable throwable2 =
                    throwable instanceof UncheckedIOException && throwable.getCause() != null
                            ? throwable.getCause()
                            : throwable;
            String string =
                    !(throwable2 instanceof IOException)
                                    && !(throwable2 instanceof IllegalStateException)
                            ? "the file couldn't be read, see the log"
                            : throwable2.getMessage();
            ModuleChat.send(
                    module,
                    ModuleChat.body()
                            .text("Couldn't load ")
                            .name(characterDesign.name())
                            .text(": ")
                            .danger(string));
        }
    }

    static WornCharacter worn(EntityRenderState entityRenderState) {
        WornCharacter wornCharacter;
        if (entityRenderState instanceof CharacterHolder characterHolder) {
            wornCharacter = characterHolder.efn$character();
        } else {
            wornCharacter = null;
        }
        return wornCharacter;
    }

    static float seat(EntityRenderState entityRenderState) {
        float efn$seat;
        if (entityRenderState instanceof CharacterHolder characterHolder) {
            efn$seat = characterHolder.efn$seat();
        } else {
            efn$seat = 0.0F;
        }
        return efn$seat;
    }

    public static boolean hidesModel(EntityRenderState entityRenderState) {
        return worn(entityRenderState) != null;
    }

    public static boolean hidesLayer(
            FeatureRenderer<?, ?> featureRenderer, EntityRenderState entityRenderState) {
        return worn(entityRenderState) == null
                ? false
                : featureRenderer instanceof ArmorFeatureRenderer
                        || featureRenderer instanceof CapeFeatureRenderer
                        || featureRenderer instanceof ElytraFeatureRenderer
                        || featureRenderer instanceof StuckObjectsFeatureRenderer
                        || featureRenderer instanceof HeadFeatureRenderer
                        || featureRenderer instanceof ShoulderParrotFeatureRenderer
                        || featureRenderer instanceof Deadmau5FeatureRenderer
                        || featureRenderer instanceof CrownLayer
                        || featureRenderer instanceof HeldItemFeatureRenderer && !showsHeldItems();
    }

    private static boolean showsHeldItems() {
        CharacterModule module = active;
        return module != null && module.heldItems.get();
    }

    static WornCharacter firstPerson() {
        CharacterModule module = active;
        if (module == null) {
            return null;
        } else {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && !player.isSpectator()) {
                WornCharacter wornCharacter =
                        CharacterCache.ready(
                                CharacterCatalog.byName(module.character.get()),
                                module.effectiveDetail());
                return wornCharacter != null && wornCharacter.model.firstPersonArms
                        ? wornCharacter
                        : null;
            } else {
                return null;
            }
        }
    }

    private WornCharacter resolve(PlayerEntityRenderState playerEntityRenderState) {
        if (playerEntityRenderState.spectator) {
            return null;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            return !this.wears(client, playerEntityRenderState)
                    ? null
                    : CharacterCache.ready(
                            CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
        }
    }

    private boolean wears(MinecraftClient client, PlayerEntityRenderState playerEntityRenderState) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            if (playerEntityRenderState.id != player.getId()) {
                return this.friends.get() && this.isFriend(client, playerEntityRenderState.id);
            } else {
                return FreecamModule.active == null
                                && client.options.getPerspective().isFirstPerson()
                        ? client.currentScreen != null && !player.isSleeping()
                        : true;
            }
        } else {
            return false;
        }
    }

    private boolean isFriend(MinecraftClient client, int i) {
        long l = System.nanoTime();
        if (l - this.friendsCheckedAt > 1000000000L) {
            this.friendCache.clear();
            this.friendsCheckedAt = l;
        }
        if (this.friendCache.containsKey(i)) {
            return this.friendCache.get(i);
        } else {
            boolean friend;
            if (client.world.getEntityById(i) instanceof PlayerEntity player) {
                friend = FriendStore.getInstance().isFriend(player.getGameProfile().name());
            } else {
                friend = false;
            }
            this.friendCache.put(i, friend);
            return friend;
        }
    }
}
