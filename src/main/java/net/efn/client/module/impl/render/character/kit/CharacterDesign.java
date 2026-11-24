package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class CharacterDesign {

    private final String id;

    private final String name;

    protected CharacterDesign(String id2, String name2) {
        if (id2 != null && id2.matches("[a-z0-9_]+")) {
            this.id = id2;
            this.name = name2;
        } else {
            throw new IllegalArgumentException("character id must match [a-z0-9_]+: " + id2);
        }
    }

    public final String id() {
        return this.id;
    }

    public final String name() {
        return this.name;
    }

    protected abstract void define(Rig rig);

    public final Rig rig() {
        Rig rig = new Rig();
        this.define(rig);
        return rig;
    }
}
