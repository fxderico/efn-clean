package net.efn.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
public interface ModuleProvider {
    String id();

    List<ClientModule> createModules();
}
