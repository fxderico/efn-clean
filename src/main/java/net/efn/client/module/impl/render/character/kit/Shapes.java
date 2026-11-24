package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
public final class Shapes {

    private static final ThreadLocal<Shapes.Scratch> SCRATCH =
            ThreadLocal.withInitial(Shapes.Scratch::new);

    private Shapes() {}

    public static Shape sphere(float f, float g, float f2, float g2) {
        return new Shapes.Sphere(f, g, f2, g2);
    }

    public static Shape ellipsoid(float f, float g, float f2, float g2, float h2, float f3) {
        return new Shapes.Ellipsoid(f, g, f2, g2, h2, f3);
    }

    public static Shape box(float f, float g, float f2, float g2, float h2, float f3) {
        return new Shapes.Box(f, g, f2, g2, h2, f3, 0.0F);
    }

    public static Shape box(float f, float g, float f2, float g2, float h2, float f3, float g3) {
        return new Shapes.Box(f, g, f2, g2, h2, f3, g3);
    }

    public static Shape capsule(
            float f, float g, float f2, float g2, float h2, float f3, float g3) {
        return new Shapes.RoundCone(f, g, f2, g3, g2, h2, f3, g3);
    }

    public static Shape cone(
            float f, float g, float f2, float g2, float h2, float f3, float g3, float h3) {
        return new Shapes.RoundCone(f, g, f2, g2, h2, f3, g3, h3);
    }

    public static Shape cylinder(
            float f, float g, float f2, float g2, float h2, float f3, float g3, float h3) {
        return new Shapes.Cylinder(f, g, f2, g2, h2, f3, g3, h3);
    }

    public static Shape torus(float f, float g, float f2, float g2, float h2) {
        return new Shapes.Torus(f, g, f2, g2, h2);
    }

    public static Shape plane(float f, float g, float f2, float g2, float h2, float f3) {
        return new Shapes.Plane(f, g, f2, g2, h2, f3);
    }

    public static Shape below(float f) {
        return new Shapes.Plane(0.0F, f, 0.0F, 0.0F, 1.0F, 0.0F);
    }

    public static Shape above(float f) {
        return new Shapes.Plane(0.0F, f, 0.0F, 0.0F, -1.0F, 0.0F);
    }

    public static Strand strand(float f, float g, float... f2) {
        return Strand.tapered(f, g, f2);
    }

    public static Strand tube(float... f) {
        return Strand.of(f, false);
    }

    public static Shape extrude(Shape2D shape2D, float f, float g) {
        return new Shapes.Extrude(shape2D, f, g);
    }

    public static Shape field(
            Shapes.Field field2, float f, float g, float f2, float g2, float h2, float f3) {
        return new Shapes.Custom(field2, f, g, f2, g2, h2, f3);
    }

    public static Shape union(Shape... shape) {
        return (Shape) (shape.length == 1 ? shape[0] : new Shapes.Union(0.0F, shape));
    }

    public static Shape blend(float f, Shape... shape) {
        return (Shape) (shape.length == 1 ? shape[0] : new Shapes.Union(f, shape));
    }

    public static Shape subtract(Shape shape, Shape shape2) {
        return new Shapes.Subtract(0.0F, shape, shape2);
    }

    public static Shape subtract(float f, Shape shape, Shape shape2) {
        return new Shapes.Subtract(f, shape, shape2);
    }

    public static Shape intersect(Shape shape, Shape shape2) {
        return new Shapes.Intersect(0.0F, shape, shape2);
    }

    public static Shape intersect(float f, Shape shape, Shape shape2) {
        return new Shapes.Intersect(f, shape, shape2);
    }

    static float clamp01(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }

    static float length(float f, float g, float f2) {
        return (float) Math.sqrt(f * f + g * g + f2 * f2);
    }

    static float boxLowerBound(Shape shape, float f, float g, float f2) {
        float g2 = (shape.minX + shape.maxX) * 0.5F;
        float h2 = (shape.minY + shape.maxY) * 0.5F;
        float f3 = (shape.minZ + shape.maxZ) * 0.5F;
        float g3 = Math.abs(f - g2) - (shape.maxX - shape.minX) * 0.5F;
        float h3 = Math.abs(g - h2) - (shape.maxY - shape.minY) * 0.5F;
        float f4 = Math.abs(f2 - f3) - (shape.maxZ - shape.minZ) * 0.5F;
        float g4 = Math.max(g3, 0.0F);
        float h4 = Math.max(h3, 0.0F);
        float f5 = Math.max(f4, 0.0F);
        return (float) Math.sqrt(g4 * g4 + h4 * h4 + f5 * f5)
                + Math.min(Math.max(g3, Math.max(h3, f4)), 0.0F);
    }

    @Environment(EnvType.CLIENT)
    static final class Box extends Shape {

        private final float cx;

        private final float cy;

        private final float cz;

        private final float hx;

        private final float hy;

        private final float hz;

        private final float r;

        Box(float cx2, float cy2, float cz2, float f, float g, float f2, float g2) {
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            this.r = Math.max(0.0F, Math.min(g2, Math.min(f, Math.min(g, f2))));
            this.hx = f - this.r;
            this.hy = g - this.r;
            this.hz = f2 - this.r;
            this.bounds(cx2 - f, cy2 - g, cz2 - f2, cx2 + f, cy2 + g, cz2 + f2);
        }

