package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class Rig {

    float scale = 1.0F;

    int outlineColor = -14017512;

    float outline = 0.2F;

    float occlusion = 0.55F;

    float flat;

    boolean firstPersonArms = true;

    final Part[] bones = new Part[Bone.values().length];

    final List<Part> extras = new ArrayList<>();

    public Rig() {}

    public Rig scale(float scale2) {
        if (scale2 > 0.05F && scale2 < 8.0F) {
            this.scale = scale2;
            return this;
        } else {
            throw new IllegalArgumentException("scale must be between 0.05 and 8");
        }
    }

    public Rig outline(int outlineColor2, float f) {
        this.outlineColor = outlineColor2;
        this.outline = Math.max(0.0F, f);
        return this;
    }

    public Rig occlusion(float f) {
        this.occlusion = Math.max(0.0F, Math.min(1.0F, f));
        return this;
    }

    public Rig flatLight(float f) {
        this.flat = Math.max(0.0F, Math.min(1.0F, f));
        return this;
    }

    public Rig firstPersonArms(boolean firstPersonArms2) {
        this.firstPersonArms = firstPersonArms2;
        return this;
    }

    public Part part(Bone bone) {
        Part bone2 = this.bones[bone.ordinal()];
        if (bone2 == null) {
            this.bones[bone.ordinal()] = bone2 = new Part(bone.name(), bone, null, false);
        }
        return bone2;
    }

    public Part extra(String string, Bone bone) {
        return this.extra(string, this.part(bone));
    }

    public Part extra(String string, Part part) {
        if (string != null && !string.isEmpty()) {
            for (Part extra : this.extras) {
                if (extra.name.equals(string)) {
                    throw new IllegalArgumentException("duplicate extra name: " + string);
                }
            }
            for (Bone bone : Bone.values()) {
                if (bone.name().equals(string)) {
                    throw new IllegalArgumentException("extra name clashes with a bone: " + string);
                }
            }
            Part part2 = new Part(string, part.bone, part, true);
            this.extras.add(part2);
            return part2;
        } else {
            throw new IllegalArgumentException("extra needs a name");
        }
    }

    public float scale() {
        return this.scale;
    }
}
