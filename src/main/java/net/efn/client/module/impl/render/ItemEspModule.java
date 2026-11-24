package net.efn.client.module.impl.render;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ItemColorListSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ItemEspModule extends ClientModule {

    private static ItemEspModule instance;

    private final NumberSetting sRange =
            this.addSetting("range", new NumberSetting("Range", 128.0F, 10.0F, 512.0F).step(1.0F));

    private final BooleanSetting sOutline =
            this.addSetting("outline", new BooleanSetting("Outline", true));

    private final BooleanSetting sTracers =
            this.addSetting("tracers", new BooleanSetting("Tracers", false));

    private final ItemColorListSetting sItemColors =
            this.addSetting(ItemColorListSetting.createDefault());

    public ItemEspModule() {
        super(
                "item-esp",
                "Item ESP",
                "Highlights selected dropped items with model outlines and tracers.",
                ModuleCategory.RENDER);
        instance = this;
    }

    public static boolean shouldGlow(Entity entity) {
        ItemEspModule module = instance;
        ItemEntity itemEntity;
        return module != null && module.isEnabled() && module.sOutline.get()
                ? entity instanceof ItemEntity
                        && module.wants(itemEntity = (ItemEntity) entity, 1.0F)
                : false;
    }

    public static int glowColor(Entity entity) {
        ItemEspModule module = instance;
        return module != null && entity instanceof ItemEntity itemEntity
                ? module.sItemColors.colorArgb(itemEntity.getStack().getItem()) & 16777215
                : 16777215;
    }

    private boolean wants(ItemEntity itemEntity, float f) {
        if (itemEntity.isRemoved()) {
            return false;
        } else {
            Item item = itemEntity.getStack().getItem();
            if (!this.sItemColors.isEnabled(item)) {
                return false;
            } else {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) {
                    return true;
                } else {
                    float g = Math.max(1.0F, this.sRange.get());
                    return itemEntity
                                    .getLerpedPos(f)
                                    .squaredDistanceTo(client.player.getCameraPosVec(f))
                            <= (double) g * g;
                }
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (this.sTracers.get() && this.sItemColors.hasAnyEnabled()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                for (Entity entity : client.world.getEntities()) {
                    ItemEntity itemEntity;
                    if (entity instanceof ItemEntity
                            && this.wants(itemEntity = (ItemEntity) entity, event.partialTick)) {
                        Vec3d lerpedPos = itemEntity.getLerpedPos(event.partialTick);
                        TracerEngine.draw(
                                lerpedPos,
                                this.sItemColors.colorArgb(itemEntity.getStack().getItem()));
                    }
                }
            }
        }
    }
}
