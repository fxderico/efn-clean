package net.efn.client.module.impl.misc;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TridentUtilModule extends ClientModule {

    private final NumberSetting chargeScale =
            this.addSetting(
                    "scale", new NumberSetting("Charge Scale", 0.0F, 0.0F, 1.0F).step(0.1F));

    private final BooleanSetting noWater =
            this.addSetting("no-water", new BooleanSetting("No Water", true));

    public TridentUtilModule() {
        super(
                "trident-util",
                "Trident Util",
                "Use tridents out of water and charge them faster.",
                ModuleCategory.MOVEMENT);
    }

    private static TridentUtilModule active() {
        TridentUtilModule module = ModuleRegistry.get(TridentUtilModule.class);
        return module != null && module.isEnabled() ? module : null;
    }

    public static boolean allowOutOfWater() {
        TridentUtilModule module = active();
        return module != null && module.noWater.get();
    }

    public static int minChargeTicks(int i) {
        TridentUtilModule module = active();
        return module == null ? i : Math.max(1, Math.round(i * module.chargeScale.get()));
    }
}
