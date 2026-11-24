package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PartMesh {

    public final String name;

    public final Bone bone;

    public final int parent;

    public final boolean extra;

    public final float pivotX;

    public final float pivotY;

    public final float pivotZ;

    public final float gripX;

    public final float gripY;

    public final float gripZ;

    public final Motion motion;

    public final Surface base;

    public final Surface outline;

    public final Surface decals;

    public final int motionSlot;

    public final float swingScale;

    public final float swingLimit;

    public final boolean jointed;

    public final float jointX;

    public final float jointY;

    public final float jointZ;

    public final boolean neck;

    public final float lookPitch;

    public final float lookYaw;

    public final float lookLimit;

    PartMesh(
            String name2,
            Bone bone2,
            int parent2,
            boolean extra2,
            float pivotX2,
            float pivotY2,
            float pivotZ2,
            float gripX2,
            float gripY2,
            float gripZ2,
            Motion motion2,
            Surface base2,
            Surface outline2,
            Surface decals2,
            int motionSlot2,
            float swingScale2,
            float swingLimit2,
            boolean jointed2,
            float jointX2,
            float jointY2,
            float jointZ2,
            boolean neck2,
            float lookPitch2,
            float lookYaw2,
            float lookLimit2) {
        this.name = name2;
        this.bone = bone2;
        this.parent = parent2;
        this.extra = extra2;
        this.pivotX = pivotX2;
        this.pivotY = pivotY2;
        this.pivotZ = pivotZ2;
        this.gripX = gripX2;
        this.gripY = gripY2;
        this.gripZ = gripZ2;
        this.motion = motion2;
        this.base = base2;
        this.outline = outline2;
        this.decals = decals2;
        this.motionSlot = motionSlot2;
        this.swingScale = swingScale2;
        this.swingLimit = swingLimit2;
        this.jointed = jointed2;
        this.jointX = jointX2;
        this.jointY = jointY2;
        this.jointZ = jointZ2;
        this.neck = neck2;
        this.lookPitch = lookPitch2;
        this.lookYaw = lookYaw2;
        this.lookLimit = lookLimit2;
    }

    public boolean bends() {
        return this.motion != null && this.motion.bendPower() > 0.0F;
    }

    public boolean swings() {
        return this.swingScale != 1.0F || this.swingLimit > 0.0F;
    }

    public float swingPitch(float f, float g, float h) {
        if (h > 0.0F && this.swings()) {
            float soft = Motion.soft(g * this.swingScale, this.swingLimit);
            return f + Math.min(1.0F, h) * (soft - g);
        } else {
            return f;
        }
    }

    public int triangles() {
        return (this.base == null ? 0 : this.base.triangles)
                + (this.outline == null ? 0 : this.outline.triangles)
                + (this.decals == null ? 0 : this.decals.triangles);
    }
}
