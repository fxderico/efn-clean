package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
final class SurfaceNets {

    private static final int BLOCK = 4;

    private static final int MAX_POINTS = 6000000;

    private static final int[] EDGE_A = new int[] {0, 2, 4, 6, 0, 1, 4, 5, 0, 1, 2, 3};

    private static final int[] EDGE_B = new int[] {1, 3, 5, 7, 2, 3, 6, 7, 4, 5, 6, 7};

    private final Shape shape;

    private final float ox;

    private final float oy;

    private final float oz;

    private final float h;

    private final int nx;

    private final int ny;

    private final int nz;

    private final int bx;

    private final int by;

    private final int bz;

    private final float[] field;

    private final boolean[] active;

    private SurfaceNets(
            Shape shape2, float ox2, float oy2, float oz2, int nx2, int ny2, int nz2, float f) {
        this.shape = shape2;
        this.ox = ox2;
        this.oy = oy2;
        this.oz = oz2;
        this.nx = nx2;
        this.ny = ny2;
        this.nz = nz2;
        this.h = f;
        this.bx = (nx2 + 4 - 1) / 4;
        this.by = (ny2 + 4 - 1) / 4;
        this.bz = (nz2 + 4 - 1) / 4;
        this.field = new float[nx2 * ny2 * nz2];
        Arrays.fill(this.field, 0.0F / 0.0F);
        this.active = new boolean[this.bx * this.by * this.bz];
    }

    static float fitStep(float f, float g, float f2, float g2) {
        float h2 = g2;
        while ((long) (f / h2 + 4.0F) * (long) (g / h2 + 4.0F) * (long) (f2 / h2 + 4.0F)
                > 6000000L) {
            h2 *= 1.12F;
        }
        return h2;
    }

    static Mesh mesh(
            Shape shape2, float f, float g, float f2, float g2, float h2, float f3, float g3) {
        float h3 = g3 * 3.0F;
        float f4;
        float g4;
        float h4;
        float f5;
        float g5;
        float h5;
        float fitStep =
                fitStep(
                        (f5 = g2 + h3) - (f4 = f - h3),
                        (g5 = h2 + h3) - (g4 = g - h3),
                        (h5 = f3 + h3) - (h4 = f2 - h3),
                        g3);
        int i = (int) Math.ceil((f5 - f4) / fitStep) + 1;
        int j = (int) Math.ceil((g5 - g4) / fitStep) + 1;
        int k = (int) Math.ceil((h5 - h4) / fitStep) + 1;
        SurfaceNets surfaceNets = new SurfaceNets(shape2, f4, g4, h4, i, j, k, fitStep);
        surfaceNets.evaluate();
        return surfaceNets.extract();
    }

    private void evaluate() {
        float f = 4.0F * this.h * 0.8660254F * 1.6F + this.h * 1.5F;
        float g = this.h * 2.5F;
        IntStream.range(0, this.bz)
                .parallel()
                .forEach(
                        i -> {
                            for (int j = 0; j < this.by; j++) {
                                for (int k = 0; k < this.bx; k++) {
                                    float f2 = this.ox + (k * 4 + 2.0F) * this.h;
                                    float g2 = this.oy + (j * 4 + 2.0F) * this.h;
                                    float h2 = this.oz + (i * 4 + 2.0F) * this.h;
                                    float distance = this.shape.distance(f2, g2, h2);
                                    if (!(distance < -f) && !(distance > f)) {
                                        this.active[(i * this.by + j) * this.bx + k] = true;
                                        Shape shape2 =
                                                this.shape.prune(
                                                        this.ox + k * 4 * this.h - g,
                                                        this.oy + j * 4 * this.h - g,
                                                        this.oz + i * 4 * this.h - g,
                                                        this.ox + (k + 1) * 4 * this.h + g,
                                                        this.oy + (j + 1) * 4 * this.h + g,
                                                        this.oz + (i + 1) * 4 * this.h + g);
                                        this.fill(shape2, k * 4, j * 4, i * 4);
                                    }
                                }
                            }
                        });
    }

    private void fill(Shape shape2, int i, int j, int k) {
        int m = Math.min(i + 4, this.nx);
        int n = Math.min(j + 4, this.ny);
        int o = Math.min(k + 4, this.nz);
        for (int p = k; p < o; p++) {
            for (int q = j; q < n; q++) {
                for (int r = i; r < m; r++) {
                    float distance =
                            shape2.distance(
                                    this.ox + r * this.h,
                                    this.oy + q * this.h,
                                    this.oz + p * this.h);
                    this.field[(p * this.ny + q) * this.nx + r] =
                            distance != distance ? 1.0F : distance;
                }
            }
        }
    }

    private float value(int i, int j, int k) {
        int m = (k * this.ny + j) * this.nx + i;
        float f = this.field[m];
        if (f != f) {
            f =
                    this.shape.distance(
                            this.ox + i * this.h, this.oy + j * this.h, this.oz + k * this.h);
            if (f != f) {
                f = 1.0F;
            }
            this.field[m] = f;
        }
        return f;
    }

    private Mesh extract() {
        SurfaceNets.Extraction extraction = new SurfaceNets.Extraction();
        for (int i = 0; i < this.bz; i++) {
            for (int j = 0; j < this.by; j++) {
                for (int k = 0; k < this.bx; k++) {
                    if (this.active[(i * this.by + j) * this.bx + k]) {
                        extraction.block(k * 4, j * 4, i * 4);
                    }
                }
            }
        }
        return new Mesh(
                Arrays.copyOf(extraction.pos, extraction.verts * 3),
                extraction.verts,
                Arrays.copyOf(extraction.tris, extraction.triCount * 3),
                extraction.triCount);
    }

    @Environment(EnvType.CLIENT)
    private final class Extraction {

