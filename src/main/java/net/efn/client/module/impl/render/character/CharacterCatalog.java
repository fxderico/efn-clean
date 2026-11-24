/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.designs.Aoi;
import net.efn.client.module.impl.render.character.designs.Luna;
import net.efn.client.module.impl.render.character.designs.Mika;
import net.efn.client.module.impl.render.character.designs.Miku;
import net.efn.client.module.impl.render.character.designs.Sakura;
import net.efn.client.module.impl.render.character.designs.Tralalero;
import net.efn.client.module.impl.render.character.designs.TungTungSahur;
import net.efn.client.module.impl.render.character.kit.BakedDesign;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(value = EnvType.CLIENT)
public final class CharacterCatalog {

    private static final List<CharacterDesign> BUILT_IN =
            List.of(
                    new Sakura(),
                    new Luna(),
                    new Mika(),
                    new Aoi(),
                    new Miku(),
                    BakedDesign.builtIn("cj", "CJ", "/assets/efn/characters/cj.anc"),
                    BakedDesign.builtIn(
                            "neighbor", "Neighbor", "/assets/efn/characters/neighbor.anc"),
                    new TungTungSahur(),
                    new Tralalero());

    private static volatile List<CharacterDesign> designs = BUILT_IN;

    private CharacterCatalog() {}

    public static List<CharacterDesign> all() {
        return designs;
    }

    static boolean refresh() {
        ArrayList<CharacterDesign> list = new ArrayList<CharacterDesign>(BUILT_IN);
        ArrayList<String> list2 = new ArrayList<String>(CharacterCatalog.names(BUILT_IN));
        for (BakedDesign bakedDesign : CustomCharacters.scan()) {
            if (list2.contains(bakedDesign.name())) continue;
            list2.add(bakedDesign.name());
            list.add(bakedDesign);
        }
        List<CharacterDesign> list3 = designs;
        designs = List.copyOf(list);
        if (list3.size() != list.size()) {
            return true;
        }
        for (int i = 0; i < list.size(); ++i) {
            if (((CharacterDesign) list3.get(i)).id().equals(list.get(i).id())) continue;
            return true;
        }
        return false;
    }

    static List<String> names() {
        return CharacterCatalog.names(designs);
    }

    private static List<String> names(List<CharacterDesign> list) {
        ArrayList<String> list2 = new ArrayList<String>(list.size());
        for (CharacterDesign characterDesign : list) {
            list2.add(characterDesign.name());
        }
        return list2;
    }

    static CharacterDesign byName(String string) {
        List<CharacterDesign> list = designs;
        for (CharacterDesign characterDesign : list) {
            if (!characterDesign.name().equals(string)) continue;
            return characterDesign;
        }
        return list.get(0);
    }
}
