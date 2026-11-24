package net.efn.client.input;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRevision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntPredicate;

@Environment(EnvType.CLIENT)
public final class ModuleKeybinds {

    public static final int MOUSE_OFFSET = 1000;

    private static final int KEY_COUNT = 1008;

    private final boolean[] previous = new boolean[1008];

    private final boolean[] current = new boolean[1008];

    private final boolean[] sampled = new boolean[1008];

    private int[] sampledKeys = new int[1008];

    private int[] previousKeys = new int[1008];

    private int sampledCount;

    private int previousCount;

    private final Map<ClientModule, Integer> held = new IdentityHashMap<>();

    private boolean wasAcceptingInput = true;

    private final List<ClientModule> bound = new ArrayList<>();

    private List<ClientModule> boundFrom;

    private long boundRevision;

    public ModuleKeybinds() {}

    public void update(
            List<ClientModule> list, IntPredicate intPredicate, boolean wasAcceptingInput2) {
        List list2 = this.boundModules(list);
        for (int i = 0; i < this.previousCount; i++) {
            int previousKey = this.previousKeys[i];
            this.current[previousKey] = false;
            this.sampled[previousKey] = false;
        }
        this.sampledCount = 0;
        for (ClientModule module : (Iterable<ClientModule>) list2) {
            int keybind = module.keybind();
            if (!this.sampled[keybind]) {
                this.current[keybind] = intPredicate.test(keybind);
                this.sampled[keybind] = true;
                this.sampledKeys[this.sampledCount++] = keybind;
            }
        }
        if (!this.held.isEmpty()) {
            Iterator iterator = this.held.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                ClientModule key = (ClientModule) entry.getKey();
                int entry2 = (Integer) entry.getValue();
                if (!wasAcceptingInput2
                        || !key.isHoldMode()
                        || key.keybind() != entry2
                        || !this.current[entry2]) {
                    iterator.remove();
                    key.setEnabled(false);
                }
            }
        }
        if (wasAcceptingInput2 && this.wasAcceptingInput) {
            for (ClientModule module2 : (Iterable<ClientModule>) list2) {
                int keybind2 = module2.keybind();
                if (isBindable(keybind2) && this.current[keybind2] && !this.previous[keybind2]) {
                    if (module2.isAction()) {
                        module2.onClick();
                    } else if (module2.isHoldMode()) {
                        module2.setEnabled(true);
                        if (module2.isEnabled()) {
                            this.held.put(module2, keybind2);
                        }
                    } else {
                        module2.toggle();
                    }
                }
            }
        }
        for (int o = 0; o < this.previousCount; o++) {
            this.previous[this.previousKeys[o]] = false;
        }
        for (int p = 0; p < this.sampledCount; p++) {
            this.previous[this.sampledKeys[p]] = this.current[this.sampledKeys[p]];
        }
        int[] previousKeys2 = this.previousKeys;
        this.previousKeys = this.sampledKeys;
        this.sampledKeys = previousKeys2;
        this.previousCount = this.sampledCount;
        this.wasAcceptingInput = wasAcceptingInput2;
    }

    private List<ClientModule> boundModules(List<ClientModule> boundFrom2) {
        long l = ModuleRevision.current();
        if (boundFrom2 != this.boundFrom || l != this.boundRevision) {
            this.bound.clear();
            for (ClientModule module : boundFrom2) {
                if (isBindable(module.keybind())) {
                    this.bound.add(module);
                }
            }
            this.boundFrom = boundFrom2;
            this.boundRevision = l;
        }
        return this.bound;
    }

    public static boolean isBindable(int i) {
        return i >= 32 && i <= 348 || i >= 1000 && i < 1008;
    }
}