        private final int[] cells;

        private final float[] corner = new float[8];

        private float[] pos = new float[12288];

        private int verts;

        private int[] tris = new int[24576];

        private int triCount;

        Extraction() {
            this.cells = new int[SurfaceNets.this.nx * SurfaceNets.this.ny * SurfaceNets.this.nz];
            Arrays.fill(this.cells, -1);
        }

        void block(int i, int j, int k) {
            int m = Math.min(i + 4, SurfaceNets.this.nx);
            int n = Math.min(j + 4, SurfaceNets.this.ny);
            int o = Math.min(k + 4, SurfaceNets.this.nz);
            for (int p = k; p < o; p++) {
                for (int q = j; q < n; q++) {
                    for (int r = i; r < m; r++) {
                        boolean bl = SurfaceNets.this.value(r, q, p) < 0.0F;
                        if (r + 1 < SurfaceNets.this.nx
                                && q >= 1
                                && p >= 1
                                && q < SurfaceNets.this.ny - 1
                                && p < SurfaceNets.this.nz - 1
                                && SurfaceNets.this.value(r + 1, q, p) < 0.0F != bl) {
                            this.quad(
                                    this.cell(r, q - 1, p - 1),
                                    this.cell(r, q, p - 1),
                                    this.cell(r, q, p),
                                    this.cell(r, q - 1, p),
                                    bl);
                        }
                        if (q + 1 < SurfaceNets.this.ny
                                && r >= 1
                                && p >= 1
                                && r < SurfaceNets.this.nx - 1
                                && p < SurfaceNets.this.nz - 1
                                && SurfaceNets.this.value(r, q + 1, p) < 0.0F != bl) {
                            this.quad(
                                    this.cell(r - 1, q, p - 1),
                                    this.cell(r - 1, q, p),
                                    this.cell(r, q, p),
                                    this.cell(r, q, p - 1),
                                    bl);
                        }
                        if (p + 1 < SurfaceNets.this.nz
                                && r >= 1
                                && q >= 1
                                && r < SurfaceNets.this.nx - 1
                                && q < SurfaceNets.this.ny - 1
                                && SurfaceNets.this.value(r, q, p + 1) < 0.0F != bl) {
                            this.quad(
                                    this.cell(r - 1, q - 1, p),
                                    this.cell(r, q - 1, p),
                                    this.cell(r, q, p),
                                    this.cell(r - 1, q, p),
                                    bl);
                        }
                    }
                }
            }
        }

        private int cell(int i, int j, int k) {
            int m = (k * SurfaceNets.this.ny + j) * SurfaceNets.this.nx + i;
            int cell = this.cells[m];
            if (cell >= 0) {
                return cell;
            } else {
                float[] corner2 = this.corner;
                for (int o = 0; o < 8; o++) {
                    corner2[o] =
                            SurfaceNets.this.value(i + (o & 1), j + (o >> 1 & 1), k + (o >> 2 & 1));
                }
                float f = 0.0F;
                float g = 0.0F;
                float f2 = 0.0F;
                int n = 0;
                for (int q = 0; q < 12; q++) {
                    int p = SurfaceNets.EDGE_A[q];
                    float g2 = corner2[p];
                    int r;
                    float h2;
                    if (g2 < 0.0F != (h2 = corner2[r = SurfaceNets.EDGE_B[q]]) < 0.0F) {
                        float f3 = g2 / (g2 - h2);
                        float g3 = p & 1;
                        float h3 = p >> 1 & 1;
                        float f4 = p >> 2 & 1;
                        float g4 = r & 1;
                        float h4 = r >> 1 & 1;
                        float f5 = r >> 2 & 1;
                        f += g3 + (g4 - g3) * f3;
                        g += h3 + (h4 - h3) * f3;
                        f2 += f4 + (f5 - f4) * f3;
                        n++;
                    }
                }
                float g5 = n > 0 ? f / n : 0.5F;
                float h5 = n > 0 ? g / n : 0.5F;
                float f6 = n > 0 ? f2 / n : 0.5F;
                if (this.verts * 3 + 3 > this.pos.length) {
                    this.pos = Arrays.copyOf(this.pos, this.pos.length * 2);
                }
                this.pos[this.verts * 3] = SurfaceNets.this.ox + (i + g5) * SurfaceNets.this.h;
                this.pos[this.verts * 3 + 1] = SurfaceNets.this.oy + (j + h5) * SurfaceNets.this.h;
                this.pos[this.verts * 3 + 2] = SurfaceNets.this.oz + (k + f6) * SurfaceNets.this.h;
                this.cells[m] = this.verts;
                return this.verts++;
            }
        }

        private void quad(int i, int j, int k, int m, boolean bl) {
            if (!bl) {
                int n = j;
                j = m;
                m = n;
            }
            if (this.dist2(i, k) <= this.dist2(j, m)) {
                this.tri(i, j, k);
                this.tri(i, k, m);
            } else {
                this.tri(i, j, m);
                this.tri(j, k, m);
            }
        }

        private float dist2(int i, int j) {
            float f = this.pos[i * 3] - this.pos[j * 3];
            float g = this.pos[i * 3 + 1] - this.pos[j * 3 + 1];
            float f2 = this.pos[i * 3 + 2] - this.pos[j * 3 + 2];
            return f * f + g * g + f2 * f2;
        }

        private void tri(int i, int j, int k) {
            if (i != j && j != k && i != k) {
                if (this.triCount * 3 + 3 > this.tris.length) {
                    this.tris = Arrays.copyOf(this.tris, this.tris.length * 2);
                }
                this.tris[this.triCount * 3] = i;
                this.tris[this.triCount * 3 + 1] = j;
                this.tris[this.triCount * 3 + 2] = k;
                this.triCount++;
            }
        }
    }
}
