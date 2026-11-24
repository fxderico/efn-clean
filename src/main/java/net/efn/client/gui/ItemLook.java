package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ItemLook extends ItemRenderState {

    public final List<Object> identity = new ArrayList<>();

    private final List<LayerRenderState> layers = new ArrayList<>();

    public ItemLook() {}

    @Override
    public void clear() {
        super.clear();
        this.identity.clear();
        this.layers.clear();
    }

    @Override
    public LayerRenderState newLayer() {
        LayerRenderState layer = super.newLayer();
        this.layers.add(layer);
        return layer;
    }

    @Override
    public void addModelKey(Object modelKey) {
        if (!(modelKey instanceof Glint)) {
            this.identity.add(modelKey);
        }
    }

    public boolean resolve(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            client.getItemModelManager()
                    .clearAndUpdate(
                            this, stack, ItemDisplayContext.GUI, client.world, client.player, 0);
        } catch (RuntimeException exception) {
            this.clear();
            return false;
        }
        for (LayerRenderState layer : this.layers) {
            layer.setGlint(Glint.NONE);
        }
        if (this.identity.isEmpty()) {
            this.identity.add(stack.getItem());
        }
        return true;
    }
}
