/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.EfnClientMod;
import net.efn.client.chat.ModuleChat;
import net.efn.client.esp.BlockLayer;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.esp.EspStyle;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.LongPredicate;

@Environment(value = EnvType.CLIENT)
public final class SpawnerNotifierModule extends ClientModule implements ChunkScanner.Consumer {

    private static final String SPAWNER = "Spawner";

    private final BooleanSetting sSound =
            this.addSetting("sound", new BooleanSetting("Sound", true));

    private final BooleanSetting sRender =
            this.addSetting("render", new BooleanSetting("Render", true));

    private final ColorSetting sColor =
            this.addSetting("color", new ColorSetting("Color", -587246251));

    private final EspStyle style = new EspStyle(this::addSetting, "Outline", 0.1f, 256.0f);

    private final BlockLayer layer =
            new BlockLayer(
                    new BlockLayer.Palette() {

                        @Override
                        public int color(Object object) {
                            return SpawnerNotifierModule.this.sColor.argb();
                        }

                        @Override
                        public int tracerColor(Object object2) {
                            return 0;
                        }
                    },
                    () ->
                            MinecraftClient.getInstance().world == null
                                    ? null
                                    : MinecraftClient.getInstance().world.getRegistryKey());

    private final LongOpenHashSet notified = new LongOpenHashSet();

    public SpawnerNotifierModule() {
        super(
                "spawner-notifier",
                "Spawner Notifier",
                "Scans chunks for mob spawners and highlights them.",
                ModuleCategory.DONUT);
        this.usesNotifications();
    }

    @Override
    protected void onEnable() {
        ChunkScanner.register(this);
    }

    @Override
    protected void onDisable() {
        ChunkScanner.unregister(this);
        this.layer.clear();
        this.notified.clear();
    }

    @Override
    public boolean wants(BlockState state) {
        return state.isOf(Blocks.SPAWNER);
    }

    @Override
    public void found(long l, BlockState state) {
        this.layer.put(l, SPAWNER);
        if (this.notified.add(l)) {
            this.notify(BlockPos.fromLong(l));
        }
    }

    @Override
    public void removed(long l) {
        this.layer.remove(l);
    }

    @Override
    public void reset() {
        this.layer.clear();
        this.notified.clear();
    }

    @Override
    public void chunkScanned(long l, LongPredicate longPredicate) {
        this.layer.retainChunk(l, longPredicate);
    }

    private void notify(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.notificationsOn()) {
            ModuleChat.send((ClientModule) this, SpawnerNotifierModule.foundMessage(client, pos));
        }
        if (this.sSound.get() && client.player != null && client.world != null) {
            client.getSoundManager()
                    .play(
                            PositionedSoundInstance.ui(
                                    EfnClientMod.SPAWNER_NOTIFY_SOUND, 1.0f, 1.0f));
        }
    }

    private static ModuleChat.Body foundMessage(MinecraftClient client, BlockPos pos) {
        String entityName = SpawnerNotifierModule.entityName(client, pos);
        ModuleChat.Body body =
                entityName == null
                        ? ModuleChat.body().warn("Unknown")
                        : ModuleChat.body().name(entityName);
        body.text(" spawner ")
                .good("found")
                .text(" at ")
                .coords(pos.getX(), pos.getY(), pos.getZ());
        if (client.player != null) {
            long l = Math.round(client.player.getEntityPos().distanceTo(Vec3d.ofCenter(pos)));
            body.text(" (").value(l + "m").text(")");
        }
        return body;
    }

    private static String entityName(MinecraftClient client, BlockPos pos) {
        BlockEntity blockEntity;
        if (client.world == null
                || !((blockEntity = client.world.getBlockEntity(pos))
                        instanceof MobSpawnerBlockEntity)) {
            return null;
        }
        MobSpawnerBlockEntity mobSpawnerBlockEntity = (MobSpawnerBlockEntity) blockEntity;
        Entity renderedEntity =
                mobSpawnerBlockEntity.getLogic().getRenderedEntity(client.world, pos);
        return renderedEntity == null ? null : renderedEntity.getType().getName().getString();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (this.sRender.get()) {
            this.layer.render(event, this.style);
        }
    }
}
