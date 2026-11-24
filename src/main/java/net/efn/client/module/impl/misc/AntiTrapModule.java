/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.event.impl.WorldEntityAddEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

@Environment(value = EnvType.CLIENT)
public final class AntiTrapModule extends ClientModule {

    private static final int SWEEP_INTERVAL_TICKS = 20;

    private final BooleanSetting removeExisting =
            this.addSetting("remove-existing", new BooleanSetting("Remove Existing", true));

    private final BooleanSetting preventSpawn =
            this.addSetting("prevent-spawn", new BooleanSetting("Prevent Spawn", true));

    private final BooleanSetting armorStands =
            this.addSetting("armor-stands", new BooleanSetting("Armor Stands", true));

    private final BooleanSetting chestMinecarts =
            this.addSetting("chest-minecarts", new BooleanSetting("Chest Minecarts", true));

    private final List<Entity> pendingSpawns = new ArrayList<Entity>();

    public AntiTrapModule() {
        super(
                "anti-trap",
                "Anti Trap",
                "Allows you to escape from armor stands and chest minecarts.",
                ModuleCategory.DONUT);
        this.usesNotifications();
    }

    @Override
    protected void onEnable() {
        this.pendingSpawns.clear();
        this.on(WorldEntityAddEvent.TYPE, this::onEntityAdded);
        this.on(TickEvent.TYPE, this::onTick);
        if (this.removeExisting.get()) {
            this.removeTrapEntities();
        }
    }

    @Override
    protected void onDisable() {
        this.pendingSpawns.clear();
    }

    private void onEntityAdded(WorldEntityAddEvent event) {
        if (!this.preventSpawn.get()) {
            return;
        }
        Entity entity = event.entity;
        if (this.isTrapEntity(entity)) {
            this.pendingSpawns.add(entity);
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) {
            this.pendingSpawns.clear();
            return;
        }
        if (!this.pendingSpawns.isEmpty()) {
            for (Entity pendingSpawn : this.pendingSpawns) {
                Entity entityById;
                if (pendingSpawn.isRemoved()
                        || pendingSpawn.getEntityWorld() != world
                        || (entityById = world.getEntityById(pendingSpawn.getId())) != null
                                && entityById != pendingSpawn
                        || !this.isTrapEntity(pendingSpawn)
                        || !AntiTrapModule.canRemove(client, player, pendingSpawn)) continue;
                pendingSpawn.discard();
            }
            this.pendingSpawns.clear();
        }
        if (player.age % 20 != 0) {
            return;
        }
        for (Entity entity : this.collectTrapEntities(client, world, player)) {
            entity.discard();
        }
    }

    private void removeTrapEntities() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        List<Entity> trapEntities = this.collectTrapEntities(client, world, client.player);
        for (Entity trapEntity : trapEntities) {
            trapEntity.discard();
        }
        if (!trapEntities.isEmpty()) {
            ModuleChat.send(
                    (ClientModule) this,
                    ModuleChat.body()
                            .good("Removed")
                            .text(" ")
                            .value(trapEntities.size())
                            .text(trapEntities.size() == 1 ? " trap entity" : " trap entities"));
        }
    }

    private List<Entity> collectTrapEntities(
            MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        ArrayList<Entity> list = new ArrayList<Entity>();
        for (Entity entity : world.getEntities()) {
            if (!this.isTrapEntity(entity) || !AntiTrapModule.canRemove(client, player, entity))
                continue;
            list.add(entity);
        }
        return list;
    }

    private boolean isTrapEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        EntityType<?> entityType = entity.getType();
        if (this.armorStands.get() && entityType == EntityType.ARMOR_STAND) {
            return true;
        }
        return this.chestMinecarts.get() && entityType == EntityType.CHEST_MINECART;
    }

    private static boolean canRemove(
            MinecraftClient client, ClientPlayerEntity player, Entity entity) {
        if (entity == client.getCameraEntity()) {
            return false;
        }
        if (player == null) {
            return true;
        }
        return entity != player && !entity.hasPassengerDeep(player);
    }
}
