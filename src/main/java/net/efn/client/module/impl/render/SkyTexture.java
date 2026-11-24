package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.ColorHelper;

import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
final class SkyTexture implements SkyImage {

    private final int width;

    private final int height;

    private boolean mipmapped;

    private GpuTexture texture;

    private GpuTextureView view;

    private SkyTexture(int width2, int height2, GpuTexture texture2, GpuTextureView view2) {
        this.width = width2;
        this.height = height2;
        this.texture = texture2;
        this.view = view2;
    }

    static SkyTexture upload(String string, int i, int j, int[][] intGrid) {
        if (intGrid.length <= 1) {
            return upload(string, i, j, intGrid.length == 0 ? new int[0] : intGrid[0]);
        } else {
            for (int k = 0; k < intGrid.length; k++) {
                if (intGrid[k].length < SkyMips.size(i, k) * SkyMips.size(j, k)) {
                    throw new IllegalArgumentException(
                            "Sky texture " + string + " is missing pixels at mip level " + k);
                }
            }
            String string2 = "efn:sky/" + string;
            GpuDevice device = RenderSystem.getDevice();
            GpuTexture texture2 =
                    device.createTexture(
                            () -> string2, 5, TextureFormat.RGBA8, i, j, 1, intGrid.length);
            try {
                for (int m = 0; m < intGrid.length; m++) {
                    int n = SkyMips.size(i, m);
                    int o = SkyMips.size(j, m);
                    int[] ints = intGrid[m];
                    try (NativeImage nativeImage = new NativeImage(n, o, false)) {
                        IntBuffer intBuffer = MemoryUtil.memIntBuffer(nativeImage.imageId(), n * o);
                        for (int p = 0; p < n * o; p++) {
                            intBuffer.put(p, ColorHelper.toAbgr(ints[p]));
                        }
                        device.createCommandEncoder()
                                .writeToTexture(texture2, nativeImage, m, 0, 0, 0, n, o, 0, 0);
                    }
                }
                SkyTexture skyTexture =
                        new SkyTexture(i, j, texture2, device.createTextureView(texture2));
                skyTexture.mipmapped = true;
                return skyTexture;
            } catch (RuntimeException exception) {
                texture2.close();
                throw exception;
            }
        }
    }

    static SkyTexture upload(String string, int i, int j, int[] ints) {
        if (ints.length < i * j) {
            throw new IllegalArgumentException("Sky texture " + string + " is missing pixels");
        } else {
            String string2 = "efn:sky/" + string;
            GpuDevice device = RenderSystem.getDevice();
            SkyTexture skyTexture;
            try (NativeImage nativeImage = new NativeImage(i, j, false)) {
                IntBuffer intBuffer = MemoryUtil.memIntBuffer(nativeImage.imageId(), i * j);
                for (int k = 0; k < i * j; k++) {
                    intBuffer.put(k, ColorHelper.toAbgr(ints[k]));
                }
                GpuTexture texture2 =
                        device.createTexture(() -> string2, 5, TextureFormat.RGBA8, i, j, 1, 1);
                try {
                    device.createCommandEncoder().writeToTexture(texture2, nativeImage);
                    SkyTexture skyTexture2 =
                            new SkyTexture(i, j, texture2, device.createTextureView(texture2));
                    skyTexture = skyTexture2;
                } catch (RuntimeException exception) {
                    texture2.close();
                    throw exception;
                }
            }
            return skyTexture;
        }
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    GpuTextureView view() {
        return this.view;
    }

    GpuSampler sampler() {
        return RenderSystem.getSamplerCache().get(FilterMode.LINEAR, this.mipmapped);
    }

    @Override
    public void close() {
        if (this.view != null) {
            this.view.close();
        }
        if (this.texture != null) {
            this.texture.close();
        }
        this.view = null;
        this.texture = null;
    }
}
