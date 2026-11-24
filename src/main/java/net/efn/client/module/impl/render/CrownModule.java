package net.efn.client.module.impl.render;

import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class CrownModule extends HeadCosmeticModule {

    private static final String STYLE_CROWN = "Crown";

    private static final String STYLE_HALO = "Halo";

    private static final String STYLE_SHARDS = "Shards";

    private final ModeSetting style =
            this.addSetting("style", new ModeSetting("Style", "Crown", "Crown", "Halo", "Shards"));

    private final NumberSetting size =
            this.addSetting("size", new NumberSetting("Size", 1.0F, 0.8F, 1.3F).step(0.05F));

    private final ModeSetting color =
            this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting gold =
            this.addSetting("manual-gold", new ColorSetting("Main Color", -7688204));

    private final ColorSetting inlay =
            this.addSetting("manual-inlay", new ColorSetting("Trim Color", -13738055));

    private final ColorSetting gem =
            this.addSetting("manual-gem", new ColorSetting("Gem Color", -10313473));

    private final BooleanSetting gemGlow =
            this.addSetting("gem-glow", new BooleanSetting("Gem Glow", true));

    private final BooleanSetting glint =
            this.addSetting("glint", new BooleanSetting("Glint", true));

    private final BooleanSetting friends =
            this.addSetting("friends", new BooleanSetting("On Friends", false));

    private final CrownMesh[] meshes = new CrownMesh[4];

    private long styleRevision = -1L;

    public CrownModule() {
        super(
                "efn-crown",
                "EFN Crown",
                "Puts a glowing EFN crown, halo or crystal shards on your player model.");
        this.gold.visibleWhen(() -> this.color.is("Manual"));
        this.inlay.visibleWhen(() -> this.color.is("Manual"));
        this.gem.visibleWhen(() -> this.color.is("Manual"));
    }

    @Override
    void reset() {
        super.reset();
        Arrays.fill(this.meshes, null);
        this.styleRevision = -1L;
    }

    @Override
    CrownMesh headMesh(boolean bl) {
        this.refreshMeshes();
        int i = bl ? 1 : 0;
        CrownMesh mesh = this.meshes[i];
        if (mesh == null) {
            this.meshes[i] = mesh = CrownMesh.head(this.styleIndex(), 0, bl);
        }
        return mesh;
    }

    @Override
    CrownMesh bodyMesh(boolean bl) {
        this.refreshMeshes();
        int i = bl ? 3 : 2;
        CrownMesh mesh = this.meshes[i];
        if (mesh == null) {
            this.meshes[i] = mesh = CrownMesh.broadCollar(bl, false);
        }
        return mesh;
    }

    private void refreshMeshes() {
        long revision = this.style.revision();
        if (revision != this.styleRevision) {
            this.styleRevision = revision;
            Arrays.fill(this.meshes, null);
        }
    }

    private int styleIndex() {
        if (this.style.is("Halo")) {
            return 1;
        } else {
            return this.style.is("Shards") ? 2 : 0;
        }
    }

    @Override
    float scale() {
        return this.size.get();
    }

    @Override
    int[] palette() {
        int[] ints = new int[5];
        if (this.color.is("Manual")) {
            int i;
            ints[0] = i = this.gold.argb() | 0xFF000000;
            ints[1] = this.inlay.argb() | 0xFF000000;
            ints[2] = this.gem.argb() | 0xFF000000;
            ints[3] = bronze(i);
        } else {
            int j = CosmeticAccent.argb();
            int metal;
            ints[0] = metal = metal(j);
            ints[1] = ColorHelper.scaleRgb(j, 0.78F);
            ints[2] = gem(j);
            ints[3] = ColorHelper.scaleRgb(metal, 0.62F);
        }
        return ints;
    }

    @Override
    boolean glows() {
        return this.gemGlow.get();
    }

    @Override
    boolean glints() {
        return this.glint.get();
    }

    @Override
    boolean onFriends() {
        return this.friends.get();
    }

    private static int bronze(int i) {
        int j = (int) ((i >> 16 & 0xFF) * 0.7F);
        int k = (int) ((i >> 8 & 0xFF) * 0.55F);
        int m = (int) ((i & 0xFF) * 0.4F);
        return 0xFF000000 | j << 16 | k << 8 | m;
    }
}
