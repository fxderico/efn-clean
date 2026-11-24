package net.efn.client.module.impl.render.character;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;

@Environment(EnvType.CLIENT)
final class CharacterTexture extends AbstractTexture {

    CharacterTexture(String string, int i, int j, int[][] intGrid) {
        GpuDevice device = RenderSystem.getDevice();
        this.glTexture =
                device.createTexture(() -> string, 5, TextureFormat.RGBA8, i, j, 1, intGrid.length);
        this.glTextureView = device.createTextureView(this.glTexture);
        CommandEncoder commandEncoder = device.createCommandEncoder();
        int k = i;
        int m = j;
        for (int n = 0; n < intGrid.length; n++) {
            int[] ints = intGrid[n];
            try (NativeImage nativeImage = new NativeImage(k, m, false)) {
                for (int o = 0; o < m; o++) {
                    for (int p = 0; p < k; p++) {
                        nativeImage.setColorArgb(p, o, ints[o * k + p]);
                    }
                }
                commandEncoder.writeToTexture(this.glTexture, nativeImage, n, 0, 0, 0, k, m, 0, 0);
            }
            k = Math.max(1, k / 2);
            m = Math.max(1, m / 2);
        }
        this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true);
    }
}
