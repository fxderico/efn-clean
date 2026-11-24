package net.efn.client.module.impl.render.character.gltf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public interface ImageDecoder {

    ImageDecoder.Picture decode(byte[] bytes) throws IOException;

    @Environment(EnvType.CLIENT)
    public record Picture(int width, int height, int[] argb) {}
}
