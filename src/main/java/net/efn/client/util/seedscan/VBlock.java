package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.IdentityHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class VBlock {

    public static final byte AIR = 0;

    public static final byte WATER = 1;

    public static final byte LAVA = 2;

    public static final byte STONE = 3;

    public static final byte GRANITE = 4;

    public static final byte DIORITE = 5;

    public static final byte ANDESITE = 6;

    public static final byte TUFF = 7;

    public static final byte GRAVEL = 8;

    public static final byte DIRT = 9;

    public static final byte CLAY = 10;

    public static final byte ORE = 11;

    public static final byte OTHER = 12;

    public static final byte UNKNOWN = 15;

    public static final int FAMILY_GRANITE = 1;

    public static final int FAMILY_DIORITE = 2;

    public static final int FAMILY_ANDESITE = 4;

    public static final int FAMILY_GRAVEL = 8;

    private static volatile Map<Block, Byte> kinds;

    private VBlock() {}

    public static boolean isBase(byte b) {
        return b >= 3 && b <= 7;
    }

    public static boolean isOpen(byte b) {
        return b == 0 || b == 1 || b == 2;
    }

    public static int familyBit(byte b) {
        return switch (b) {
            case 4 -> 1;
            case 5 -> 2;
            case 6 -> 4;
            default -> 0;
            case 8 -> 8;
        };
    }

    public static Block familyBlock(byte b) {
        return switch (b) {
            case 4 -> Blocks.GRANITE;
            case 5 -> Blocks.DIORITE;
            case 6 -> Blocks.ANDESITE;
            default -> throw new IllegalArgumentException("not a blob kind: " + b);
            case 8 -> Blocks.GRAVEL;
        };
    }

    public static byte of(BlockState state) {
        if (state.isAir()) {
            return 0;
        } else {
            Byte kind = kinds().get(state.getBlock());
            return kind != null ? kind : 12;
        }
    }

    private static Map<Block, Byte> kinds() {
        Map map = kinds;
        if (map != null) {
            return map;
        } else {
            IdentityHashMap map2 = new IdentityHashMap();
            map2.put(Blocks.STONE, (byte) 3);
            map2.put(Blocks.DEEPSLATE, (byte) 3);
            map2.put(Blocks.GRANITE, (byte) 4);
            map2.put(Blocks.DIORITE, (byte) 5);
            map2.put(Blocks.ANDESITE, (byte) 6);
            map2.put(Blocks.TUFF, (byte) 7);
            map2.put(Blocks.GRAVEL, (byte) 8);
            map2.put(Blocks.DIRT, (byte) 9);
            map2.put(Blocks.CLAY, (byte) 10);
            map2.put(Blocks.WATER, (byte) 1);
            map2.put(Blocks.LAVA, (byte) 2);
            for (Block block :
                    new Block[] {
                        Blocks.COAL_ORE,
                        Blocks.DEEPSLATE_COAL_ORE,
                        Blocks.IRON_ORE,
                        Blocks.DEEPSLATE_IRON_ORE,
                        Blocks.COPPER_ORE,
                        Blocks.DEEPSLATE_COPPER_ORE,
                        Blocks.GOLD_ORE,
                        Blocks.DEEPSLATE_GOLD_ORE,
                        Blocks.REDSTONE_ORE,
                        Blocks.DEEPSLATE_REDSTONE_ORE,
                        Blocks.LAPIS_ORE,
                        Blocks.DEEPSLATE_LAPIS_ORE,
                        Blocks.DIAMOND_ORE,
                        Blocks.DEEPSLATE_DIAMOND_ORE,
                        Blocks.EMERALD_ORE,
                        Blocks.DEEPSLATE_EMERALD_ORE,
                        Blocks.RAW_IRON_BLOCK,
                        Blocks.RAW_COPPER_BLOCK,
                        Blocks.INFESTED_STONE,
                        Blocks.INFESTED_DEEPSLATE
                    }) {
                map2.put(block, (byte) 11);
            }
            kinds = map2;
            return map2;
        }
    }
}
