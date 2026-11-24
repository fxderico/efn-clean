package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.Lights;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting.Type;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType.CardinalLightType;

import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class CharacterLighting {

    private static final Vector3f LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();

    private static final Vector3f LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();

    private static final Lights NETHER = new Lights(0.2F, 1.0F, -0.7F, -0.2F, -1.0F, 0.7F);

    private static final Lights INVENTORY = new Lights(0.2F, -1.0F, 1.0F, -0.2F, -1.0F, 0.0F);

    private static final Lights ITEMS_FLAT =
            transformed(new Matrix4f().rotationY(-0.3926991F).rotateX(2.3561945F));

    private static final Lights ITEMS_3D =
            transformed(
                    new Matrix4f()
                            .scaling(1.0F, -1.0F, 1.0F)
                            .rotateYXZ(1.0821041F, 3.2375858F, 0.0F)
                            .rotateYXZ(-0.3926991F, 2.3561945F, 0.0F));

    private static Type entry = Type.LEVEL;

    private CharacterLighting() {}

    public static void setup(Type type) {
        entry = type;
    }

    static Lights current() {
        return switch (entry) {
            case LEVEL -> nether() ? NETHER : Lights.WORLD;
            case ITEMS_FLAT -> ITEMS_FLAT;
            case ITEMS_3D -> ITEMS_3D;
            case ENTITY_IN_UI, PLAYER_SKIN -> INVENTORY;
        };
    }

    private static boolean nether() {
        ClientWorld world = MinecraftClient.getInstance().world;
        return world != null
                && world.getDimension().cardinalLightType() == CardinalLightType.NETHER;
    }

    private static Lights transformed(Matrix4f matrix) {
        Vector3f vec = matrix.transformDirection(LIGHT_0, new Vector3f());
        Vector3f vec2 = matrix.transformDirection(LIGHT_1, new Vector3f());
        return new Lights(vec.x, vec.y, vec.z, vec2.x, vec2.y, vec2.z);
    }
}
