package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class WornCharacter {

    final CharacterModel model;

    final RenderLayer solid;

    final RenderLayer decal;

    final RenderLayer ghost;

    WornCharacter(CharacterModel model2, Identifier id) {
        this.model = model2;
        this.solid = RenderLayers.entitySolid(id);
        this.decal = RenderLayers.entityTranslucent(id);
        this.ghost = RenderLayers.itemEntityTranslucentCull(id);
    }
}
