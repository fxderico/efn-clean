package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Paint {

    static final Paint DEFAULT = of(-4934476);

    public Paint() {}

    public abstract void apply(float f, float g, float h, Sample sample);

    public static Paint of(int i) {
        return new Paint.Solid(i);
    }

    public static Paint gradient(
            int i, float f, float g, float h, int j, float f2, float g2, float h2) {
        return new Paint.Linear(i, f, g, h, j, f2, g2, h2);
    }

    public static Paint vertical(int i, float f, int j, float g) {
        return new Paint.Linear(i, 0.0F, f, 0.0F, j, 0.0F, g, 0.0F);
    }

    public static Paint radial(int i, float f, float g, float h, float f2, int j) {
        return new Paint.Radial(i, f, g, h, f2, j);
    }

    public static Paint wood(
            int i, int j, float f, float g, float h, float f2, float g2, float h2, float f3) {
        return new Paint.Wood(i, j, f, g, h, f2, g2, h2, f3);
    }

    public static Paint field(Paint.Field field2) {
        return new Paint.Custom(field2);
    }

    public Paint noise(int i, float f, float g) {
        return new Paint.Blotch(this, i, f, g);
    }

    public Paint grain(float f, float g) {
        return new Paint.Grain(this, f, g);
    }

    public Paint band(
            int i, float f, float g, float h, float f2, float g2, float h2, float f3, float g3) {
        return new Paint.Band(this, i, f, g, h, f2, g2, h2, f3, g3);
    }

    public Paint glow() {
        return this.glow(1.0F);
    }

    public Paint glow(float f) {
        return new Paint.Glow(this, f);
    }

    public Paint flatLight(float f) {
        return new Paint.Shading(this, Math.max(0.0F, Math.min(1.0F, f)), 0.0F / 0.0F);
    }

    public Paint occlusion(float f) {
        return new Paint.Shading(this, 0.0F / 0.0F, Math.max(0.0F, Math.min(1.0F, f)));
    }

    static float smooth(float f, float g, float h) {
        float f2 = (h - f) / (g - f);
        f2 = f2 < 0.0F ? 0.0F : Math.min(f2, 1.0F);
        return f2 * f2 * (3.0F - 2.0F * f2);
    }

    @Environment(EnvType.CLIENT)
    private static final class Band extends Paint {

        private final Paint base;

        private final int argb;

        private final float px;

        private final float py;

        private final float pz;

        private final float nx;

        private final float ny;

        private final float nz;

        private final float half;

        private final float soft;

        Band(
                Paint base2,
                int argb2,
                float px2,
                float py2,
                float pz2,
                float f,
                float g,
                float h,
                float half2,
                float f2) {
            this.base = base2;
            this.argb = argb2;
            this.px = px2;
            this.py = py2;
            this.pz = pz2;
            float g2 = (float) Math.sqrt(f * f + g * g + h * h);
            this.nx = g2 < 1.0E-6F ? 0.0F : f / g2;
            this.ny = g2 < 1.0E-6F ? 1.0F : g / g2;
            this.nz = g2 < 1.0E-6F ? 0.0F : h / g2;
            this.half = half2;
            this.soft = Math.max(0.001F, f2);
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            this.base.apply(f, g, h, sample);
            float f2 =
                    Math.abs(
                            (f - this.px) * this.nx
                                    + (g - this.py) * this.ny
                                    + (h - this.pz) * this.nz);
            float g2 = 1.0F - smooth(this.half, this.half + this.soft, f2);
            if (g2 > 0.0F) {
                float glow = sample.glow;
                sample.toward(this.argb, g2 * ((this.argb >>> 24) / 255.0F));
                sample.glow = glow;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Blotch extends Paint {

        private final Paint base;

        private final int other;

        private final float freq;

        private final float amount;

        Blotch(Paint base2, int other2, float f, float amount2) {
            this.base = base2;
            this.other = other2;
            this.freq = 1.0F / Math.max(0.01F, f);
            this.amount = amount2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            this.base.apply(f, g, h, sample);
            float f2 = Noise.fbm(f * this.freq, g * this.freq, h * this.freq, 3) * 1.6F + 0.5F;
            float g2 = (f2 < 0.0F ? 0.0F : Math.min(f2, 1.0F)) * this.amount;
            float glow = sample.glow;
            sample.toward(this.other, g2);
            sample.glow = glow;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Custom extends Paint {

        private final Paint.Field field;

        Custom(Paint.Field field2) {
            this.field = field2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            sample.set(this.field.argb(f, g, h));
        }
    }

    @Environment(EnvType.CLIENT)
    public interface Field {

        int argb(float f, float g, float h);
    }

    @Environment(EnvType.CLIENT)
    private static final class Glow extends Paint {

        private final Paint base;

        private final float amount;

        Glow(Paint base2, float amount2) {
            this.base = base2;
            this.amount = amount2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            this.base.apply(f, g, h, sample);
            sample.glow = this.amount;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Grain extends Paint {

        private final Paint base;

        private final float freq;

        private final float amount;

        Grain(Paint base2, float f, float amount2) {
            this.base = base2;
            this.freq = 1.0F / Math.max(0.01F, f);
            this.amount = amount2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            this.base.apply(f, g, h, sample);
            float f2 =
                    1.0F
                            + Noise.fbm(f * this.freq, g * this.freq, h * this.freq, 2)
                                    * 2.0F
                                    * this.amount;
            sample.r *= f2;
            sample.g *= f2;
            sample.b *= f2;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Linear extends Paint {

        private final int from;

        private final int to;

        private final float ax;

        private final float ay;

        private final float az;

        private final float dx;

        private final float dy;

        private final float dz;

        private final float inv;

        Linear(int from2, float ax2, float ay2, float az2, int to2, float f, float g, float h) {
            this.from = from2;
            this.to = to2;
            this.ax = ax2;
            this.ay = ay2;
            this.az = az2;
            this.dx = f - ax2;
            this.dy = g - ay2;
            this.dz = h - az2;
            float f2 = this.dx * this.dx + this.dy * this.dy + this.dz * this.dz;
            this.inv = f2 < 1.0E-8F ? 0.0F : 1.0F / f2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            float f2 =
                    ((f - this.ax) * this.dx + (g - this.ay) * this.dy + (h - this.az) * this.dz)
                            * this.inv;
            f2 = f2 < 0.0F ? 0.0F : Math.min(f2, 1.0F);
            sample.set(this.from);
            sample.toward(this.to, f2);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Radial extends Paint {

        private final int inner;

        private final int outer;

        private final float cx;

        private final float cy;

        private final float cz;

        private final float radius;

        Radial(int inner2, float cx2, float cy2, float cz2, float f, int outer2) {
            this.inner = inner2;
            this.outer = outer2;
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            this.radius = Math.max(1.0E-4F, f);
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            float f2 = f - this.cx;
            float g2 = g - this.cy;
            float h2 = h - this.cz;
            float f3 = (float) Math.sqrt(f2 * f2 + g2 * g2 + h2 * h2) / this.radius;
            sample.set(this.inner);
            sample.toward(this.outer, f3 < 0.0F ? 0.0F : Math.min(f3, 1.0F));
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Shading extends Paint {

        private final Paint base;

        private final float flat;

        private final float occlusion;

        Shading(Paint base2, float flat2, float occlusion2) {
            this.base = base2;
            this.flat = flat2;
            this.occlusion = occlusion2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            this.base.apply(f, g, h, sample);
            if (this.flat == this.flat) {
                sample.flat = this.flat;
                sample.flatWeight = 1.0F;
            }
            if (this.occlusion == this.occlusion) {
                sample.occlusion = this.occlusion;
                sample.occlusionWeight = 1.0F;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Solid extends Paint {

        private final int argb;

        Solid(int argb2) {
            this.argb = argb2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            sample.set(this.argb);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Wood extends Paint {

        private final int light;

        private final int dark;

        private final float ax;

        private final float ay;

        private final float az;

        private final float dx;

        private final float dy;

        private final float dz;

        private final float spacing;

        Wood(
                int light2,
                int dark2,
                float ax2,
                float ay2,
                float az2,
                float f,
                float g,
                float h,
                float f2) {
            this.light = light2;
            this.dark = dark2;
            this.ax = ax2;
            this.ay = ay2;
            this.az = az2;
            float g2 = (float) Math.sqrt(f * f + g * g + h * h);
            this.dx = g2 < 1.0E-6F ? 0.0F : f / g2;
            this.dy = g2 < 1.0E-6F ? 1.0F : g / g2;
            this.dz = g2 < 1.0E-6F ? 0.0F : h / g2;
            this.spacing = Math.max(0.05F, f2);
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            float f2 = f - this.ax;
            float g2 = g - this.ay;
            float h2 = h - this.az;
            float f3 = f2 * this.dx + g2 * this.dy + h2 * this.dz;
            float g3 = f2 - this.dx * f3;
            float h3 = g2 - this.dy * f3;
            float f4 = h2 - this.dz * f3;
            float g4 = (float) Math.sqrt(g3 * g3 + h3 * h3 + f4 * f4);
            float h4 = 1.0F / this.spacing;
            float fbm =
                    Noise.fbm(
                            g3 * 0.3F + f3 * 0.021F,
                            h3 * 0.3F + f3 * 0.017F,
                            f4 * 0.3F + f3 * 0.013F,
                            2);
            float fbm2 =
                    Noise.fbm(
                            g3 * h4 * 1.6F + fbm,
                            h3 * h4 * 1.6F + f3 * 0.045F,
                            f4 * h4 * 1.6F - fbm,
                            3);
            float f5 = g4 * h4 + fbm * 2.2F;
            float g5 = f5 - (float) Math.floor(f5);
            float h5 = smooth(0.7F, 0.85F, g5) * (1.0F - smooth(0.88F, 1.0F, g5));
            float f6 = 0.18F + fbm2 * 0.9F + h5 * 0.22F;
            sample.set(this.light);
            sample.toward(this.dark, f6 < 0.0F ? 0.0F : Math.min(f6, 1.0F));
        }
    }
}
