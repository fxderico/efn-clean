package net.efn.client.util.glyph;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class GlyphSwarm {

    public static final byte BOLT = 0;

    public static final byte GEM = 1;

    public static final byte GRAIN = 2;

    public static final byte SPARK = 3;

    public static final int WORST_QUADS =
            (Math.max(
                                    Math.max(GlyphShapes.BOLT.length, GlyphShapes.GEM.length),
                                    Math.max(
                                            GlyphShapes.SPARK.length,
                                            Math.max(
                                                    GlyphShapes.GRAIN.length,
                                                    GlyphShapes.DIAMOND.length)))
                            + GlyphShapes.OCTAGON.length)
                    / 8;

    private static final double CULL_DISTANCE_SQR = 9216.0;

    private static final double DETAIL_DISTANCE_SQR = 484.0;

    private static final float HALO_SCALE = 2.3F;

    private static final float HALO_ALPHA = 0.17F;

    private final int capacity;

    private final double[] x;

    private final double[] y;

    private final double[] z;

    private final float[] vx;

    private final float[] vy;

    private final float[] vz;

    private final float[] age;

    private final float[] life;

    private final float[] size;

    private final float[] rot;

    private final float[] spin;

    private final float[] seed;

    private final byte[] kind;

    private final int[] color;

    private int count;

    public GlyphSwarm(int capacity2) {
        this.capacity = capacity2;
        this.x = new double[capacity2];
        this.y = new double[capacity2];
        this.z = new double[capacity2];
        this.vx = new float[capacity2];
        this.vy = new float[capacity2];
        this.vz = new float[capacity2];
        this.age = new float[capacity2];
        this.life = new float[capacity2];
        this.size = new float[capacity2];
        this.rot = new float[capacity2];
        this.spin = new float[capacity2];
        this.seed = new float[capacity2];
        this.kind = new byte[capacity2];
        this.color = new int[capacity2];
    }

    public int count() {
        return this.count;
    }

    public void clear() {
        this.count = 0;
    }

    public void spawn(
            byte b,
            double d,
            double e,
            double d2,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3,
            int i,
            int j) {
        int k = Math.min(this.capacity, Math.max(1, j));
        int m = this.count < k ? this.count++ : this.oldest();
        this.x[m] = d;
        this.y[m] = e;
        this.z[m] = d2;
        this.vx[m] = f;
        this.vy[m] = g;
        this.vz[m] = h;
        this.age[m] = 0.0F;
        this.life[m] = Math.max(0.05F, f2);
        this.size[m] = g2;
        this.rot[m] = h2;
        this.spin[m] = f3;
        this.seed[m] = g3;
        this.kind[m] = b;
        this.color[m] = i;
    }

    public void trim(int i) {
        while (this.count > Math.max(0, i)) {
            this.remove(this.oldest());
        }
    }

    public void step(float f) {
        if (!(f <= 0.0F)) {
            for (int i = this.count - 1; i >= 0; i--) {
                float g = this.age[i] + f;
                if (g >= this.life[i]) {
                    this.remove(i);
                } else {
                    this.age[i] = g;
                    float h;
                    float f2;
                    switch (this.kind[i]) {
                        case 0:
                            f2 = 1.6F;
                            h = 0.7F;
                            this.vx[i] =
                                    this.vx[i]
                                            + MathHelper.sin(g * 2.3F + this.seed[i] * 6.0F)
                                                    * 0.35F
                                                    * f;
                            this.vz[i] =
                                    this.vz[i]
                                            + MathHelper.cos(g * 1.9F + this.seed[i] * 6.0F)
                                                    * 0.35F
                                                    * f;
                            break;
                        case 1:
                            f2 = 0.8F;
                            h = -1.1F;
                            float g2 = (this.seed[i] > 0.5F ? 1.8F : -1.8F) * f;
                            float h2 = MathHelper.cos(g2);
                            float f3 = MathHelper.sin(g2);
                            float g3 = this.vx[i] * h2 - this.vz[i] * f3;
                            this.vz[i] = this.vx[i] * f3 + this.vz[i] * h2;
                            this.vx[i] = g3;
                            break;
                        case 2:
                            f2 = 0.9F;
                            h = -6.5F;
                            break;
                        default:
                            f2 = 3.4F;
                            h = -1.6F;
                    }
                    float h3 = Math.max(0.0F, 1.0F - f2 * f);
                    this.vx[i] = this.vx[i] * h3;
                    this.vy[i] = this.vy[i] * h3 + h * f;
                    this.vz[i] = this.vz[i] * h3;
                    this.x[i] = this.x[i] + this.vx[i] * f;
                    this.y[i] = this.y[i] + this.vy[i] * f;
                    this.z[i] = this.z[i] + this.vz[i] * f;
                    this.rot[i] = this.rot[i] + this.spin[i] * f;
                }
            }
        }
    }

    public void draw(GlyphCanvas glyphCanvas, float f, boolean bl) {
        for (int i = 0; i < this.count; i++) {
            double d = this.x[i];
            double e = this.y[i];
            double d2 = this.z[i];
            double distanceSqr;
            if (!(glyphCanvas.depth(d, e, d2) < 0.05)
                    && !((distanceSqr = glyphCanvas.distanceSqr(d, e, d2)) > 9216.0)) {
                float g = this.age[i] / this.life[i];
                float h = Math.min(1.0F, g * 9.0F) * (1.0F - g * g);
                float f2 = this.size[i];
                float g2 = 1.0F;
                float[] bOLT;
                switch (this.kind[i]) {
                    case 0:
                        bOLT = GlyphShapes.BOLT;
                        f2 *= bloom(Math.min(1.0F, g * 5.0F)) * (1.0F - 0.35F * g);
                        break;
                    case 1:
                        bOLT = GlyphShapes.GEM;
                        g2 =
                                0.72F
                                        + 0.28F
                                                * Math.abs(
                                                        MathHelper.sin(
                                                                this.age[i] * 15.0F
                                                                        + this.seed[i] * 9.0F));
                        break;
                    case 2:
                        bOLT = GlyphShapes.GRAIN;
                        break;
                    default:
                        bOLT = GlyphShapes.SPARK;
                        f2 *= 1.0F - 0.6F * g;
                        h *=
                                0.75F
                                        + 0.25F
                                                * MathHelper.sin(
                                                        this.age[i] * 23.0F + this.seed[i] * 12.0F);
                }
                float h2 = h * f;
                if (!(h2 < 0.012F)) {
                    int j = this.color[i];
                    boolean bl2 = distanceSqr < 484.0;
                    if (bl && bl2) {
                        glyphCanvas.sprite(
                                GlyphShapes.OCTAGON,
                                d,
                                e,
                                d2,
                                f2 * 2.3F,
                                0.0F,
                                GlyphTint.alpha(j, h2 * 0.17F));
                    }
                    if (!bl2 && bOLT != GlyphShapes.GRAIN) {
                        bOLT = GlyphShapes.DIAMOND;
                    }
                    glyphCanvas.sprite(
                            bOLT, d, e, d2, f2 * g2, f2, this.rot[i], GlyphTint.alpha(j, h2));
                }
            }
        }
    }

    private static float bloom(float f) {
        float g = f - 1.0F;
        return 1.0F + 2.2F * g * g * g + 1.2F * g * g;
    }

    private int oldest() {
        int i = 0;
        float f = -1.0F;
        for (int j = 0; j < this.count; j++) {
            float g = this.age[j] / this.life[j];
            if (g > f) {
                f = g;
                i = j;
            }
        }
        return i;
    }

    private void remove(int i) {
        int j;
        if (i != (j = --this.count)) {
            this.x[i] = this.x[j];
            this.y[i] = this.y[j];
            this.z[i] = this.z[j];
            this.vx[i] = this.vx[j];
            this.vy[i] = this.vy[j];
            this.vz[i] = this.vz[j];
            this.age[i] = this.age[j];
            this.life[i] = this.life[j];
            this.size[i] = this.size[j];
            this.rot[i] = this.rot[j];
            this.spin[i] = this.spin[j];
            this.seed[i] = this.seed[j];
            this.kind[i] = this.kind[j];
            this.color[i] = this.color[j];
        }
    }
}
