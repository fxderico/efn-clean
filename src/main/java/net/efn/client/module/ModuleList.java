package net.efn.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
final class ModuleList extends AbstractList<ClientModule> implements RandomAccess {

    static final ModuleList EMPTY = new ModuleList(new ClientModule[0]);

    private final ClientModule[] modules;

    private ModuleList(ClientModule[] modules2) {
        this.modules = modules2;
    }

    static ModuleList of(Collection<ClientModule> collection) {
        return collection.isEmpty()
                ? EMPTY
                : new ModuleList(collection.toArray(new ClientModule[0]));
    }

    ModuleList with(ClientModule module) {
        ClientModule[] clientModules = Arrays.copyOf(this.modules, this.modules.length + 1);
        clientModules[this.modules.length] = module;
        return new ModuleList(clientModules);
    }

    ModuleList without(ClientModule module) {
        int indexOfIdentity = this.indexOfIdentity(module);
        if (indexOfIdentity < 0) {
            return this;
        } else if (this.modules.length == 1) {
            return EMPTY;
        } else {
            ClientModule[] clientModules = new ClientModule[this.modules.length - 1];
            System.arraycopy(this.modules, 0, clientModules, 0, indexOfIdentity);
            System.arraycopy(
                    this.modules,
                    indexOfIdentity + 1,
                    clientModules,
                    indexOfIdentity,
                    this.modules.length - indexOfIdentity - 1);
            return new ModuleList(clientModules);
        }
    }

    boolean containsIdentity(ClientModule module) {
        return this.indexOfIdentity(module) >= 0;
    }

    private int indexOfIdentity(ClientModule module) {
        for (int i = 0; i < this.modules.length; i++) {
            if (this.modules[i] == module) {
                return i;
            }
        }
        return -1;
    }

    public ClientModule get(int i) {
        return this.modules[i];
    }

    @Override
    public int size() {
        return this.modules.length;
    }

    @Override
    public Iterator<ClientModule> iterator() {
        return new Iterator<ClientModule>() {

            private int next;

            @Override
            public boolean hasNext() {
                return this.next < ModuleList.this.modules.length;
            }

            public ClientModule next() {
                if (this.next >= ModuleList.this.modules.length) {
                    throw new NoSuchElementException();
                } else {
                    return ModuleList.this.modules[this.next++];
                }
            }
        };
    }

    @Override
    public void forEach(Consumer<? super ClientModule> consumer) {
        for (ClientModule module : this.modules) {
            consumer.accept(module);
        }
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(this.modules, this.modules.length, Object[].class);
    }
}
