package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CharacterModel {

    public final String id;

    public final String name;

    public final Detail detail;

    public final float scale;

    public final PartMesh[] parts;

    public final int motionSlots;

    public final int atlasWidth;

    public final int atlasHeight;

    public final int[][] atlasMips;

    public final float whiteU;

    public final float whiteV;

    public final float minY;

    public final float maxY;

    public final float hipY;

    public final boolean firstPersonArms;

    public final long buildMillis;

    CharacterModel(
            String id2,
            String name2,
            Detail detail2,
            float scale2,
            PartMesh[] parts2,
            int motionSlots2,
            int atlasWidth2,
            int atlasHeight2,
            int[][] atlasMips2,
            float whiteU2,
            float whiteV2,
            float minY2,
            float maxY2,
            float hipY2,
            boolean firstPersonArms2,
            long buildMillis2) {
        this.id = id2;
        this.name = name2;
        this.detail = detail2;
        this.scale = scale2;
        this.parts = parts2;
        this.motionSlots = motionSlots2;
        this.atlasWidth = atlasWidth2;
        this.atlasHeight = atlasHeight2;
        this.atlasMips = atlasMips2;
        this.whiteU = whiteU2;
        this.whiteV = whiteV2;
        this.minY = minY2;
        this.maxY = maxY2;
        this.hipY = hipY2;
        this.firstPersonArms = firstPersonArms2;
        this.buildMillis = buildMillis2;
    }

    public int triangles() {
        int i = 0;
        for (PartMesh part : this.parts) {
            i += part.triangles();
        }
        return i;
    }

    public PartMesh part(Bone bone) {
        for (PartMesh part : this.parts) {
            if (!part.extra && part.bone == bone) {
                return part;
            }
        }
        return null;
    }
}
