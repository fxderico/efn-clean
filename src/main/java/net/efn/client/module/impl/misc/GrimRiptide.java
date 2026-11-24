package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public final class GrimRiptide {

    public static final int MIN_TICKS = 9;

    private GrimRiptide() {}

    public static boolean clientAccepts() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null && client.isOnThread()) {
            if (!player.isTouchingWaterOrRain() && !client.world.isRaining()) {
                Box box = player.getBoundingBox().expand(0.1);
                BlockPos pos = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
                BlockPos pos2 = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);
                for (BlockPos pos3 : BlockPos.iterate(pos, pos2)) {
                    BlockState blockState = client.world.getBlockState(pos3);
                    if (blockState.getFluidState().isOf(Fluids.WATER)) {
                        return true;
                    }
                    if (blockState.getFluidState().isOf(Fluids.FLOWING_WATER)) {
                        return true;
                    }
                }
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean isRiptide(ItemStack stack) {
        if (!stack.isOf(Items.TRIDENT)) {
            return false;
        } else {
            for (Entry enchantmentEntry : stack.getEnchantments().getEnchantmentEntries()) {
                RegistryEntry key = (RegistryEntry) enchantmentEntry.getKey();
                if (key.matchesKey(Enchantments.RIPTIDE) && enchantmentEntry.getIntValue() > 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
