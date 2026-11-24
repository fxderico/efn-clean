package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class Surface {

    public final float[] pos;

    public final float[] nrm;

    public final float[] uv;

    public final int[] color;

    public final byte[] glow;

    public final float[] flat;

    public final int[] quads;

    public final int triangles;

    public float[] weight;

    public float[] joint;

    Surface(
            float[] pos2,
            float[] nrm2,
            float[] uv2,
            int[] color2,
            byte[] glow2,
            float[] flat2,
            int[] quads2,
            int triangles2) {
        this.pos = pos2;
        this.nrm = nrm2;
        this.uv = uv2;
        this.color = color2;
        this.glow = glow2;
        this.flat = flat2;
        this.quads = quads2;
        this.triangles = triangles2;
    }

    public int vertexCount() {
        return this.pos.length / 3;
    }

    public int quadCount() {
        return this.quads.length / 4;
    }

    public boolean glows() {
        return this.glow != null;
    }

    @Environment(EnvType.CLIENT)
    static final class Builder {

        float[] pos = new float[768];

        float[] nrm = new float[768];

        float[] uv = new float[512];

        int[] color = new int[256];

        byte[] glow = new byte[256];

        float[] flat = new float[256];

        boolean anyGlow;

        boolean anyFlat;

        int verts;

        int[] quads = new int[1024];

        int quadCount;

        int triangles;

        Builder() {}

        int vertex(
                float f,
                float g,
                float h,
                float f2,
                float g2,
                float h2,
                float f3,
                float g3,
                int i,
                boolean bl) {
            return this.vertex(f, g, h, f2, g2, h2, f3, g3, i, bl, 0.0F);
        }

        int vertex(
                float f,
                float g,
                float h,
                float f2,
                float g2,
                float h2,
                float f3,
                float g3,
                int i,
                boolean anyGlow2,
                float h3) {
            if (this.verts == this.color.length) {
                int j = this.verts * 2;
                this.pos = Arrays.copyOf(this.pos, j * 3);
                this.nrm = Arrays.copyOf(this.nrm, j * 3);
                this.uv = Arrays.copyOf(this.uv, j * 2);
                this.color = Arrays.copyOf(this.color, j);
                this.glow = Arrays.copyOf(this.glow, j);
                this.flat = Arrays.copyOf(this.flat, j);
            }
            int k = this.verts++;
            this.pos[k * 3] = f;
            this.pos[k * 3 + 1] = g;
            this.pos[k * 3 + 2] = h;
            this.nrm[k * 3] = f2;
            this.nrm[k * 3 + 1] = g2;
            this.nrm[k * 3 + 2] = h2;
            this.uv[k * 2] = f3;
            this.uv[k * 2 + 1] = g3;
            this.color[k] = i;
            this.glow[k] = (byte) (anyGlow2 ? 1 : 0);
            this.flat[k] = h3;
            this.anyGlow |= anyGlow2;
            this.anyFlat |= h3 > 0.0F;
            return k;
        }

        void quad(int i, int j, int k, int m, int triangles2) {
            if (this.quadCount * 4 + 4 > this.quads.length) {
                this.quads = Arrays.copyOf(this.quads, this.quads.length * 2);
            }
            this.quads[this.quadCount * 4] = i;
            this.quads[this.quadCount * 4 + 1] = j;
            this.quads[this.quadCount * 4 + 2] = k;
            this.quads[this.quadCount * 4 + 3] = m;
            this.quadCount++;
            this.triangles += triangles2;
        }

        Surface build() {
            return this.verts != 0 && this.quadCount != 0
                    ? new Surface(
                            Arrays.copyOf(this.pos, this.verts * 3),
                            Arrays.copyOf(this.nrm, this.verts * 3),
                            Arrays.copyOf(this.uv, this.verts * 2),
                            Arrays.copyOf(this.color, this.verts),
                            this.anyGlow ? Arrays.copyOf(this.glow, this.verts) : null,
                            this.anyFlat ? Arrays.copyOf(this.flat, this.verts) : null,
                            Arrays.copyOf(this.quads, this.quadCount * 4),
                            this.triangles)
                    : null;
        }
    }
}
