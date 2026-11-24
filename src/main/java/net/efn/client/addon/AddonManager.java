/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.addon;

import net.efn.client.config.ModuleConfigStore;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public final class AddonManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    public static final String ENTRYPOINT = "efn";

    private static final String CLIENT_PACKAGE = "net.efn.client";

    private static List<EfnAddon> addons;

    private static boolean configLoaded;

    private AddonManager() {}

    public static List<EfnAddon> addons() {
        List<EfnAddon> list = addons;
        return list != null ? list : List.of();
    }

    public static void init() {
        if (addons != null) {
            return;
        }
        List<EfnAddon> list = AddonManager.create();
        list.removeIf(
                efnAddon ->
                        !AddonManager.run(
                                efnAddon,
                                "register its categories",
                                EfnAddon::onRegisterCategories));
        list.removeIf(
                efnAddon -> {
                    if (AddonManager.run(efnAddon, "initialise", EfnAddon::onInitialize)) {
                        return false;
                    }
                    AddonManager.removeModules(efnAddon);
                    return true;
                });
        for (EfnAddon efnAddon2 : list) {
            LOGGER.info(
                    "[EFN] Loaded addon {} v{} by {}",
                    efnAddon2.name(),
                    efnAddon2.version(),
                    efnAddon2.authors().isEmpty()
                            ? "unknown"
                            : String.join((CharSequence) ", ", efnAddon2.authors()));
        }
        addons = List.copyOf(list);
    }

    public static void configLoaded() {
        if (addons == null || configLoaded) {
            return;
        }
        configLoaded = true;
        ArrayList<EfnAddon> list = new ArrayList<EfnAddon>(addons);
        list.removeIf(
                efnAddon -> {
                    if (AddonManager.run(efnAddon, "start", EfnAddon::onConfigLoaded)) {
                        return false;
                    }
                    AddonManager.removeModules(efnAddon);
                    return true;
                });
        addons = List.copyOf(list);
    }

    private static List<EfnAddon> create() {
        List<EntrypointContainer<EfnAddon>> entrypointContainers;
        ArrayList<EfnAddon> list = new ArrayList<EfnAddon>();
        try {
            entrypointContainers =
                    FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, EfnAddon.class);
        } catch (LinkageError | RuntimeException throwable) {
            LOGGER.error("[EFN] Could not list addons; starting without them", throwable);
            return list;
        }
        for (EntrypointContainer<EfnAddon> entrypointContainer : entrypointContainers) {
            String string;
            EfnAddon entrypoint;
            ModMetadata metadata = entrypointContainer.getProvider().getMetadata();
            try {
                entrypoint = entrypointContainer.getEntrypoint();
                string = entrypoint.getPackage();
            } catch (LinkageError | RuntimeException throwable) {
                LOGGER.error(
                        "[EFN] Addon {} ({}) could not be created and is skipped",
                        metadata.getName(),
                        metadata.getId(),
                        throwable);
                continue;
            }
            entrypoint.bind(metadata);
            String packageProblem = AddonManager.packageProblem(entrypoint, string, list);
            if (packageProblem != null) {
                LOGGER.error(
                        "[EFN] Addon {} is skipped: {}",
                        (Object) entrypoint.name(),
                        (Object) packageProblem);
                continue;
            }
            entrypoint.packageName = string;
            list.add(entrypoint);
        }
        return list;
    }

    private static String packageProblem(EfnAddon efnAddon, String string, List<EfnAddon> list) {
        if (string == null || !efnAddon.getClass().getName().startsWith(string + ".")) {
            return "getPackage() is \""
                    + string
                    + "\", which doesn't hold its class "
                    + efnAddon.getClass().getName();
        }
        if (AddonManager.overlap(string, CLIENT_PACKAGE)) {
            return "its package " + string + " overlaps EFN' own (net.efn.client)";
        }
        for (EfnAddon efnAddon2 : list) {
            if (!AddonManager.overlap(string, efnAddon2.packageName)) continue;
            return "its package "
                    + string
                    + " overlaps "
                    + efnAddon2.name()
                    + "'s ("
                    + efnAddon2.packageName
                    + "); an addon made from the template needs its package renamed";
        }
        return null;
    }

    private static boolean overlap(String string, String string2) {
        return (string + ".").startsWith(string2 + ".") || (string2 + ".").startsWith(string + ".");
    }

    private static boolean run(EfnAddon efnAddon, String string, Consumer<EfnAddon> consumer) {
        try {
            consumer.accept(efnAddon);
            return true;
        } catch (LinkageError | RuntimeException throwable) {
            LOGGER.error(
                    "[EFN] Addon {} failed to {} and is skipped{}",
                    efnAddon.name(),
                    string,
                    AddonManager.reportHint(efnAddon),
                    throwable);
            return false;
        }
    }

    private static String reportHint(EfnAddon efnAddon) {
        try {
            EfnAddon.GithubRepo repo = efnAddon.getRepo();
            Object object = repo != null ? repo.url() + "/issues" : efnAddon.getWebsite();
            return object != null ? " - report it at " + (String) object : "";
        } catch (LinkageError | RuntimeException throwable) {
            return "";
        }
    }

    private static void removeModules(EfnAddon efnAddon) {
        String string = efnAddon.packageName + ".";
        for (ClientModule module : ModuleRegistry.all()) {
            if (!module.getClass().getName().startsWith(string)) continue;
            ModuleConfigStore.getInstance().release(module);
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
            ModuleRegistry.remove(module);
        }
    }
}
