package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Shape2D {

    static final float FAR = 100000.0F;

    float minX = -100000.0F;

    float minY = -100000.0F;

    float maxX = 100000.0F;

    float maxY = 100000.0F;

    protected Shape2D() {}

    public abstract float distance(float f, float g);

    protected final void bounds(float f, float g, float h, float f2) {
        this.minX = Math.min(f, h);
        this.minY = Math.min(g, f2);
        this.maxX = Math.max(f, h);
        this.maxY = Math.max(g, f2);
    }

    public final float minX() {
        return this.minX;
    }

    public final float minY() {
        return this.minY;
    }

    public final float maxX() {
        return this.maxX;
    }

    public final float maxY() {
        return this.maxY;
    }

    public static Shape2D circle(float f, float g, float h) {
        return new Shape2D.Circle(f, g, h);
    }

    public static Shape2D ellipse(float f, float g, float h, float f2) {
        return new Shape2D.Ellipse(f, g, h, f2);
    }

    public static Shape2D box(float f, float g, float h, float f2, float g2) {
        return new Shape2D.Box(f, g, h, f2, g2);
    }

    public static Shape2D line(float f, float g, float h, float f2, float g2) {
        return new Shape2D.Taper(f, g, g2 * 0.5F, h, f2, g2 * 0.5F);
    }

    public static Shape2D taper(float f, float g, float h, float f2, float g2, float h2) {
        return new Shape2D.Taper(f, g, h * 0.5F, f2, g2, h2 * 0.5F);
    }

    public static Shape2D curve(
            float f, float g, float h, float f2, float g2, float h2, float f3, float g3) {
        return new Shape2D.Path(new float[] {f, g, h, f2, g2, h2}, f3 * 0.5F, g3 * 0.5F, true);
    }

    public static Shape2D path(float f, float g, float... h) {
        return new Shape2D.Path(h, f * 0.5F, g * 0.5F, false);
    }

    public static Shape2D polygon(float... f) {
        return new Shape2D.Polygon(f);
    }

    public static Shape2D arc(float f, float g, float h, float f2, float g2, float h2) {
        int i = Math.max(3, (int) Math.ceil(Math.abs(g2 - f2) / 20.0F) + 1);
        float[] floats = new float[(i + 1) * 2];
        for (int o = 0; o <= i; o++) {
            double d = Math.toRadians(f2 + (g2 - f2) * o / i);
            floats[o * 2] = f + h * (float) Math.cos(d);
            floats[o * 2 + 1] = g + h * (float) Math.sin(d);
        }
        return new Shape2D.Path(floats, h2 * 0.5F, h2 * 0.5F, false);
    }

    public static Shape2D star(float f, float g, float h, float f2, int i) {
        float[] floats = new float[i * 4];
        for (int m = 0; m < i * 2; m++) {
            double d = 3.141592653589793 * m / i - 1.5707963267948966;
            float g2 = (m & 1) == 0 ? h : f2;
            floats[m * 2] = f + g2 * (float) Math.cos(d);
            floats[m * 2 + 1] = g + g2 * (float) Math.sin(d);
        }
        return new Shape2D.Polygon(floats);
    }

    public static Shape2D field(Shape2D.Field field2, float f, float g, float h, float f2) {
        return new Shape2D.Custom(field2, f, g, h, f2);
    }

    public static Shape2D union(Shape2D... shape2D) {
        return (Shape2D) (shape2D.length == 1 ? shape2D[0] : new Shape2D.Union(0.0F, shape2D));
    }

    public static Shape2D blend(float f, Shape2D... shape2D) {
        return (Shape2D) (shape2D.length == 1 ? shape2D[0] : new Shape2D.Union(f, shape2D));
    }

    public Shape2D union(Shape2D shape2D) {
        return new Shape2D.Union(0.0F, new Shape2D[] {this, shape2D});
    }

    public Shape2D blend(float f, Shape2D shape2D) {
        return new Shape2D.Union(f, new Shape2D[] {this, shape2D});
    }

    public Shape2D subtract(Shape2D shape2D) {
        return new Shape2D.Op(this, shape2D, 1, 0.0F);
    }

    public Shape2D subtract(float f, Shape2D shape2D) {
        return new Shape2D.Op(this, shape2D, 1, f);
    }

    public Shape2D intersect(Shape2D shape2D) {
        return new Shape2D.Op(this, shape2D, 2, 0.0F);
    }

    public Shape2D intersect(float f, Shape2D shape2D) {
        return new Shape2D.Op(this, shape2D, 2, f);
    }

    public Shape2D inflate(float f) {
        return new Shape2D.Offset(this, f, false);
    }

    public Shape2D outline(float f) {
        return new Shape2D.Offset(this, f * 0.5F, true);
    }

    public Shape2D move(float f, float g) {
        return new Shape2D.Transform(this, 1.0F, 0.0F, f, g, 0.0F, 0.0F, 1.0F);
    }

    public Shape2D rotate(float f, float g, float h) {
        double d = Math.toRadians(f);
        return new Shape2D.Transform(
                this, (float) Math.cos(d), (float) Math.sin(d), 0.0F, 0.0F, g, h, 1.0F);
    }

    public Shape2D scale(float f, float g, float h) {
        return new Shape2D.Transform(this, 1.0F, 0.0F, 0.0F, 0.0F, g, h, f);
    }

    public Shape2D mirrorX(float f) {
        return new Shape2D.Flip(this, f);
    }

    static float clamp01(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }

    @Environment(EnvType.CLIENT)
    static final class Box extends Shape2D {

        private final float cx;

        private final float cy;

        private final float hx;

        private final float hy;

        private final float r;

        Box(float cx2, float cy2, float f, float g, float h) {
            this.cx = cx2;
            this.cy = cy2;
            this.r = Math.max(0.0F, Math.min(h, Math.min(f, g)));
            this.hx = f - this.r;
            this.hy = g - this.r;
            this.bounds(cx2 - f, cy2 - g, cx2 + f, cy2 + g);
        }

        @Override
        public float distance(float f, float g) {
            float h = Math.abs(f - this.cx) - this.hx;
            float f2 = Math.abs(g - this.cy) - this.hy;
            float g2 = Math.max(h, 0.0F);
            float h2 = Math.max(f2, 0.0F);
            return (float) Math.sqrt(g2 * g2 + h2 * h2) + Math.min(Math.max(h, f2), 0.0F) - this.r;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Circle extends Shape2D {

        private final float cx;

        private final float cy;

        private final float r;

        Circle(float cx2, float cy2, float f) {
            this.cx = cx2;
            this.cy = cy2;
            this.r = f;
            this.bounds(cx2 - f, cy2 - f, cx2 + f, cy2 + f);
        }

        @Override
        public float distance(float f, float g) {
            float h = f - this.cx;
            float f2 = g - this.cy;
            return (float) Math.sqrt(h * h + f2 * f2) - this.r;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Custom extends Shape2D {

        private final Shape2D.Field field;

        Custom(Shape2D.Field field2, float f, float g, float h, float f2) {
            this.field = field2;
            this.bounds(f, g, h, f2);
        }

        @Override
        public float distance(float f, float g) {
            return this.field.distance(f, g);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Ellipse extends Shape2D {

        private final float cx;

        private final float cy;

        private final float ix;

        private final float iy;

        private final float iix;

        private final float iiy;

        private final float rmin;

        Ellipse(float cx2, float cy2, float f, float g) {
            this.cx = cx2;
            this.cy = cy2;
            this.ix = 1.0F / f;
            this.iy = 1.0F / g;
            this.iix = this.ix * this.ix;
            this.iiy = this.iy * this.iy;
            this.rmin = Math.min(f, g);
            this.bounds(cx2 - f, cy2 - g, cx2 + f, cy2 + g);
        }

        @Override
        public float distance(float f, float g) {
            float h = f - this.cx;
            float f2 = g - this.cy;
            float g2 = h * this.ix;
            float h2 = f2 * this.iy;
            float f3 = (float) Math.sqrt(g2 * g2 + h2 * h2);
            float g3 = h * this.iix;
            float h3 = f2 * this.iiy;
            float f4 = (float) Math.sqrt(g3 * g3 + h3 * h3);
            return f4 < 1.0E-9F ? -this.rmin : f3 * (f3 - 1.0F) / f4;
        }
    }

    @Environment(EnvType.CLIENT)
    public interface Field {

        float distance(float f, float g);
    }

    @Environment(EnvType.CLIENT)
    static final class Flip extends Shape2D {

        private final Shape2D child;

        private final float axis;

        Flip(Shape2D child2, float axis2) {
            this.child = child2;
            this.axis = axis2;
            this.bounds(
                    2.0F * axis2 - child2.maxX,
                    child2.minY,
                    2.0F * axis2 - child2.minX,
                    child2.maxY);
        }

        @Override
        public float distance(float f, float g) {
            return this.child.distance(2.0F * this.axis - f, g);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Heart extends Shape2D {

        private final float cx;

        private final float cy;

        private final float size;

        Heart(float cx2, float cy2, float size2) {
            this.cx = cx2;
            this.cy = cy2;
            this.size = size2;
            this.bounds(
                    cx2 - size2 * 0.7F,
                    cy2 - size2 * 0.35F,
                    cx2 + size2 * 0.7F,
                    cy2 + size2 * 0.8F);
        }

        @Override
        public float distance(float f, float g) {
            float h = Math.abs(f - this.cx) / this.size;
            float f2;
            if (h + (f2 = (this.cy + this.size * 0.75F - g) / this.size) > 1.0F) {
                float g2 = h - 0.25F;
                float h2 = f2 - 0.75F;
                return ((float) Math.sqrt(g2 * g2 + h2 * h2) - 0.35355338F) * this.size;
            } else {
                float f3 = f2 - 1.0F;
                float g3 = Math.max(h + f2, 0.0F) * 0.5F;
                float h3 = h - g3;
                float f4 = f2 - g3;
                return (float) Math.sqrt(Math.min(h * h + f3 * f3, h3 * h3 + f4 * f4))
                        * Math.signum(h - f2)
                        * this.size;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Offset extends Shape2D {

        private final Shape2D child;

        private final float r;

        private final boolean ring;

        Offset(Shape2D child2, float f, boolean ring2) {
            this.child = child2;
            this.r = f;
            this.ring = ring2;
            float g = Math.abs(f);
            this.bounds(child2.minX - g, child2.minY - g, child2.maxX + g, child2.maxY + g);
        }

        @Override
        public float distance(float f, float g) {
            float distance = this.child.distance(f, g);
            return this.ring ? Math.abs(distance) - this.r : distance - this.r;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Op extends Shape2D {

        private final Shape2D a;

        private final Shape2D b;

        private final int mode;

        private final float k;

        Op(Shape2D shape2D, Shape2D shape2D2, int mode2, float f) {
            this.a = shape2D;
            this.b = shape2D2;
            this.mode = mode2;
            this.k = Math.max(0.0F, f);
            if (mode2 == 2) {
                this.bounds(
                        Math.max(shape2D.minX, shape2D2.minX),
                        Math.max(shape2D.minY, shape2D2.minY),
                        Math.min(shape2D.maxX, shape2D2.maxX),
                        Math.min(shape2D.maxY, shape2D2.maxY));
            } else {
                this.bounds(shape2D.minX, shape2D.minY, shape2D.maxX, shape2D.maxY);
            }
        }

        @Override
        public float distance(float f, float g) {
            float distance = this.a.distance(f, g);
            float distance2 = this.b.distance(f, g);
            if (this.mode == 1) {
                if (this.k <= 0.0F) {
                    return Math.max(distance, -distance2);
                } else {
                    float h = clamp01(0.5F - 0.5F * (distance + distance2) / this.k);
                    return distance + (-distance2 - distance) * h + this.k * h * (1.0F - h);
                }
            } else if (this.k <= 0.0F) {
                return Math.max(distance, distance2);
            } else {
                float f2 = clamp01(0.5F - 0.5F * (distance2 - distance) / this.k);
                return distance2 + (distance - distance2) * f2 + this.k * f2 * (1.0F - f2);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Path extends Shape2D {

        private final float[] seg;

        private final int count;

        Path(float[] floats, float f, float g, boolean bl) {
            if (floats.length >= 4 && floats.length % 2 == 0) {
                int i = floats.length / 2;
                float[] floats2 = new float[i];
                for (int j = 1; j < i; j++) {
                    float h = floats[j * 2] - floats[j * 2 - 2];
                    float f2 = floats[j * 2 + 1] - floats[j * 2 - 1];
                    floats2[j] = floats2[j - 1] + (float) Math.sqrt(h * h + f2 * f2);
                }
                float g2 = Math.max(1.0E-6F, floats2[i - 1]);
                float[] floats3 = new float[i];
                for (int m = 0; m < i; m++) {
                    floats3[m] = f + (g - f) * floats2[m] / g2;
                }
                if (!bl && i != 2) {
                    this.count = i - 2;
                    this.seg = new float[this.count * 12];
                    for (int o = 1; o <= this.count; o++) {
                        float h2 = o == 1 ? floats[0] : (floats[o * 2 - 2] + floats[o * 2]) * 0.5F;
                        float f3 =
                                o == 1 ? floats[1] : (floats[o * 2 - 1] + floats[o * 2 + 1]) * 0.5F;
                        float g3 = o == 1 ? floats3[0] : (floats3[o - 1] + floats3[o]) * 0.5F;
                        float h3 =
                                o == this.count
                                        ? floats[o * 2 + 2]
                                        : (floats[o * 2] + floats[o * 2 + 2]) * 0.5F;
                        float f4 =
                                o == this.count
                                        ? floats[o * 2 + 3]
                                        : (floats[o * 2 + 1] + floats[o * 2 + 3]) * 0.5F;
                        float g4 =
                                o == this.count
                                        ? floats3[o + 1]
                                        : (floats3[o] + floats3[o + 1]) * 0.5F;
                        fill(
                                this.seg,
                                (o - 1) * 12,
                                h2,
                                f3,
                                g3,
                                floats[o * 2],
                                floats[o * 2 + 1],
                                floats3[o],
                                h3,
                                f4,
                                g4);
                    }
                } else {
                    this.count = 1;
                    this.seg = new float[12];
                    if (i == 2) {
                        fill(
                                this.seg,
                                0,
                                floats[0],
                                floats[1],
                                floats3[0],
                                (floats[0] + floats[2]) * 0.5F,
                                (floats[1] + floats[3]) * 0.5F,
                                (floats3[0] + floats3[1]) * 0.5F,
                                floats[2],
                                floats[3],
                                floats3[1]);
                    } else {
                        fill(
                                this.seg,
                                0,
                                floats[0],
                                floats[1],
                                f,
                                floats[2],
                                floats[3],
                                (f + g) * 0.5F,
                                floats[4],
                                floats[5],
                                g);
                    }
                }
                float h4 = 100000.0F;
                float f5 = 100000.0F;
                float g5 = -100000.0F;
                float h5 = -100000.0F;
                for (int p = 0; p < this.count; p++) {
                    int q = p * 12;
                    h4 = Math.min(h4, this.seg[q + 8]);
                    f5 = Math.min(f5, this.seg[q + 9]);
                    g5 = Math.max(g5, this.seg[q + 10]);
                    h5 = Math.max(h5, this.seg[q + 11]);
                }
                this.bounds(h4, f5, g5, h5);
            } else {
                throw new IllegalArgumentException(
                        "path needs at least two points given as x,y pairs");
            }
        }

        private static void fill(
                float[] floats,
                int i,
                float f,
                float g,
                float h,
                float f2,
                float g2,
                float h2,
                float f3,
                float g3,
                float h3) {
            floats[i] = f;
            floats[i + 1] = g;
            floats[i + 2] = f2;
            floats[i + 3] = g2;
            floats[i + 4] = f3;
            floats[i + 5] = g3;
            floats[i + 6] = h;
            floats[i + 7] = h3;
            float f4 = Math.max(h, Math.max(h2, h3));
            floats[i + 8] = Math.min(f, Math.min(f2, f3)) - f4;
            floats[i + 9] = Math.min(g, Math.min(g2, g3)) - f4;
            floats[i + 10] = Math.max(f, Math.max(f2, f3)) + f4;
            floats[i + 11] = Math.max(g, Math.max(g2, g3)) + f4;
        }

        @Override
        public float distance(float f, float g) {
            float h = 3.4028235E38F;
            for (int i = 0; i < this.count; i++) {
                int j = i * 12;
                float f2 =
                        f < this.seg[j + 8]
                                ? this.seg[j + 8] - f
                                : (f > this.seg[j + 10] ? f - this.seg[j + 10] : 0.0F);
                float g2 =
                        g < this.seg[j + 9]
                                ? this.seg[j + 9] - g
                                : (g > this.seg[j + 11] ? g - this.seg[j + 11] : 0.0F);
                float segment;
                if ((!(h > 0.0F) || h == 3.4028235E38F || !(f2 * f2 + g2 * g2 >= h * h))
                        && (segment = this.segment(j, f, g)) < h) {
                    h = segment;
                }
            }
            return h;
        }

        private float segment(int i, float f, float g) {
            float h = this.seg[i];
            float f2 = this.seg[i + 1];
            float g2 = this.seg[i + 2];
            float h2 = this.seg[i + 3];
            float f3 = this.seg[i + 4];
            float g3 = this.seg[i + 5];
            float h3 = g2 - h;
            float f4 = h2 - f2;
            float g4 = h - 2.0F * g2 + f3;
            float h4 = f2 - 2.0F * h2 + g3;
            float f5 = g4 * g4 + h4 * h4;
            float g5 = h - f;
            float h5 = f2 - g;
            float f6 = f3 - h;
            float g6 = g3 - f2;
            float h6 = f6 * f6 + g6 * g6;
            float f7;
            if (f5 < 1.0E-5F * Math.max(h6, 1.0E-6F)) {
                f7 = h6 > 1.0E-12F ? clamp01(((f - h) * f6 + (g - f2) * g6) / h6) : 0.0F;
            } else {
                float g7 = 1.0F / f5;
                float h7 = g7 * (h3 * g4 + f4 * h4);
                float f8 = g7 * (2.0F * (h3 * h3 + f4 * f4) + g5 * g4 + h5 * h4) / 3.0F;
                float g8 = g7 * (g5 * h3 + h5 * f4);
                float h8 = h7 * (2.0F * h7 * h7 - 3.0F * f8) + g8;
                float f9 = f8 - h7 * h7;
                float g9 = f9 * f9 * f9;
                float h9 = h8 * h8 + 4.0F * g9;
                if (h9 >= 0.0F) {
                    h9 = (float) Math.sqrt(h9);
                    f7 =
                            clamp01(
                                    (float)
                                                    (Math.cbrt((h9 - h8) * 0.5F)
                                                            + Math.cbrt((-h9 - h8) * 0.5F))
                                            - h7);
                } else {
                    float f10 = (float) Math.sqrt(-f9);
                    float g10 = h8 / (f9 * f10 * 2.0F);
                    g10 = g10 < -1.0F ? -1.0F : Math.min(g10, 1.0F);
                    float h10 = (float) Math.acos(g10) / 3.0F;
                    float f11 = (float) Math.cos(h10);
                    float g11 = (float) Math.sin(h10) * 1.7320508F;
                    float h11 = clamp01((f11 + f11) * f10 - h7);
                    float f12 = clamp01((-g11 - f11) * f10 - h7);
                    float g12 = g5 + (2.0F * h3 + g4 * h11) * h11;
                    float h12 = h5 + (2.0F * f4 + h4 * h11) * h11;
                    float f13 = g5 + (2.0F * h3 + g4 * f12) * f12;
                    float g13 = h5 + (2.0F * f4 + h4 * f12) * f12;
                    f7 = g12 * g12 + h12 * h12 <= f13 * f13 + g13 * g13 ? h11 : f12;
                }
            }
            float h13 = g5 + (2.0F * h3 + g4 * f7) * f7;
            float f14 = h5 + (2.0F * f4 + h4 * f7) * f7;
            float g14 = this.seg[i + 6] + (this.seg[i + 7] - this.seg[i + 6]) * f7;
            return (float) Math.sqrt(h13 * h13 + f14 * f14) - g14;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Polygon extends Shape2D {

        private final float[] v;

        private final int n;

        Polygon(float[] floats) {
            if (floats.length >= 6 && floats.length % 2 == 0) {
                this.v = (float[]) floats.clone();
                this.n = floats.length / 2;
                float f = 100000.0F;
                float g = 100000.0F;
                float h = -100000.0F;
                float f2 = -100000.0F;
                for (int i = 0; i < this.n; i++) {
                    f = Math.min(f, this.v[i * 2]);
                    g = Math.min(g, this.v[i * 2 + 1]);
                    h = Math.max(h, this.v[i * 2]);
                    f2 = Math.max(f2, this.v[i * 2 + 1]);
                }
                this.bounds(f, g, h, f2);
            } else {
                throw new IllegalArgumentException(
                        "polygon needs at least three points given as x,y pairs");
            }
        }

        @Override
        public float distance(float f, float g) {
            float h = f - this.v[0];
            float f2 = g - this.v[1];
            float g2 = h * h + f2 * f2;
            float h2 = 1.0F;
            int i = 0;
            for (int j = this.n - 1; i < this.n; j = i++) {
                float f3 = this.v[j * 2] - this.v[i * 2];
                float g3 = this.v[j * 2 + 1] - this.v[i * 2 + 1];
                float h3 = f - this.v[i * 2];
                float f4 = g - this.v[i * 2 + 1];
                float g4 = f3 * f3 + g3 * g3;
                float h4 = g4 > 1.0E-12F ? clamp01((h3 * f3 + f4 * g3) / g4) : 0.0F;
                float f5 = h3 - f3 * h4;
                float g5 = f4 - g3 * h4;
                g2 = Math.min(g2, f5 * f5 + g5 * g5);
                boolean bl = g >= this.v[i * 2 + 1];
                boolean bl2 = g < this.v[j * 2 + 1];
                boolean bl3 = f3 * f4 > g3 * h3;
                if (bl && bl2 && bl3 || !bl && !bl2 && !bl3) {
                    h2 = -h2;
                }
            }
            return h2 * (float) Math.sqrt(g2);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Taper extends Shape2D {

        private final float ax;

        private final float ay;

        private final float bax;

        private final float bay;

        private final float ra;

        private final float rb;

        private final float l2;

        private final float rr;

        private final float a2;

        private final float il2;

        private final boolean circle;

        private final float sx;

        private final float sy;

        private final float sr;

        Taper(float ax2, float ay2, float ra2, float f, float g, float rb2) {
            this.ax = ax2;
            this.ay = ay2;
            this.ra = ra2;
            this.rb = rb2;
            this.bax = f - ax2;
            this.bay = g - ay2;
            this.l2 = this.bax * this.bax + this.bay * this.bay;
            this.rr = ra2 - rb2;
            this.a2 = this.l2 - this.rr * this.rr;
            this.il2 = this.l2 > 1.0E-12F ? 1.0F / this.l2 : 0.0F;
            this.circle = this.a2 <= 1.0E-6F;
            boolean bl = ra2 >= rb2;
            this.sx = bl ? ax2 : f;
            this.sy = bl ? ay2 : g;
            this.sr = Math.max(ra2, rb2);
            this.bounds(
                    Math.min(ax2 - ra2, f - rb2),
                    Math.min(ay2 - ra2, g - rb2),
                    Math.max(ax2 + ra2, f + rb2),
                    Math.max(ay2 + ra2, g + rb2));
        }

        @Override
        public float distance(float f, float g) {
            if (this.circle) {
                float h = f - this.sx;
                float f2 = g - this.sy;
                return (float) Math.sqrt(h * h + f2 * f2) - this.sr;
            } else {
                float g2 = f - this.ax;
                float h2 = g - this.ay;
                float f3 = g2 * this.bax + h2 * this.bay;
                float g3 = f3 - this.l2;
                float h3 = g2 * this.l2 - this.bax * f3;
                float f4 = h2 * this.l2 - this.bay * f3;
                float g4 = h3 * h3 + f4 * f4;
                float h4 = f3 * f3 * this.l2;
                float f5 = g3 * g3 * this.l2;
                float g5 = Math.signum(this.rr) * this.rr * this.rr * g4;
                if (Math.signum(g3) * this.a2 * f5 > g5) {
                    return (float) Math.sqrt(g4 + f5) * this.il2 - this.rb;
                } else {
                    return Math.signum(f3) * this.a2 * h4 < g5
                            ? (float) Math.sqrt(g4 + h4) * this.il2 - this.ra
                            : ((float) Math.sqrt(g4 * this.a2 * this.il2) + f3 * this.rr) * this.il2
                                    - this.ra;
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Transform extends Shape2D {

        private final Shape2D child;

        private final float c;

        private final float s;

        private final float dx;

        private final float dy;

        private final float cx;

        private final float cy;

        private final float k;

        Transform(
                Shape2D child2,
                float f,
                float g,
                float dx2,
                float dy2,
                float cx2,
                float cy2,
                float h) {
            this.child = child2;
            this.c = f;
            this.s = g;
            this.dx = dx2;
            this.dy = dy2;
            this.cx = cx2;
            this.cy = cy2;
            this.k = h;
            float f2 = 100000.0F;
            float g2 = 100000.0F;
            float h2 = -100000.0F;
            float f3 = -100000.0F;
            for (int m = 0; m < 4; m++) {
                float g3 = ((m & 1) == 0 ? child2.minX : child2.maxX) - cx2;
                float h3 = ((m & 2) == 0 ? child2.minY : child2.maxY) - cy2;
                float f4 = (f * g3 - g * h3) * h + cx2 + dx2;
                float g4 = (g * g3 + f * h3) * h + cy2 + dy2;
                f2 = Math.min(f2, f4);
                g2 = Math.min(g2, g4);
                h2 = Math.max(h2, f4);
                f3 = Math.max(f3, g4);
            }
            this.bounds(f2, g2, h2, f3);
        }

        @Override
        public float distance(float f, float g) {
            float h = (f - this.dx - this.cx) / this.k;
            float f2 = (g - this.dy - this.cy) / this.k;
            return this.child.distance(
                            this.c * h + this.s * f2 + this.cx, -this.s * h + this.c * f2 + this.cy)
                    * this.k;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Union extends Shape2D {

        private final Shape2D[] kids;

        private final float k;

        Union(float f, Shape2D[] shape2Ds) {
            this.kids = (Shape2D[]) shape2Ds.clone();
            this.k = Math.max(0.0F, f);
            float g = 100000.0F;
            float h = 100000.0F;
            float f2 = -100000.0F;
            float g2 = -100000.0F;
            for (Shape2D kid : this.kids) {
                g = Math.min(g, kid.minX);
                h = Math.min(h, kid.minY);
                f2 = Math.max(f2, kid.maxX);
                g2 = Math.max(g2, kid.maxY);
            }
            this.bounds(g - this.k, h - this.k, f2 + this.k, g2 + this.k);
        }

        @Override
        public float distance(float f, float g) {
            float distance = this.kids[0].distance(f, g);
            for (int i = 1; i < this.kids.length; i++) {
                float distance2 = this.kids[i].distance(f, g);
                if (this.k > 0.0F) {
                    float h = clamp01(0.5F + 0.5F * (distance2 - distance) / this.k);
                    distance = distance2 + (distance - distance2) * h - this.k * h * (1.0F - h);
                } else if (distance2 < distance) {
                    distance = distance2;
                }
            }
            return distance;
        }
    }
}
