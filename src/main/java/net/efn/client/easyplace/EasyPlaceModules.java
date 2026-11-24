package net.efn.client.easyplace;

import net.efn.client.easyplace.builder.SchematicBuilderModule;
import net.efn.client.easyplace.module.EasyPlaceModule;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class EasyPlaceModules {

    private EasyPlaceModules() {}

    public static boolean available() {
        FabricLoader fabricLoader = FabricLoader.getInstance();
        return fabricLoader.isModLoaded("litematica") && fabricLoader.isModLoaded("malilib");
    }

    public static List<ClientModule> create() {
        return List.of(new EasyPlaceModule(), new SchematicBuilderModule());
    }
}
