package net.efn.client.util.netherite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public final class HiddenDebrisScanner {

    private static final int BLOCK_VALUES = 4096;

    private static final int BIOME_VALUES = 64;

    private static final int BLOCK_PALETTE_BITS = 8;

    private static final int BIOME_PALETTE_BITS = 3;

    private HiddenDebrisScanner() {}

    public static List<Integer> findHiddenSections(ChunkData chunkData, int i, int j, int k) {
        Objects.requireNonNull(chunkData, "data");
        if (k < 0) {
            throw new IllegalArgumentException("targetStateId must be non-negative");
        } else {
            PacketByteBuf sectionsDataBuf = chunkData.getSectionsDataBuf();
            List list;
            try {
                List list2 = scanSections(sectionsDataBuf, i, j, k);
                return sectionsDataBuf.readableBytes() == 0 ? list2 : List.of();
            } catch (RuntimeException exception) {
                List list3 = List.of();
                list = list3;
            } finally {
                sectionsDataBuf.release();
            }
            return list;
        }
    }

    static List<Integer> scanSections(PacketByteBuf packetByteBuf, int i, int j, int k) {
        Objects.requireNonNull(packetByteBuf, "buf");
        if (i >= 0 && k >= 0) {
            ArrayList list = new ArrayList();
            for (int m = 0; m < i; m++) {
                int unsignedShort = packetByteBuf.readUnsignedShort();
                HiddenDebrisScanner.PaletteRead palette = readPalette(packetByteBuf, 4096, 8, 4, k);
                readPalette(packetByteBuf, 64, 3, 1, -1);
                if (unsignedShort > 0 && palette.hiddenTarget()) {
                    list.add(j + m);
                }
            }
            return List.copyOf(list);
        } else {
            throw new IllegalArgumentException("invalid section scan arguments");
        }
    }

    private static HiddenDebrisScanner.PaletteRead readPalette(
            PacketByteBuf packetByteBuf, int i, int j, int k, int m) {
        short unsignedByte = packetByteBuf.readUnsignedByte();
        int[] ints;
        int n;
        if (unsignedByte == 0) {
            n = 0;
            ints = new int[] {packetByteBuf.readVarInt()};
        } else if (unsignedByte <= j) {
            n = Math.max(k, unsignedByte);
            int varInt = packetByteBuf.readVarInt();
            if (varInt <= 0 || varInt > 1 << n) {
                throw new IllegalArgumentException("invalid local palette size");
            }
            ints = new int[varInt];
            for (int p = 0; p < varInt; p++) {
                ints[p] = packetByteBuf.readVarInt();
            }
        } else {
            n = unsignedByte;
            if (unsignedByte > 30) {
                throw new IllegalArgumentException("direct palette is too wide");
            }
            ints = null;
        }
        int o = n == 0 ? 0 : 64 / n;
        if (n > 0 && o == 0) {
            throw new IllegalArgumentException("invalid palette storage width");
        } else {
            int q = n == 0 ? 0 : (i + o - 1) / o;
            long l = q * 8L;
            if (l > packetByteBuf.readableBytes()) {
                throw new IndexOutOfBoundsException("truncated palette data");
            } else {
                boolean bl = ints != null && contains(ints, m);
                if (!bl) {
                    packetByteBuf.skipBytes((int) l);
                    return new HiddenDebrisScanner.PaletteRead(false, true);
                } else {
                    int r = 0;
                    if (n == 0) {
                        if (ints[0] == m) {
                            r = i;
                        }
                    } else {
                        int readerIndex = packetByteBuf.readerIndex();
                        long l2 = (1L << n) - 1L;
                        for (int v = 0; v < i; v++) {
                            int t = v / o;
                            int u = v - t * o;
                            int w =
                                    (int)
                                            (packetByteBuf.getLong(readerIndex + t * 8) >>> u * n
                                                    & l2);
                            int i2 = ints == null ? w : paletteValue(ints, w);
                            if (i2 == m) {
                                r++;
                            }
                        }
                    }
                    packetByteBuf.skipBytes((int) l);
                    return new HiddenDebrisScanner.PaletteRead(bl, r == 0);
                }
            }
        }
    }

    private static int paletteValue(int[] ints, int i) {
        if (i >= 0 && i < ints.length) {
            return ints[i];
        } else {
            throw new IllegalArgumentException("packed palette index is invalid");
        }
    }

    private static boolean contains(int[] ints, int i) {
        for (int j : ints) {
            if (j == i) {
                return true;
            }
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    private record PaletteRead(boolean paletteHasTarget, boolean targetUnused) {

        boolean hiddenTarget() {
            return this.paletteHasTarget && this.targetUnused;
        }
    }
}
