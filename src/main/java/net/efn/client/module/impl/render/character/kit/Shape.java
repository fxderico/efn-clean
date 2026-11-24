package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
public abstract class Shape {

    static final float FAR = 100000.0F;

    float minX = -100000.0F;

    float minY = -100000.0F;

    float minZ = -100000.0F;

    float maxX = 100000.0F;

    float maxY = 100000.0F;

    float maxZ = 100000.0F;

    Paint paint;

    protected Shape() {}

    public abstract float distance(float f, float g, float h);

    float distance(float f, float g, float h, float f2) {
        return this.distance(f, g, h);
    }

    float shade(float f, float g, float h, Sample sample) {
        float distance = this.distance(f, g, h);
        Paint.DEFAULT.apply(f, g, h, sample);
        return distance;
    }

    public final float sample(float f, float g, float h, Sample sample) {
        if (this.paint != null) {
            float distance = this.distance(f, g, h);
            this.paint.apply(f, g, h, sample);
            return distance;
        } else {
            return this.shade(f, g, h, sample);
        }
    }

    protected final void bounds(float f, float g, float h, float f2, float g2, float h2) {
        this.minX = Math.min(f, f2);
        this.minY = Math.min(g, g2);
        this.minZ = Math.min(h, h2);
        this.maxX = Math.max(f, f2);
        this.maxY = Math.max(g, g2);
        this.maxZ = Math.max(h, h2);
    }

    final void boundsOf(Shape shape, float f) {
        if (!shape.bounded()) {
            this.minZ = -100000.0F;
            this.minY = -100000.0F;
            this.minX = -100000.0F;
            this.maxZ = 100000.0F;
            this.maxY = 100000.0F;
            this.maxX = 100000.0F;
        } else {
            this.bounds(
                    shape.minX - f,
                    shape.minY - f,
                    shape.minZ - f,
                    shape.maxX + f,
                    shape.maxY + f,
                    shape.maxZ + f);
        }
    }

    public final boolean bounded() {
        return this.maxX - this.minX < 100000.0F
                && this.maxY - this.minY < 100000.0F
                && this.maxZ - this.minZ < 100000.0F;
    }

    public final float minX() {
        return this.minX;
    }

    public final float minY() {
        return this.minY;
    }

    public final float minZ() {
        return this.minZ;
    }

    public final float maxX() {
        return this.maxX;
    }

    public final float maxY() {
        return this.maxY;
    }

    public final float maxZ() {
        return this.maxZ;
    }

    Shape prune(float f, float g, float h, float f2, float g2, float h2) {
        return this;
    }

    Shape map(UnaryOperator<Shape> unaryOperator) {
        return unaryOperator.apply(this);
    }

    final Shape inherit(Shape shape) {
        if (shape.paint != null && this.paint == null) {
            this.paint = shape.paint;
        }
        return this;
    }

    final float boxGap(float f, float g, float h, float f2, float g2, float h2) {
        float f3 = Math.max(0.0F, Math.max(this.minX - f2, f - this.maxX));
        float g3 = Math.max(0.0F, Math.max(this.minY - g2, g - this.maxY));
        float h3 = Math.max(0.0F, Math.max(this.minZ - h2, h - this.maxZ));
        return (float) Math.sqrt(f3 * f3 + g3 * g3 + h3 * h3);
    }

    public Shape color(int i) {
        this.paint = Paint.of(i);
        return this;
    }

    public Shape paint(Paint paint2) {
        this.paint = paint2;
        return this;
    }

    public Shape move(float f, float g, float h) {
        return new Shapes.Translate(this, f, g, h);
    }

    public Shape rotate(float f, float g, float h) {
        return this.rotate(f, g, h, 0.0F, 0.0F, 0.0F);
    }

    public Shape rotate(float f, float g, float h, float f2, float g2, float h2) {
        return new Shapes.Rotate(this, Mat3.euler(f, g, h), f2, g2, h2);
    }

    public Shape rotateAxis(float f, float g, float h, float f2, float g2, float h2, float f3) {
        return new Shapes.Rotate(this, Mat3.about(f, g, h, f2), g2, h2, f3);
    }

    public Shape scale(float f) {
        return this.scale(f, 0.0F, 0.0F, 0.0F);
    }

    public Shape scale(float f, float g, float h, float f2) {
        return new Shapes.Scale(this, f, g, h, f2);
    }

    public Shape mirrorX() {
        return new Shapes.Mirror(this, true);
    }

    public Shape mirrored() {
        return new Shapes.Mirror(this, false);
    }

    public Shape inflate(float f) {
        return new Shapes.Round(this, f);
    }

    public Shape shell(float f) {
        return new Shapes.Shell(this, f);
    }

    public Shape roughen(float f, float g) {
        return new Shapes.Roughen(this, f, g);
    }

    public Shape union(Shape... shape) {
        return Shapes.union(this.prepend(shape));
    }

    public Shape blend(float f, Shape... shape) {
        return Shapes.blend(f, this.prepend(shape));
    }

    public Shape subtract(Shape shape) {
        return Shapes.subtract(this, shape);
    }

    public Shape subtract(float f, Shape shape) {
        return Shapes.subtract(f, this, shape);
    }

    public Shape intersect(Shape shape) {
        return Shapes.intersect(this, shape);
    }

    public Shape intersect(float f, Shape shape) {
        return Shapes.intersect(f, this, shape);
    }

    private Shape[] prepend(Shape[] shapes) {
        Shape[] shapes2 = new Shape[shapes.length + 1];
        shapes2[0] = this;
        System.arraycopy(shapes, 0, shapes2, 1, shapes.length);
        return shapes2;
    }
}
