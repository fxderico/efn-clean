package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Mesh {

    final float[] pos;

    final int vertexCount;

    final int[] tris;

    final int triCount;

    Mesh(float[] pos2, int vertexCount2, int[] tris2, int triCount2) {
        this.pos = pos2;
        this.vertexCount = vertexCount2;
        this.tris = tris2;
        this.triCount = triCount2;
    }
}
