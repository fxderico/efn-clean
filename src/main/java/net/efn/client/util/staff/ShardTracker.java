package net.efn.client.util.staff;

import net.efn.client.gui.DonutGoliathMapData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class ShardTracker {

    private static final int OFF_MAP = -2147483648;

    private ClientWorld level;

    private ClientPlayerEntity self;

    private String brand;

    private int goliath = -2147483648;

    public ShardTracker() {}

    public boolean moved(MinecraftClient client) {
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world != null && player != null) {
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            String string = networkHandler == null ? null : networkHandler.getBrand();
            int goliath2 = goliath(world, player);
            boolean bl =
                    this.level != null
                            && (world != this.level
                                    || player != this.self && !this.self.isDead()
                                    || goliath2 != this.goliath
                                    || string != null
                                            && this.brand != null
                                            && !string.equals(this.brand));
            this.level = world;
            this.self = player;
            this.brand = string;
            this.goliath = goliath2;
            return bl;
        } else {
            this.clear();
            return false;
        }
    }

    public void clear() {
        this.level = null;
        this.self = null;
        this.brand = null;
        this.goliath = -2147483648;
    }

    private static int goliath(ClientWorld world, ClientPlayerEntity player) {
        return world.getRegistryKey() != World.OVERWORLD
                ? -2147483648
                : DonutGoliathMapData.goliathAt(player.getX(), player.getZ());
    }
}
