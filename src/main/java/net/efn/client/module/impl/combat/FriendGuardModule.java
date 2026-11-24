package net.efn.client.module.impl.combat;

import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

@Environment(EnvType.CLIENT)
public final class FriendGuardModule extends ClientModule {

    private static final long HINT_COOLDOWN_MS = 1500L;

    private static volatile FriendGuardModule active;

    private long lastHintAt;

    public FriendGuardModule() {
        super(
                "friend-guard",
                "Friend Guard",
                "Stops you from hitting players on your friends list.",
                ModuleCategory.COMBAT);
        this.usesNotifications();
    }

    @Override
    protected void onEnable() {
        this.lastHintAt = 0L;
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        this.lastHintAt = 0L;
    }

    public static boolean intercepts(PlayerEntity player, Entity entity) {
        FriendGuardModule module = active;
        PlayerEntity player2;
        if (module == null
                || !(entity instanceof PlayerEntity)
                || (player2 = (PlayerEntity) entity) == player) {
            return false;
        } else if (player == MinecraftClient.getInstance().player
                && MeleeAttacks.isFriend(player2)) {
            module.hint(player2);
            return true;
        } else {
            return false;
        }
    }

    public static boolean interceptsStab(ClientPlayerEntity player) {
        FriendGuardModule module = active;
        if (module != null && player != null) {
            Entity entity2 =
                    MeleeAttacks.stabTarget(
                            player,
                            entity -> {
                                PlayerEntity player2;
                                return entity instanceof PlayerEntity
                                        && MeleeAttacks.isFriend(player2 = (PlayerEntity) entity);
                            });
            if (entity2 == null) {
                return false;
            } else {
                module.hint((PlayerEntity) entity2);
                return true;
            }
        } else {
            return false;
        }
    }

    private void hint(PlayerEntity player) {
        if (this.notificationsOn()) {
            long l = System.currentTimeMillis();
            if (l - this.lastHintAt >= 1500L) {
                this.lastHintAt = l;
                ModuleChat.actionBar(
                        this,
                        ModuleChat.body()
                                .text("Held back your hit on ")
                                .name(player.getGameProfile().name()));
            }
        }
    }
}
