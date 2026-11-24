package net.efn.client.module.impl.misc;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SlowSwingModule extends ClientModule {

    private final NumberSetting speed =
            this.addSetting("speed", new NumberSetting("Speed", 0.25F, 0.1F, 2.0F).step(0.05F));

    public SlowSwingModule() {
        super(
                "slow-swing",
                "Slow Swing",
                "Control hand swing animation speed.",
                ModuleCategory.MISC);
    }

    public int getModifiedSwingDuration(int i) {
        float speed2 = this.speed.get();
        if (speed2 <= 0.0F) {
            speed2 = 0.1F;
        }
        int j = Math.round(i / speed2);
        return Math.max(1, j);
    }
}
