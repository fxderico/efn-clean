package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Environment(EnvType.CLIENT)
public final class BakedDesign extends CharacterDesign {

    private final BakedDesign.Source source;

    public BakedDesign(String string, String string2, BakedDesign.Source source2) {
        super(string, string2);
        this.source = source2;
    }

    public static BakedDesign builtIn(String string, String string2, String string3) {
        return new BakedDesign(
                string,
                string2,
                () -> {
                    InputStream resourceAsStream = BakedDesign.class.getResourceAsStream(string3);
                    if (resourceAsStream == null) {
                        throw new FileNotFoundException(string3);
                    } else {
                        return resourceAsStream;
                    }
                });
    }

    InputStream open() throws IOException {
        return this.source.open();
    }

    @Override
    protected void define(Rig rig) {
        throw new UnsupportedOperationException(this.id() + " is baked and has no rig");
    }

    @Environment(EnvType.CLIENT)
    public interface Source {

        InputStream open() throws IOException;
    }
}
