package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

import net.efn.client.config.FriendStore;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.lang.ref.WeakReference;

@Environment(EnvType.CLIENT)
abstract class HeadCosmeticModule extends ClientModule {

    static volatile HeadCosmeticModule worn;

    static final String COLOR_THEME = "Theme";

    static final String COLOR_MANUAL = "Manual";

    private static final long EPOCH = System.nanoTime();

    private static final long LOOK_REFRESH_NANOS = 4000000L;

    private static final long FRIEND_REFRESH_NANOS = 1000000000L;

    private static final float GLINT_CYCLE_SECONDS = 3.6F;

    private static final float GLINT_STRENGTH = 0.6F;

    private CrownMesh.Look look;

    private long lookAt;

    private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();

    private WeakReference<ClientWorld> friendLevel = new WeakReference<>(null);

    private long friendsCheckedAt;

    HeadCosmeticModule(String string, String string2, String string3) {
        super(string, string2, string3, ModuleCategory.VISUALS);
    }

    abstract CrownMesh headMesh(boolean bl);

    abstract CrownMesh bodyMesh(boolean bl);

    abstract float scale();

    abstract int[] palette();

    abstract boolean glows();

    abstract boolean glints();

    abstract boolean onFriends();

    @Override
    protected void onEnable() {
        CharacterModule module = ModuleRegistry.get(CharacterModule.class);
        if (module != null) {
            module.setEnabled(false);
        }
        this.reset();
        worn = this;
    }

    @Override
    protected void onDisable() {
        if (worn == this) {
            worn = null;
        }
        this.reset();
    }

    void reset() {
        this.look = null;
        this.friendCache.clear();
        this.friendLevel = new WeakReference<>(null);
    }

    final boolean wears(PlayerEntityRenderState playerEntityRenderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            return playerEntityRenderState.id == player.getId()
                    ? !client.options.getPerspective().isFirstPerson()
                            || client.currentScreen != null
                            || FreecamModule.active != null
                    : this.onFriends() && this.isFriend(client, playerEntityRenderState.id);
        } else {
            return false;
        }
    }

    private boolean isFriend(MinecraftClient client, int i) {
        long l = System.nanoTime();
        if (this.friendLevel.get() != client.world) {
            this.friendLevel = new WeakReference<>(client.world);
            this.friendCache.clear();
        }
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

    final CrownMesh.Look look() {
        long l = System.nanoTime();
        CrownMesh.Look look2 = this.look;
        if (look2 != null && l - this.lookAt < 4000000L) {
            return look2;
        } else {
            float f = (float) (l - EPOCH) / 1.0E9F;
            float g = f % 3.6F / 3.6F;
            float h = (g * 2.0F - 1.0F) * 3.1415927F * 1.25F;
            float f2 = MathHelper.sin(g * 3.1415927F);
            float g2 = this.glints() ? 0.6F * f2 * f2 : 0.0F;
            float h2 = 0.9F + 0.1F * MathHelper.sin(f * 2.2F);
            this.look =
                    look2 =
                            new CrownMesh.Look(
                                    this.palette(),
                                    this.glows(),
                                    MathHelper.cos(h),
                                    MathHelper.sin(h),
                                    g2,
                                    h2);
            this.lookAt = l;
            return look2;
        }
    }

    static int metal(int i) {
        return shade(i, 0.55F, 0.96F);
    }

    static int gem(int i) {
        return shade(i, 1.15F, 1.0F);
    }

    static int shade(int i, float f, float g) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        int n = Math.max(j, Math.max(k, m));
        int o = n - Math.min(j, Math.min(k, m));
        float h = 0.0F;
        if (o > 0) {
            h =
                    n == j
                            ? (float) (k - m) / o
                            : (n == k ? 2.0F + (float) (m - j) / o : 4.0F + (float) (j - k) / o);
            if ((h = h / 6.0F) < 0.0F) {
                h++;
            }
        }
        float f2 = n == 0 ? 0.0F : (float) o / n;
        return MathHelper.hsvToArgb(h, Math.min(1.0F, f2 * f), Math.min(1.0F, g), 255);
    }
}
