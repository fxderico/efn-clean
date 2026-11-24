package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class Part {

    final String name;

    final Bone bone;

    final Part parent;

    final boolean extra;

    Shape shape;

    float pivotX;

    float pivotY;

    float pivotZ;

    boolean pivotSet;

    float gripX;

    float gripY;

    float gripZ;

    boolean gripSet;

    int outlineColor;

    float outline = -1.0F;

    boolean outlineColorSet;

    final List<Decal> decals = new ArrayList<>();

    Motion motion;

    float detail = 1.0F;

    float resolution = 1.0F;

    float flat = -1.0F;

    float occlusion = -1.0F;

    float swing = 1.0F;

    float swingLimit;

    float jointX;

    float jointY;

    float jointZ;

    float jointDirX;

    float jointDirY = -1.0F;

    float jointDirZ;

    float jointBlend;

    boolean jointSet;

    boolean neck;

    float lookPitch;

    float lookYaw;

    float lookLimit;

    Part(String name2, Bone bone2, Part parent2, boolean extra2) {
        this.name = name2;
        this.bone = bone2;
        this.parent = parent2;
        this.extra = extra2;
        if (parent2 == null) {
            this.pivotX = bone2.pivotX;
            this.pivotY = bone2.pivotY;
            this.pivotZ = bone2.pivotZ;
        } else {
            this.pivotX = parent2.pivotX;
            this.pivotY = parent2.pivotY;
            this.pivotZ = parent2.pivotZ;
        }
    }

    public String name() {
        return this.name;
    }

    public Bone bone() {
        return this.bone;
    }

    public boolean extra() {
        return this.extra;
    }

    public Shape shape() {
        return this.shape;
    }

    public Part shape(Shape shape2) {
        this.shape = shape2;
        return this;
    }

    public Part pivot(float pivotX2, float pivotY2, float pivotZ2) {
        this.pivotX = pivotX2;
        this.pivotY = pivotY2;
        this.pivotZ = pivotZ2;
        this.pivotSet = true;
        return this;
    }

    public Vec3 pivot() {
        return new Vec3(this.pivotX, this.pivotY, this.pivotZ);
    }

    public Part grip(float gripX2, float gripY2, float gripZ2) {
        this.gripX = gripX2;
        this.gripY = gripY2;
        this.gripZ = gripZ2;
        this.gripSet = true;
        return this;
    }

    public Part outline(float f) {
        this.outline = Math.max(0.0F, f);
        return this;
    }

    public Part outline(int outlineColor2, float f) {
        this.outlineColor = outlineColor2;
        this.outlineColorSet = true;
        this.outline = Math.max(0.0F, f);
        return this;
    }

    public Part noOutline() {
        this.outline = 0.0F;
        return this;
    }

    public Part decal(Decal decal) {
        this.decals.add(decal);
        return this;
    }

    public Part motion(Motion motion2) {
        this.motion = motion2;
        return this;
    }

    public Part detail(float f) {
        this.detail = Math.max(0.05F, f);
        return this;
    }

    public Part resolution(float f) {
        this.resolution = Math.max(0.35F, Math.min(3.0F, f));
        return this;
    }

    public Part flatLight(float f) {
        this.flat = Math.max(0.0F, Math.min(1.0F, f));
        return this;
    }

    public Part occlusion(float f) {
        this.occlusion = Math.max(0.0F, Math.min(1.0F, f));
        return this;
    }

    public Part swing(float f) {
        return this.swing(f, 0.0F);
    }

    public Part swing(float f, float g) {
        if (!this.extra && (this.bone.arm() || this.bone.leg())) {
            this.swing = Math.max(0.0F, Math.min(2.0F, f));
            this.swingLimit = Math.max(0.0F, Math.min(90.0F, g));
            return this;
        } else {
            throw new IllegalStateException(
                    this.name + ": swing() only applies to the arm and leg bone parts");
        }
    }

    public Part joint(float f, float g, float h) {
        return this.joint(f, g, h, 0.7F);
    }

    public Part joint(float jointX2, float jointY2, float jointZ2, float f) {
        if (!this.extra && (this.bone.arm() || this.bone.leg())) {
            this.jointX = jointX2;
            this.jointY = jointY2;
            this.jointZ = jointZ2;
            this.jointDirX = 0.0F;
            this.jointDirY = -1.0F;
            this.jointDirZ = 0.0F;
            this.jointBlend = Math.max(0.1F, f);
            this.jointSet = true;
            this.neck = false;
            return this;
        } else {
            throw new IllegalStateException(
                    this.name + ": joint() only applies to the arm and leg bone parts");
        }
    }

    public Part neck(
            float jointX2, float jointY2, float jointZ2, float f, float g, float h, float f2) {
        if (!this.bone.arm() && !this.bone.leg()) {
            float g2 = (float) Math.sqrt(f * f + g * g + h * h);
            if (!(g2 > 1.0E-4F)) {
                throw new IllegalArgumentException(this.name + ": neck() needs a direction");
            } else {
                this.jointX = jointX2;
                this.jointY = jointY2;
                this.jointZ = jointZ2;
                this.jointDirX = f / g2;
                this.jointDirY = g / g2;
                this.jointDirZ = h / g2;
                this.jointBlend = Math.max(0.1F, f2);
                this.jointSet = true;
                this.neck = true;
                return this;
            }
        } else {
            throw new IllegalStateException(this.name + ": limbs bend with joint(), not neck()");
        }
    }

    public Part look(float lookPitch2, float lookYaw2, float f) {
        if (!this.neck) {
            throw new IllegalStateException(this.name + ": look() needs a neck()");
        } else {
            this.lookPitch = lookPitch2;
            this.lookYaw = lookYaw2;
            this.lookLimit = Math.max(0.0F, f);
            return this;
        }
    }

    public Part mirrorOf(Part part) {
        this.shape = part.shape == null ? null : part.shape.mirrored();
        this.pivotX = -part.pivotX;
        this.pivotY = part.pivotY;
        this.pivotZ = part.pivotZ;
        this.pivotSet = true;
        if (part.gripSet) {
            this.gripX = -part.gripX;
            this.gripY = part.gripY;
            this.gripZ = part.gripZ;
            this.gripSet = true;
        }
        this.outline = part.outline;
        this.outlineColor = part.outlineColor;
        this.outlineColorSet = part.outlineColorSet;
        this.decals.clear();
        for (Decal decal : part.decals) {
            this.decals.add(decal.mirrored());
        }
        this.motion = part.motion == null ? null : part.motion.mirrored();
        this.detail = part.detail;
        this.resolution = part.resolution;
        this.flat = part.flat;
        this.occlusion = part.occlusion;
        this.swing = part.swing;
        this.swingLimit = part.swingLimit;
        this.jointX = -part.jointX;
        this.jointY = part.jointY;
        this.jointZ = part.jointZ;
        this.jointDirX = -part.jointDirX;
        this.jointDirY = part.jointDirY;
        this.jointDirZ = part.jointDirZ;
        this.jointBlend = part.jointBlend;
        this.jointSet = part.jointSet;
        this.neck = part.neck;
        this.lookPitch = part.lookPitch;
        this.lookYaw = part.lookYaw;
        this.lookLimit = part.lookLimit;
        return this;
    }

    float gripX() {
        return this.gripSet ? this.gripX : this.pivotX + this.bone.gripX;
    }

    float gripY() {
        return this.gripSet ? this.gripY : this.pivotY + this.bone.gripY;
    }

    float gripZ() {
        return this.gripSet ? this.gripZ : this.pivotZ + this.bone.gripZ;
    }
}
