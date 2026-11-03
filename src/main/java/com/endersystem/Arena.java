package com.endersystem;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Penalty Zone: a huge desert (radius 700) high above the Overworld.
 * Terrain is generated chunk by chunk around the player, so nothing big is built at once.
 *
 * Every column has a BEDROCK sheet under it, so explosions can never open a hole in the
 * world. The edge of the zone is an invisible barrier wall.
 */
public final class Arena {
    public static final int CENTER_X = 40000;
    public static final int CENTER_Z = 40000;
    public static final int RADIUS = 700;
    public static final int BASE_Y = 268;

    private static final int WALL_THICKNESS = 2;
    private static final int CLEAR_ABOVE = 20;     // blocks above the ground that are wiped on regeneration
    private static final int FLAGS = Block.NOTIFY_LISTENERS;

    private static final Set<Long> DONE = new HashSet<>();
    private static final List<int[]> OFFSETS = new ArrayList<>();

    static {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                OFFSETS.add(new int[]{dx, dz});
            }
        }
        OFFSETS.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
    }

    private Arena() {}

    /**
     * Call when a new Penalty starts (and nobody is inside). Every chunk will then be rebuilt from
     * scratch the next time it is needed, so holes, placed blocks etc. from earlier penalties vanish.
     */
    public static void newSession() {
        DONE.clear();
    }

    // ------------------------------------------------------------------ shape of the land

    private static long hash(int x, int z) {
        long h = x * 341873128712L + z * 132897987541L + 0x9E3779B97F4A7C15L;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        return h & Long.MAX_VALUE;
    }

    /** Height of the sand dunes at x,z. */
    public static int dune(int x, int z) {
        double h = 5 * Math.sin(x / 40.0) * Math.cos(z / 50.0)
                + 3 * Math.sin((x + z) / 26.0)
                + 8 * Math.sin(x / 70.0 + 1) * Math.sin(z / 60.0);
        return BASE_Y + (int) Math.round(h);
    }

    /** Extra height of rock formations (mesas / spires) at x,z. 0 if none. Flat near the centre. */
    public static int rockExtra(int x, int z) {
        if (Math.hypot(x - CENTER_X, z - CENTER_Z) < 45) return 0;
        int best = 0;
        int cellX = Math.floorDiv(x, 48);
        int cellZ = Math.floorDiv(z, 48);
        for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
            for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                long h = hash(cx, cz);
                if (h % 100 >= 35) continue; // 35% of cells have a rock
                int rx = cx * 48 + 6 + (int) ((h >> 8) % 36);
                int rz = cz * 48 + 6 + (int) ((h >> 16) % 36);
                int rad = 3 + (int) ((h >> 24) % 5);
                int hgt = 6 + (int) ((h >> 32) % 11);
                double dd = Math.hypot(x - rx, z - rz);
                if (dd <= rad) {
                    best = Math.max(best, (int) (hgt * (1 - 0.7 * dd / rad)));
                }
            }
        }
        return best;
    }

    /** Y of the top solid block at x,z. */
    public static int surfaceY(int x, int z) {
        return dune(x, z) + rockExtra(x, z);
    }

    public static boolean contains(double x, double z) {
        double dx = x - CENTER_X, dz = z - CENTER_Z;
        double r = RADIUS + WALL_THICKNESS + 24;
        return dx * dx + dz * dz <= r * r;
    }

    public static Box box() {
        int r = RADIUS + 10;
        return new Box(CENTER_X - r, BASE_Y - 40, CENTER_Z - r, CENTER_X + r, 319, CENTER_Z + r);
    }

    // ------------------------------------------------------------------ generation

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    /**
     * (Re)builds chunks around chunk (pcx,pcz) that are within maxDist2 (squared, in chunks) and were
     * not yet built in the current session, at most maxNew per call. Returns how many were handled.
     */
    public static int ensureChunks(ServerWorld w, int pcx, int pcz, int maxDist2, int maxNew) {
        int handled = 0;
        for (int[] o : OFFSETS) {
            if (o[0] * o[0] + o[1] * o[1] > maxDist2) break; // list is sorted by distance
            int cx = pcx + o[0];
            int cz = pcz + o[1];
            long k = key(cx, cz);
            if (DONE.contains(k)) continue;
            if (handled >= maxNew) break;
            DONE.add(k);
            handled++;

            double cdx = cx * 16 + 8 - CENTER_X;
            double cdz = cz * 16 + 8 - CENTER_Z;
            if (Math.sqrt(cdx * cdx + cdz * cdz) - 12 > RADIUS + WALL_THICKNESS) continue; // outside the zone
            generateChunk(w, cx, cz);
        }
        return handled;
    }

    private static Block rockBlock(int y) {
        return switch (Math.floorMod(y, 4)) {
            case 0 -> Blocks.RED_SANDSTONE;
            case 1 -> Blocks.ORANGE_TERRACOTTA;
            case 2 -> Blocks.TERRACOTTA;
            default -> Blocks.YELLOW_TERRACOTTA;
        };
    }

    private static void generateChunk(ServerWorld w, int cx, int cz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockState bedrock = Blocks.BEDROCK.getDefaultState();
        BlockState sandstone = Blocks.SANDSTONE.getDefaultState();
        BlockState sand = Blocks.SAND.getDefaultState();
        BlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockState cactus = Blocks.CACTUS.getDefaultState();
        BlockState deadBush = Blocks.DEAD_BUSH.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        int minX = cx << 4;
        int minZ = cz << 4;
        long inner = (long) RADIUS * RADIUS;
        long outer = (long) (RADIUS + WALL_THICKNESS) * (RADIUS + WALL_THICKNESS);

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                long ddx = x - CENTER_X;
                long ddz = z - CENTER_Z;
                long d2 = ddx * ddx + ddz * ddz;

                if (d2 <= inner) {
                    int h = dune(x, z);
                    // one unbreakable bedrock sheet under every column
                    w.setBlockState(pos.set(x, h - 10, z), bedrock, FLAGS);
                    for (int y = h - 9; y <= h - 4; y++) {
                        w.setBlockState(pos.set(x, y, z), sandstone, FLAGS);
                    }
                    for (int y = h - 3; y <= h; y++) {
                        w.setBlockState(pos.set(x, y, z), sand, FLAGS);
                    }

                    int extra = rockExtra(x, z);

                    // wipe anything placed above the ground in earlier penalties
                    int top = h + extra;
                    for (int y = top + 1; y <= Math.min(316, top + CLEAR_ABOVE); y++) {
                        pos.set(x, y, z);
                        if (!w.getBlockState(pos).isAir()) w.setBlockState(pos, air, FLAGS);
                    }

                    if (extra > 0) {
                        for (int y = h + 1; y <= h + extra; y++) {
                            w.setBlockState(pos.set(x, y, z), rockBlock(y).getDefaultState(), FLAGS);
                        }
                    } else if (d2 > 30L * 30L) {
                        long r = hash(x, z);
                        if (r % 260 == 0) {
                            int ch = 1 + (int) ((r >> 12) % 3);
                            for (int y = h + 1; y <= h + ch; y++) {
                                w.setBlockState(pos.set(x, y, z), cactus, FLAGS);
                            }
                        } else if (r % 90 == 1) {
                            w.setBlockState(pos.set(x, h + 1, z), deadBush, FLAGS);
                        }
                    }
                } else if (d2 <= outer) {
                    for (int y = BASE_Y - 30; y <= BASE_Y + 44; y++) {
                        w.setBlockState(pos.set(x, y, z), barrier, FLAGS);
                    }
                }
            }
        }
    }
}
