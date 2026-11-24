package net.efn.client.module;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class ModuleRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final List<ModuleProvider> PROVIDERS = List.of(new BuiltinModuleProvider());

    private static final List<ClientModule> STAGED = new ArrayList<>();

    private static final Comparator<ClientModule> ALPHABETICAL =
            Comparator.comparing(ClientModule::name, String.CASE_INSENSITIVE_ORDER);

    private static volatile ModuleList modules;

    private static final Map<String, ClientModule> byName = new ConcurrentHashMap<>();

    private static final Map<String, ClientModule> byId = new ConcurrentHashMap<>();

    private static volatile Reference2ReferenceOpenHashMap<Class<?>, ClientModule> byClass;

    private static volatile Reference2ReferenceOpenHashMap<ModuleCategory, ModuleList> byCategory;

    private static volatile ModuleList enabled = ModuleList.EMPTY;

    private static volatile ModuleRegistry.TypeIndex types =
            new ModuleRegistry.TypeIndex(List.of());

    private static int revision;

    private ModuleRegistry() {}

    public static List<ClientModule> all() {
        ModuleList moduleList = modules;
        if (moduleList == null) {
            freeze();
            moduleList = modules;
        }
        return moduleList;
    }

    public static ClientModule byName(String string) {
        all();
        return byName.get(string);
    }

    public static ClientModule resolve(String string) {
        all();
        ClientModule module = byId.get(string);
        return module != null ? module : byName.get(string);
    }

    public static <T extends ClientModule> T get(Class<T> classValue) {
        Reference2ReferenceOpenHashMap reference2ReferenceOpenHashMap = byClass;
        if (reference2ReferenceOpenHashMap == null) {
            all();
            reference2ReferenceOpenHashMap = byClass;
        }
        return (T) reference2ReferenceOpenHashMap.get(classValue);
    }

    public static List<ClientModule> inCategory(ModuleCategory moduleCategory) {
        all();
        return category(moduleCategory);
    }

    public static List<ClientModule> enabled() {
        all();
        return enabled;
    }

    public static <T> List<T> instancesOf(Class<T> classValue) {
        List list2 = all();
        ModuleRegistry.TypeIndex typeIndex = types;
        if (typeIndex.source() != list2) {
            types = typeIndex = new ModuleRegistry.TypeIndex(list2);
        }
        List list3;
        if ((list3 = typeIndex.lists().get(classValue)) == null) {
            ArrayList list4 = new ArrayList();
            for (ClientModule module : (Iterable<ClientModule>) list2) {
                if (classValue.isInstance(module)) {
                    list4.add(module);
                }
            }
            list3 = List.copyOf(list4);
            typeIndex.lists().put(classValue, list3);
        }
        return list3;
    }

    public static int revision() {
        all();
        return revision;
    }

    public static synchronized boolean add(ClientModule module) {
        if (modules != null) {
            if (!byId.containsKey(module.id()) && !byName.containsKey(module.name())) {
                module.captureDefaults();
                byName.put(module.name(), module);
                byId.put(module.id(), module);
                if (!byClass.containsKey(module.getClass())) {
                    Reference2ReferenceOpenHashMap reference2ReferenceOpenHashMap = byClass.clone();
                    reference2ReferenceOpenHashMap.put(module.getClass(), module);
                    byClass = reference2ReferenceOpenHashMap;
                }
                replaceCategory(
                        module.category(), alphabetical(category(module.category()).with(module)));
                module.registered = true;
                if (module.isEnabled()) {
                    enabled = enabled.with(module);
                }
                publish(modules.with(module));
                return true;
            } else {
                return false;
            }
        } else {
            for (ClientModule module2 : STAGED) {
                if (module2.id().equals(module.id()) || module2.name().equals(module.name())) {
                    return false;
                }
            }
            STAGED.add(module);
            return true;
        }
    }

    public static synchronized void remove(ClientModule module) {
        all();
        if (byId.get(module.id()) == module) {
            ModuleList moduleList = modules.without(module);
            byName.remove(module.name(), module);
            byId.remove(module.id(), module);
            if (byClass.get(module.getClass()) == module) {
                Reference2ReferenceOpenHashMap reference2ReferenceOpenHashMap = byClass.clone();
                reference2ReferenceOpenHashMap.remove(module.getClass());
                for (ClientModule module2 : moduleList) {
                    if (module2.getClass() == module.getClass()) {
                        reference2ReferenceOpenHashMap.put(module2.getClass(), module2);
                        break;
                    }
                }
                byClass = reference2ReferenceOpenHashMap;
            }
            replaceCategory(module.category(), category(module.category()).without(module));
            module.registered = false;
            enabled = enabled.without(module);
            publish(moduleList);
        }
    }

    static synchronized void switched(ClientModule module) {
        if (module.registered) {
            if (module.isEnabled()) {
                if (!enabled.containsIdentity(module)) {
                    enabled = enabled.with(module);
                }
            } else {
                enabled = enabled.without(module);
            }
        }
    }

    private static synchronized void freeze() {
        if (modules == null) {
            ArrayList list2 = new ArrayList();
            HashSet set = new HashSet();
            HashSet set2 = new HashSet();
            for (ModuleProvider moduleProvider : PROVIDERS) {
                int i = list2.size();
                for (ClientModule module : moduleProvider.createModules()) {
                    if (set2.contains(module.id())) {
                        throw new IllegalStateException("Duplicate module id: " + module.id());
                    }
                    if (!set.add(module.name())) {
                        LOGGER.warn(
                                "[EFN] Duplicate module name '{}' from provider '{}' \u2014"
                                        + " skipped",
                                module.name(),
                                moduleProvider.id());
                    } else {
                        module.captureDefaults();
                        list2.add(module);
                        set2.add(module.id());
                    }
                }
                LOGGER.info(
                        "[EFN] Module provider '{}' contributed {} modules",
                        moduleProvider.id(),
                        list2.size() - i);
            }
            int j = list2.size();
            for (ClientModule module2 : STAGED) {
                if (!set2.contains(module2.id()) && set.add(module2.name())) {
                    module2.captureDefaults();
                    list2.add(module2);
                    set2.add(module2.id());
                } else {
                    LOGGER.warn(
                            "[EFN] Module '{}' ({}) clashes with a provided module's name or id"
                                    + " \u2014 skipped",
                            module2.name(),
                            module2.id());
                }
            }
            if (!STAGED.isEmpty()) {
                LOGGER.info("[EFN] Addons contributed {} modules", list2.size() - j);
            }
            STAGED.clear();
            Reference2ReferenceOpenHashMap reference2ReferenceOpenHashMap =
                    new Reference2ReferenceOpenHashMap();
            LinkedHashMap map = new LinkedHashMap();
            ArrayList list3 = new ArrayList();
            for (ClientModule module3 : (Iterable<ClientModule>) list2) {
                byName.put(module3.name(), module3);
                byId.put(module3.id(), module3);
                reference2ReferenceOpenHashMap.putIfAbsent(module3.getClass(), module3);
                ((java.util.List)
                                map.computeIfAbsent(
                                        module3.category(), moduleCategory -> new ArrayList<>()))
                        .add(module3);
                module3.registered = true;
                if (module3.isEnabled()) {
                    list3.add(module3);
                }
            }
            Reference2ReferenceOpenHashMap reference2ReferenceOpenHashMap2 =
                    new Reference2ReferenceOpenHashMap();
            map.forEach(
                    (moduleCategory, list) ->
                            reference2ReferenceOpenHashMap2.put(
                                    moduleCategory, alphabetical((List<ClientModule>) list)));
            byClass = reference2ReferenceOpenHashMap;
            byCategory = reference2ReferenceOpenHashMap2;
            enabled = ModuleList.of(list3);
            publish(ModuleList.of(list2));
        }
    }

    private static void publish(ModuleList moduleList) {
        revision++;
        ModuleRevision.changed();
        modules = moduleList;
    }

    private static ModuleList alphabetical(List<ClientModule> list2) {
        ArrayList list3 = new ArrayList(list2);
        list3.sort(ALPHABETICAL);
        return ModuleList.of(list3);
    }

    private static ModuleList category(ModuleCategory moduleCategory) {
        ModuleList moduleList = byCategory.get(moduleCategory);
        return moduleList != null ? moduleList : ModuleList.EMPTY;
    }

    private static void replaceCategory(ModuleCategory moduleCategory, ModuleList moduleList) {
        Reference2ReferenceOpenHashMap reference2ReferenceOpenHashMap = byCategory.clone();
        if (moduleList.isEmpty()) {
            reference2ReferenceOpenHashMap.remove(moduleCategory);
        } else {
            reference2ReferenceOpenHashMap.put(moduleCategory, moduleList);
        }
        byCategory = reference2ReferenceOpenHashMap;
    }

    @Environment(EnvType.CLIENT)
    private record TypeIndex(List<ClientModule> source, Map<Class<?>, List<?>> lists) {

        TypeIndex(List<ClientModule> list) {
            this(list, new ConcurrentHashMap<>());
        }
    }
}