        @Override
        public float distance(float f, float g, float f2) {
            float g2 = Math.abs(f - this.cx) - this.hx;
            float h2 = Math.abs(g - this.cy) - this.hy;
            float f3 = Math.abs(f2 - this.cz) - this.hz;
            float g3 = Math.max(g2, 0.0F);
            float h3 = Math.max(h2, 0.0F);
            float f4 = Math.max(f3, 0.0F);
            return Shapes.length(g3, h3, f4)
                    + Math.min(Math.max(g2, Math.max(h2, f3)), 0.0F)
                    - this.r;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Custom extends Shape {

        private final Shapes.Field field;

        Custom(Shapes.Field field2, float f, float g, float f2, float g2, float h2, float f3) {
            this.field = field2;
            this.bounds(f, g, f2, g2, h2, f3);
        }

        @Override
        public float distance(float f, float g, float f2) {
            return this.field.distance(f, g, f2);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Cylinder extends Shape {

        private final float ax;

        private final float ay;

        private final float az;

        private final float bax;

        private final float bay;

        private final float baz;

        private final float baba;

        private final float r;

        private final float round;

        Cylinder(float f, float g, float f2, float g2, float h2, float f3, float g3, float h3) {
            float f4 = g2 - f;
            float g4 = h2 - g;
            float h4 = f3 - f2;
            float f5 = Shapes.length(f4, g4, h4);
            float g5 = Math.max(0.0F, Math.min(h3, Math.min(g3, f5 * 0.5F)));
            float h5 = f5 > 1.0E-6F ? f4 / f5 : 0.0F;
            float f6 = f5 > 1.0E-6F ? g4 / f5 : 1.0F;
            float g6 = f5 > 1.0E-6F ? h4 / f5 : 0.0F;
            this.ax = f + h5 * g5;
            this.ay = g + f6 * g5;
            this.az = f2 + g6 * g5;
            this.bax = g2 - h5 * g5 - this.ax;
            this.bay = h2 - f6 * g5 - this.ay;
            this.baz = f3 - g6 * g5 - this.az;
            this.baba =
                    Math.max(
                            1.0E-8F,
                            this.bax * this.bax + this.bay * this.bay + this.baz * this.baz);
            this.r = g3 - g5;
            this.round = g5;
            float h6 = g3 * (float) Math.sqrt(Math.max(0.0F, 1.0F - h5 * h5));
            float f7 = g3 * (float) Math.sqrt(Math.max(0.0F, 1.0F - f6 * f6));
            float g7 = g3 * (float) Math.sqrt(Math.max(0.0F, 1.0F - g6 * g6));
            this.bounds(
                    Math.min(f, g2) - h6,
                    Math.min(g, h2) - f7,
                    Math.min(f2, f3) - g7,
                    Math.max(f, g2) + h6,
                    Math.max(g, h2) + f7,
                    Math.max(f2, f3) + g7);
        }

        @Override
        public float distance(float f, float g, float f2) {
            float g2 = f - this.ax;
            float h2 = g - this.ay;
            float f3 = f2 - this.az;
            float g3 = g2 * this.bax + h2 * this.bay + f3 * this.baz;
            float h3 =
                    Shapes.length(
                                    g2 * this.baba - this.bax * g3,
                                    h2 * this.baba - this.bay * g3,
                                    f3 * this.baba - this.baz * g3)
                            - this.r * this.baba;
            float f4 = Math.abs(g3 - this.baba * 0.5F) - this.baba * 0.5F;
            float g4 = h3 * h3;
            float h4 = f4 * f4 * this.baba;
            float f5 =
                    Math.max(h3, f4) < 0.0F
                            ? -Math.min(g4, h4)
                            : (h3 > 0.0F ? g4 : 0.0F) + (f4 > 0.0F ? h4 : 0.0F);
            return Math.signum(f5) * (float) Math.sqrt(Math.abs(f5)) / this.baba - this.round;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Ellipsoid extends Shape {

        private final float cx;

        private final float cy;

        private final float cz;

        private final float ix;

        private final float iy;

        private final float iz;

        private final float iix;

        private final float iiy;

        private final float iiz;

        private final float rmin;

        Ellipsoid(float cx2, float cy2, float cz2, float f, float g, float f2) {
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            this.ix = 1.0F / f;
            this.iy = 1.0F / g;
            this.iz = 1.0F / f2;
            this.iix = this.ix * this.ix;
            this.iiy = this.iy * this.iy;
            this.iiz = this.iz * this.iz;
            this.rmin = Math.min(f, Math.min(g, f2));
            this.bounds(cx2 - f, cy2 - g, cz2 - f2, cx2 + f, cy2 + g, cz2 + f2);
        }

        @Override
        public float distance(float f, float g, float f2) {
            float g2 = f - this.cx;
            float h2 = g - this.cy;
            float f3 = f2 - this.cz;
            float g3 = Shapes.length(g2 * this.ix, h2 * this.iy, f3 * this.iz);
            float h3 = Shapes.length(g2 * this.iix, h2 * this.iiy, f3 * this.iiz);
            return h3 < 1.0E-9F ? -this.rmin : g3 * (g3 - 1.0F) / h3;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Extrude extends Shape {

        private final Shape2D profile;

        private final float h;

        private final float r;

        Extrude(Shape2D profile2, float f, float g) {
            this.profile = profile2;
            this.r = Math.max(0.0F, Math.min(g, f));
            this.h = f;
            this.bounds(profile2.minX, profile2.minY, -f, profile2.maxX, profile2.maxY, f);
        }

        @Override
        public float distance(float f, float g, float f2) {
            float g2 = this.profile.distance(f, g) + this.r;
            float h2 = Math.abs(f2) - this.h + this.r;
            float f3 = Math.max(g2, 0.0F);
            float g3 = Math.max(h2, 0.0F);
            return Math.min(Math.max(g2, h2), 0.0F) + (float) Math.sqrt(f3 * f3 + g3 * g3) - this.r;
        }
    }

    @Environment(EnvType.CLIENT)
    public interface Field {

        float distance(float f, float g, float f2);
    }

    @Environment(EnvType.CLIENT)
    static final class Intersect extends Shape {

        private final Shape a;

        private final Shape b;

        private final float k;

        Intersect(float f, Shape shape, Shape shape2) {
            this.k = Math.max(0.0F, f);
            this.a = shape;
            this.b = shape2;
            if (shape.bounded() && shape2.bounded()) {
                this.bounds(
                        Math.max(shape.minX, shape2.minX),
                        Math.max(shape.minY, shape2.minY),
                        Math.max(shape.minZ, shape2.minZ),
                        Math.min(shape.maxX, shape2.maxX),
                        Math.min(shape.maxY, shape2.maxY),
                        Math.min(shape.maxZ, shape2.maxZ));
            } else if (shape.bounded()) {
                this.boundsOf(shape, 0.0F);
            } else if (shape2.bounded()) {
                this.boundsOf(shape2, 0.0F);
            }
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            Shape shape = this.a.prune(f, g, f2, i, j, k);
            Shape shape2 = this.b.prune(f, g, f2, i, j, k);
            return shape == this.a && shape2 == this.b
                    ? this
                    : new Shapes.Intersect(this.k, shape, shape2);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.a.map(unaryOperator);
            Shape shape2 = this.b.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.a && shape2 == this.b
                                    ? this
                                    : new Shapes.Intersect(this.k, shape, shape2).inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            float distance = this.a.distance(f, g, f2);
            float distance2 = this.b.distance(f, g, f2);
            if (this.k <= 0.0F) {
                return Math.max(distance, distance2);
            } else {
                float g2 = Shapes.clamp01(0.5F - 0.5F * (distance2 - distance) / this.k);
                return distance2 + (distance - distance2) * g2 + this.k * g2 * (1.0F - g2);
            }
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            float sample2 = this.a.sample(f, g, f2, sample);
            if (this.b.paint == null) {
                float distance = this.b.distance(f, g, f2);
                if (this.k <= 0.0F) {
                    return Math.max(sample2, distance);
                } else {
                    float g2 = Shapes.clamp01(0.5F - 0.5F * (distance - sample2) / this.k);
                    return distance + (sample2 - distance) * g2 + this.k * g2 * (1.0F - g2);
                }
            } else {
                Sample sample3 = new Sample();
                float sample4 = this.b.sample(f, g, f2, sample3);
                if (this.k <= 0.0F) {
                    if (sample4 > sample2) {
                        sample.set(sample3);
                        return sample4;
                    } else {
                        return sample2;
                    }
                } else {
                    float h2 = Shapes.clamp01(0.5F - 0.5F * (sample4 - sample2) / this.k);
                    sample.mix(sample3, 1.0F - h2);
                    return sample4 + (sample2 - sample4) * h2 + this.k * h2 * (1.0F - h2);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Mirror extends Shape {

        private final Shape child;

        private final boolean keep;

        Mirror(Shape child2, boolean keep2) {
            this.child = child2;
            this.keep = keep2;
            if (child2.bounded()) {
                if (keep2) {
                    this.bounds(
                            Math.min(child2.minX, -child2.maxX),
                            child2.minY,
                            child2.minZ,
                            Math.max(child2.maxX, -child2.minX),
                            child2.maxY,
                            child2.maxZ);
                } else {
                    this.bounds(
                            -child2.maxX,
                            child2.minY,
                            child2.minZ,
                            -child2.minX,
                            child2.maxY,
                            child2.maxZ);
                }
            }
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            Shape shape =
                    this.keep
                            ? this.child.prune(Math.min(f, -i), g, f2, Math.max(i, -f), j, k)
                            : this.child.prune(-i, g, f2, -f, j, k);
            return shape == this.child ? this : new Shapes.Mirror(shape, this.keep);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Mirror(shape, this.keep).inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            if (!this.keep) {
                return this.child.distance(-f, g, f2);
            } else {
                float distance = this.child.distance(f, g, f2);
                return Shapes.boxLowerBound(this.child, -f, g, f2) >= distance
                        ? distance
                        : Math.min(distance, this.child.distance(-f, g, f2));
            }
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            if (!this.keep) {
                return this.child.sample(-f, g, f2, sample);
            } else {
                float distance = this.child.distance(f, g, f2);
                float g2 =
                        Shapes.boxLowerBound(this.child, -f, g, f2) >= distance
                                ? 3.4028235E38F
                                : this.child.distance(-f, g, f2);
                return g2 < distance
                        ? this.child.sample(-f, g, f2, sample)
                        : this.child.sample(f, g, f2, sample);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Plane extends Shape {

        private final float px;

        private final float py;

        private final float pz;

        private final float nx;

        private final float ny;

        private final float nz;

        Plane(float px2, float py2, float pz2, float f, float g, float f2) {
            float g2 = Shapes.length(f, g, f2);
            this.px = px2;
            this.py = py2;
            this.pz = pz2;
            this.nx = g2 < 1.0E-8F ? 0.0F : f / g2;
            this.ny = g2 < 1.0E-8F ? 1.0F : g / g2;
            this.nz = g2 < 1.0E-8F ? 0.0F : f2 / g2;
        }

        @Override
        public float distance(float f, float g, float f2) {
            return (f - this.px) * this.nx + (g - this.py) * this.ny + (f2 - this.pz) * this.nz;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Revolve extends Shape {

        private final Shape2D profile;

        Revolve(Shape2D profile2) {
            this.profile = profile2;
            float f = Math.max(Math.abs(profile2.minX), Math.abs(profile2.maxX));
            this.bounds(-f, profile2.minY, -f, f, profile2.maxY, f);
        }

        @Override
        public float distance(float f, float g, float f2) {
            return this.profile.distance((float) Math.sqrt(f * f + f2 * f2), g);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Rotate extends Shape {

        private final Shape child;

        private final Mat3 inv;

        private final float cx;

        private final float cy;

        private final float cz;

        Rotate(Shape child2, Mat3 mat3, float cx2, float cy2, float cz2) {
            this.child = child2;
            this.inv = mat3.transpose();
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            if (child2.bounded()) {
                float f = 100000.0F;
                float g = 100000.0F;
                float f2 = 100000.0F;
                float g2 = -100000.0F;
                float h2 = -100000.0F;
                float f3 = -100000.0F;
                for (int i = 0; i < 8; i++) {
                    float g3 = ((i & 1) == 0 ? child2.minX : child2.maxX) - cx2;
                    float h3 = ((i & 2) == 0 ? child2.minY : child2.maxY) - cy2;
                    float f4 = ((i & 4) == 0 ? child2.minZ : child2.maxZ) - cz2;
                    float g4 = mat3.x(g3, h3, f4) + cx2;
                    float h4 = mat3.y(g3, h3, f4) + cy2;
                    float f5 = mat3.z(g3, h3, f4) + cz2;
                    f = Math.min(f, g4);
                    g = Math.min(g, h4);
                    f2 = Math.min(f2, f5);
                    g2 = Math.max(g2, g4);
                    h2 = Math.max(h2, h4);
                    f3 = Math.max(f3, f5);
                }
                this.bounds(f, g, f2, g2, h2, f3);
            }
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            float g2 = 100000.0F;
            float h2 = 100000.0F;
            float f3 = 100000.0F;
            float g3 = -100000.0F;
            float h3 = -100000.0F;
            float f4 = -100000.0F;
            for (int m = 0; m < 8; m++) {
                float g4 = ((m & 1) == 0 ? f : i) - this.cx;
                float h4 = ((m & 2) == 0 ? g : j) - this.cy;
                float f5 = ((m & 4) == 0 ? f2 : k) - this.cz;
                float g5 = this.inv.x(g4, h4, f5) + this.cx;
                float h5 = this.inv.y(g4, h4, f5) + this.cy;
                float f6 = this.inv.z(g4, h4, f5) + this.cz;
                g2 = Math.min(g2, g5);
                h2 = Math.min(h2, h5);
                f3 = Math.min(f3, f6);
                g3 = Math.max(g3, g5);
                h3 = Math.max(h3, h5);
                f4 = Math.max(f4, f6);
            }
            Shape shape = this.child.prune(g2, h2, f3, g3, h3, f4);
            return shape == this.child
                    ? this
                    : new Shapes.Rotate(shape, this.inv.transpose(), this.cx, this.cy, this.cz);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Rotate(
                                                    shape,
                                                    this.inv.transpose(),
                                                    this.cx,
                                                    this.cy,
                                                    this.cz)
                                            .inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            float g2 = f - this.cx;
            float h2 = g - this.cy;
            float f3 = f2 - this.cz;
            return this.child.distance(
                    this.inv.x(g2, h2, f3) + this.cx,
                    this.inv.y(g2, h2, f3) + this.cy,
                    this.inv.z(g2, h2, f3) + this.cz);
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            float g2 = f - this.cx;
            float h2 = g - this.cy;
            float f3 = f2 - this.cz;
            return this.child.sample(
                    this.inv.x(g2, h2, f3) + this.cx,
                    this.inv.y(g2, h2, f3) + this.cy,
                    this.inv.z(g2, h2, f3) + this.cz,
                    sample);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Roughen extends Shape {

        private final Shape child;

        private final float amp;

        private final float freq;

        Roughen(Shape child2, float amp2, float f) {
            this.child = child2;
            this.amp = amp2;
            this.freq = 1.0F / Math.max(0.01F, f);
            this.boundsOf(child2, Math.abs(amp2));
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            Shape shape = this.child.prune(f, g, f2, i, j, k);
            return shape == this.child
                    ? this
                    : new Shapes.Roughen(shape, this.amp, 1.0F / this.freq);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Roughen(shape, this.amp, 1.0F / this.freq)
                                            .inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            float distance = this.child.distance(f, g, f2);
            return distance > this.amp * 1.5F + 0.5F
                    ? distance
                    : distance
                            + this.amp * Noise.noise(f * this.freq, g * this.freq, f2 * this.freq);
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            return this.child.sample(f, g, f2, sample)
                    + this.amp * Noise.noise(f * this.freq, g * this.freq, f2 * this.freq);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Round extends Shape {

        private final Shape child;

        private final float r;

        Round(Shape child2, float f) {
            this.child = child2;
            this.r = f;
            this.boundsOf(child2, Math.max(0.0F, f));
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            Shape shape = this.child.prune(f, g, f2, i, j, k);
            return shape == this.child ? this : new Shapes.Round(shape, this.r);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Round(shape, this.r).inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            return this.child.distance(f, g, f2) - this.r;
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            return this.child.sample(f, g, f2, sample) - this.r;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class RoundCone extends Shape {

        private final float ax;

        private final float ay;

        private final float az;

        private final float bax;

        private final float bay;

        private final float baz;

        private final float ra;

        private final float rb;

        private final float l2;

        private final float rr;

        private final float a2;

        private final float il2;

        private final boolean sphere;

        private final float sx;

        private final float sy;

        private final float sz;

        private final float sr;

        RoundCone(
                float ax2, float ay2, float az2, float ra2, float f, float g, float f2, float rb2) {
            this.ax = ax2;
            this.ay = ay2;
            this.az = az2;
            this.ra = ra2;
            this.rb = rb2;
            this.bax = f - ax2;
            this.bay = g - ay2;
            this.baz = f2 - az2;
            this.l2 = this.bax * this.bax + this.bay * this.bay + this.baz * this.baz;
            this.rr = ra2 - rb2;
            this.a2 = this.l2 - this.rr * this.rr;
            this.il2 = this.l2 > 1.0E-12F ? 1.0F / this.l2 : 0.0F;
            this.sphere = this.a2 <= 1.0E-6F;
            boolean bl = ra2 >= rb2;
            this.sx = bl ? ax2 : f;
            this.sy = bl ? ay2 : g;
            this.sz = bl ? az2 : f2;
            this.sr = Math.max(ra2, rb2);
            this.bounds(
                    Math.min(ax2 - ra2, f - rb2),
                    Math.min(ay2 - ra2, g - rb2),
                    Math.min(az2 - ra2, f2 - rb2),
                    Math.max(ax2 + ra2, f + rb2),
                    Math.max(ay2 + ra2, g + rb2),
                    Math.max(az2 + ra2, f2 + rb2));
        }

        @Override
        public float distance(float f, float g, float f2) {
            if (this.sphere) {
                return Shapes.length(f - this.sx, g - this.sy, f2 - this.sz) - this.sr;
            } else {
                float g2 = f - this.ax;
                float h2 = g - this.ay;
                float f3 = f2 - this.az;
                float g3 = g2 * this.bax + h2 * this.bay + f3 * this.baz;
                float h3 = g3 - this.l2;
                float f4 = g2 * this.l2 - this.bax * g3;
                float g4 = h2 * this.l2 - this.bay * g3;
                float h4 = f3 * this.l2 - this.baz * g3;
                float f5 = f4 * f4 + g4 * g4 + h4 * h4;
                float g5 = g3 * g3 * this.l2;
                float h5 = h3 * h3 * this.l2;
                float f6 = Math.signum(this.rr) * this.rr * this.rr * f5;
                if (Math.signum(h3) * this.a2 * h5 > f6) {
                    return (float) Math.sqrt(f5 + h5) * this.il2 - this.rb;
                } else {
                    return Math.signum(g3) * this.a2 * g5 < f6
                            ? (float) Math.sqrt(f5 + g5) * this.il2 - this.ra
                            : ((float) Math.sqrt(f5 * this.a2 * this.il2) + g3 * this.rr) * this.il2
                                    - this.ra;
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Scale extends Shape {

        private final Shape child;

        private final float k;

        private final float ik;

        private final float cx;

        private final float cy;

        private final float cz;

        Scale(Shape child2, float f, float cx2, float cy2, float cz2) {
            this.child = child2;
            this.k = f;
            this.ik = 1.0F / f;
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            if (child2.bounded()) {
                this.bounds(
                        (child2.minX - cx2) * f + cx2,
                        (child2.minY - cy2) * f + cy2,
                        (child2.minZ - cz2) * f + cz2,
                        (child2.maxX - cx2) * f + cx2,
                        (child2.maxY - cy2) * f + cy2,
                        (child2.maxZ - cz2) * f + cz2);
            }
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            float g2 = (f - this.cx) * this.ik + this.cx;
            float h2 = (i - this.cx) * this.ik + this.cx;
            float f3 = (g - this.cy) * this.ik + this.cy;
            float g3 = (j - this.cy) * this.ik + this.cy;
            float h3 = (f2 - this.cz) * this.ik + this.cz;
            float f4 = (k - this.cz) * this.ik + this.cz;
            Shape shape =
                    this.child.prune(
                            Math.min(g2, h2),
                            Math.min(f3, g3),
                            Math.min(h3, f4),
                            Math.max(g2, h2),
                            Math.max(f3, g3),
                            Math.max(h3, f4));
            return shape == this.child
                    ? this
                    : new Shapes.Scale(shape, this.k, this.cx, this.cy, this.cz);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Scale(shape, this.k, this.cx, this.cy, this.cz)
                                            .inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            return this.child.distance(
                            (f - this.cx) * this.ik + this.cx,
                            (g - this.cy) * this.ik + this.cy,
                            (f2 - this.cz) * this.ik + this.cz)
                    * this.k;
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            return this.child.sample(
                            (f - this.cx) * this.ik + this.cx,
                            (g - this.cy) * this.ik + this.cy,
                            (f2 - this.cz) * this.ik + this.cz,
                            sample)
                    * this.k;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Scratch {

        final float[][] pool = new float[64][];

        int depth;

        private Scratch() {}

        float[] take(int i) {
            int j;
            if ((j = this.depth++) >= this.pool.length) {
                return new float[i];
            } else {
                float[] floats = this.pool[j];
                if (floats == null || floats.length < i) {
                    floats = new float[Math.max(i, 16)];
                    this.pool[j] = floats;
                }
                return floats;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Shell extends Shape {

        private final Shape child;

        private final float half;

        Shell(Shape child2, float f) {
            this.child = child2;
            this.half = Math.abs(f) * 0.5F;
            this.boundsOf(child2, this.half);
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            Shape shape = this.child.prune(f, g, f2, i, j, k);
            return shape == this.child ? this : new Shapes.Shell(shape, this.half * 2.0F);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Shell(shape, this.half * 2.0F).inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            return Math.abs(this.child.distance(f, g, f2)) - this.half;
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            return Math.abs(this.child.sample(f, g, f2, sample)) - this.half;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Sphere extends Shape {

        private final float cx;

        private final float cy;

        private final float cz;

        private final float r;

        Sphere(float cx2, float cy2, float cz2, float f) {
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            this.r = f;
            this.bounds(cx2 - f, cy2 - f, cz2 - f, cx2 + f, cy2 + f, cz2 + f);
        }

        @Override
        public float distance(float f, float g, float f2) {
            return Shapes.length(f - this.cx, g - this.cy, f2 - this.cz) - this.r;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Subtract extends Shape {

        private final Shape base;

        private final Shape cutter;

        private final float k;

        Subtract(float f, Shape base2, Shape cutter2) {
            this.k = Math.max(0.0F, f);
            this.base = base2;
            this.cutter = cutter2;
            this.boundsOf(base2, this.k * 0.3F);
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            Shape shape = this.base.prune(f, g, f2, i, j, k);
            Shape shape2 = this.cutter.prune(f, g, f2, i, j, k);
            return shape == this.base && shape2 == this.cutter
                    ? this
                    : new Shapes.Subtract(this.k, shape, shape2);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.base.map(unaryOperator);
            Shape shape2 = this.cutter.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.base && shape2 == this.cutter
                                    ? this
                                    : new Shapes.Subtract(this.k, shape, shape2).inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            float distance = this.base.distance(f, g, f2);
            if (Shapes.boxLowerBound(this.cutter, f, g, f2) >= this.k - distance) {
                return distance;
            } else {
                float distance2 = this.cutter.distance(f, g, f2);
                if (this.k <= 0.0F) {
                    return Math.max(distance, -distance2);
                } else {
                    float g2 = Shapes.clamp01(0.5F - 0.5F * (distance + distance2) / this.k);
                    return distance + (-distance2 - distance) * g2 + this.k * g2 * (1.0F - g2);
                }
            }
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            float sample2 = this.base.sample(f, g, f2, sample);
            if (Shapes.boxLowerBound(this.cutter, f, g, f2) >= this.k - sample2) {
                return sample2;
            } else if (this.cutter.paint == null) {
                float distance = this.cutter.distance(f, g, f2);
                if (this.k <= 0.0F) {
                    return Math.max(sample2, -distance);
                } else {
                    float g2 = Shapes.clamp01(0.5F - 0.5F * (sample2 + distance) / this.k);
                    return sample2 + (-distance - sample2) * g2 + this.k * g2 * (1.0F - g2);
                }
            } else {
                Sample sample3 = new Sample();
                float sample4 = this.cutter.sample(f, g, f2, sample3);
                if (this.k <= 0.0F) {
                    if (-sample4 > sample2) {
                        sample.set(sample3);
                        return -sample4;
                    } else {
                        return sample2;
                    }
                } else {
                    float h2 = Shapes.clamp01(0.5F - 0.5F * (sample2 + sample4) / this.k);
                    sample.mix(sample3, h2);
                    return sample2 + (-sample4 - sample2) * h2 + this.k * h2 * (1.0F - h2);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Torus extends Shape {

        private final float cx;

        private final float cy;

        private final float cz;

        private final float major;

        private final float minor;

        Torus(float cx2, float cy2, float cz2, float major2, float minor2) {
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            this.major = major2;
            this.minor = minor2;
            float f = major2 + minor2;
            this.bounds(cx2 - f, cy2 - minor2, cz2 - f, cx2 + f, cy2 + minor2, cz2 + f);
        }

        @Override
        public float distance(float f, float g, float f2) {
            float g2 = f - this.cx;
            float h2 = f2 - this.cz;
            float f3 = (float) Math.sqrt(g2 * g2 + h2 * h2) - this.major;
            float g3 = g - this.cy;
            return (float) Math.sqrt(f3 * f3 + g3 * g3) - this.minor;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Translate extends Shape {

        private final Shape child;

        private final float dx;

        private final float dy;

        private final float dz;

        Translate(Shape child2, float dx2, float dy2, float dz2) {
            this.child = child2;
            this.dx = dx2;
            this.dy = dy2;
            this.dz = dz2;
            if (child2.bounded()) {
                this.bounds(
                        child2.minX + dx2,
                        child2.minY + dy2,
                        child2.minZ + dz2,
                        child2.maxX + dx2,
                        child2.maxY + dy2,
                        child2.maxZ + dz2);
            }
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            Shape shape =
                    this.child.prune(
                            f - this.dx,
                            g - this.dy,
                            f2 - this.dz,
                            i - this.dx,
                            j - this.dy,
                            k - this.dz);
            return shape == this.child
                    ? this
                    : new Shapes.Translate(shape, this.dx, this.dy, this.dz);
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Translate(shape, this.dx, this.dy, this.dz)
                                            .inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            return this.child.distance(f - this.dx, g - this.dy, f2 - this.dz);
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            return this.child.sample(f - this.dx, g - this.dy, f2 - this.dz, sample);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Twist extends Shape {

        private final Shape child;

        private final float k;

        private final float cx;

        private final float cz;

        Twist(Shape child2, float f, float cx2, float cz2) {
            this.child = child2;
            this.k = (float) Math.toRadians(f);
            this.cx = cx2;
            this.cz = cz2;
            if (child2.bounded()) {
                float g = 0.0F;
                for (int i = 0; i < 4; i++) {
                    float f2 = ((i & 1) == 0 ? child2.minX : child2.maxX) - cx2;
                    float g2 = ((i & 2) == 0 ? child2.minZ : child2.maxZ) - cz2;
                    g = Math.max(g, (float) Math.sqrt(f2 * f2 + g2 * g2));
                }
                this.bounds(cx2 - g, child2.minY, cz2 - g, cx2 + g, child2.maxY, cz2 + g);
            }
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape shape = this.child.map(unaryOperator);
            return (Shape)
                    unaryOperator.apply(
                            shape == this.child
                                    ? this
                                    : new Shapes.Twist(
                                                    shape,
                                                    (float) Math.toDegrees(this.k),
                                                    this.cx,
                                                    this.cz)
                                            .inherit(this));
        }

        @Override
        public float distance(float f, float g, float f2) {
            float g2 = -this.k * g;
            float h2 = (float) Math.cos(g2);
            float f3 = (float) Math.sin(g2);
            float g3 = f - this.cx;
            float h3 = f2 - this.cz;
            return this.child.distance(
                    h2 * g3 + f3 * h3 + this.cx, g, -f3 * g3 + h2 * h3 + this.cz);
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            float g2 = -this.k * g;
            float h2 = (float) Math.cos(g2);
            float f3 = (float) Math.sin(g2);
            float g3 = f - this.cx;
            float h3 = f2 - this.cz;
            return this.child.sample(
                    h2 * g3 + f3 * h3 + this.cx, g, -f3 * g3 + h2 * h3 + this.cz, sample);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Union extends Shape {

        private final Shape[] kids;

        private final float k;

        Union(float f, Shape[] shapes) {
            if (shapes.length == 0) {
                throw new IllegalArgumentException("union needs at least one shape");
            } else {
                this.k = Math.max(0.0F, f);
                this.kids = (Shape[]) shapes.clone();
                boolean bl = false;
                float g = 100000.0F;
                float f2 = 100000.0F;
                float g2 = 100000.0F;
                float h2 = -100000.0F;
                float f3 = -100000.0F;
                float g3 = -100000.0F;
                for (Shape kid : this.kids) {
                    if (!kid.bounded()) {
                        bl = true;
                    } else {
                        g = Math.min(g, kid.minX);
                        f2 = Math.min(f2, kid.minY);
                        g2 = Math.min(g2, kid.minZ);
                        h2 = Math.max(h2, kid.maxX);
                        f3 = Math.max(f3, kid.maxY);
                        g3 = Math.max(g3, kid.maxZ);
                    }
                }
                if (!bl) {
                    float h3 = this.k * 0.3F;
                    this.bounds(g - h3, f2 - h3, g2 - h3, h2 + h3, f3 + h3, g3 + h3);
                }
            }
        }

        @Override
        Shape map(UnaryOperator<Shape> unaryOperator) {
            Shape[] shapes = new Shape[this.kids.length];
            boolean bl = false;
            for (int i = 0; i < this.kids.length; i++) {
                shapes[i] = this.kids[i].map(unaryOperator);
                bl |= shapes[i] != this.kids[i];
            }
            return (Shape)
                    unaryOperator.apply(bl ? new Shapes.Union(this.k, shapes).inherit(this) : this);
        }

        @Override
        Shape prune(float f, float g, float f2, float i, float j, float k) {
            float g2 = (f + i) * 0.5F;
            float h2 = (g + j) * 0.5F;
            float f3 = (f2 + k) * 0.5F;
            float g3 = Shapes.length(i - f, j - g, k - f2) * 0.5F * 1.2F;
            float h3 = 3.4028235E38F;
            for (Shape kid : this.kids) {
                if (!(kid.boxGap(f, g, f2, i, j, k) >= h3 + this.k)) {
                    h3 = Math.min(h3, kid.distance(g2, h2, f3) + g3);
                }
            }
            Shape[] shapes = new Shape[this.kids.length];
            int m = 0;
            boolean bl = false;
            for (Shape kid2 : this.kids) {
                if (kid2.boxGap(f, g, f2, i, j, k) >= h3 + this.k) {
                    bl = true;
                } else {
                    Shape shape = kid2.prune(f, g, f2, i, j, k);
                    bl |= shape != kid2;
                    shapes[m++] = shape;
                }
            }
            if (!bl || m == 0) {
                return this;
            } else {
                return (Shape)
                        (m == 1 ? shapes[0] : new Shapes.Union(this.k, Arrays.copyOf(shapes, m)));
            }
        }

        private int nearest(float f, float g, float f2, float[] floats) {
            int i = 0;
            float g2 = 3.4028235E38F;
            for (int j = 0; j < this.kids.length; j++) {
                float boxLowerBound = Shapes.boxLowerBound(this.kids[j], f, g, f2);
                if (floats != null) {
                    floats[j] = boxLowerBound;
                }
                if (boxLowerBound < g2) {
                    g2 = boxLowerBound;
                    i = j;
                }
            }
            return i;
        }

        @Override
        public float distance(float f, float g, float f2) {
            Shape[] kids2 = this.kids;
            if (kids2.length <= 4) {
                return this.distance(f, g, f2, null);
            } else {
                Shapes.Scratch sCRATCH = Shapes.SCRATCH.get();
                float[] floats = sCRATCH.take(kids2.length);
                float g2;
                try {
                    float distance = this.distance(f, g, f2, floats);
                    g2 = distance;
                } finally {
                    sCRATCH.depth--;
                }
                return g2;
            }
        }

        @Override
        float distance(float f, float g, float f2, float i) {
            Shape[] kids2 = this.kids;
            if (kids2.length <= 4) {
                return this.evaluate(f, g, f2, null, i);
            } else {
                Shapes.Scratch sCRATCH = Shapes.SCRATCH.get();
                float[] floats = sCRATCH.take(kids2.length);
                float g2;
                try {
                    float evaluate = this.evaluate(f, g, f2, floats, i);
                    g2 = evaluate;
                } finally {
                    sCRATCH.depth--;
                }
                return g2;
            }
        }

        private float distance(float f, float g, float f2, float[] floats) {
            return this.evaluate(f, g, f2, floats, 3.4028235E38F);
        }

        private float evaluate(float f, float g, float f2, float[] floats, float g2) {
            Shape[] kids2 = this.kids;
            int nearest = this.nearest(f, g, f2, floats);
            float h2 =
                    floats != null
                            ? floats[nearest]
                            : Shapes.boxLowerBound(kids2[nearest], f, g, f2);
            if (h2 >= g2) {
                return h2;
            } else {
                float f3 = this.k;
                float distance =
                        kids2[nearest].distance(
                                f, g, f2, g2 == 3.4028235E38F ? 3.4028235E38F : g2 + f3);
                for (int m = 0; m < kids2.length; m++) {
                    if (m != nearest) {
                        float g3 =
                                floats != null
                                        ? floats[m]
                                        : Shapes.boxLowerBound(kids2[m], f, g, f2);
                        if (!(g3 >= distance + f3)) {
                            float distance2 = kids2[m].distance(f, g, f2, distance + f3);
                            if (f3 > 0.0F) {
                                float h3 =
                                        Shapes.clamp01(0.5F + 0.5F * (distance2 - distance) / f3);
                                distance =
                                        distance2
                                                + (distance - distance2) * h3
                                                - f3 * h3 * (1.0F - h3);
                            } else if (distance2 < distance) {
                                distance = distance2;
                            }
                        }
                    }
                }
                return distance;
            }
        }

        @Override
        float shade(float f, float g, float f2, Sample sample) {
            Shape[] kids2 = this.kids;
            int nearest = this.nearest(f, g, f2, null);
            float sample2 = kids2[nearest].sample(f, g, f2, sample);
            Sample sample3 = null;
            float g2 = this.k;
            for (int j = 0; j < kids2.length; j++) {
                Shape shape;
                if (j != nearest
                        && !(Shapes.boxLowerBound(shape = kids2[j], f, g, f2) >= sample2 + g2)) {
                    if (sample3 == null) {
                        sample3 = new Sample();
                    }
                    float sample4 = shape.sample(f, g, f2, sample3);
                    if (g2 > 0.0F) {
                        float h2 = Shapes.clamp01(0.5F + 0.5F * (sample4 - sample2) / g2);
                        sample2 = sample4 + (sample2 - sample4) * h2 - g2 * h2 * (1.0F - h2);
                        sample.mix(sample3, 1.0F - h2);
                    } else if (sample4 < sample2) {
                        sample2 = sample4;
                        sample.set(sample3);
                    }
                }
            }
            return sample2;
        }
    }
}
