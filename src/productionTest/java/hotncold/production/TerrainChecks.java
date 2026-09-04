package hotncold.production;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;

import lotr.common.LOTRDimension;

/** Deterministic samples compared between separate vanilla-addon and Hot N Cold processes. */
public final class TerrainChecks {

    private TerrainChecks() {}

    public static Sample run() throws Exception {
        List<String> output = new ArrayList<>();
        List<String> registry = new ArrayList<>();
        for (Object name : Block.blockRegistry.getKeys()) {
            registry.add("block=" + name);
        }
        for (Object name : Item.itemRegistry.getKeys()) {
            registry.add("item=" + name);
        }
        for (Object name : EntityList.stringToClassMapping.keySet()) {
            if (!name.toString()
                .startsWith("hotncold_")) {
                registry.add("entity=" + name);
            }
        }
        java.util.Collections.sort(registry);
        output.addAll(registry);
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        ProductionFixture.require(world != null, "Middle-earth world absent");
        int[] coordinates = { -12, -5, 0, 5, 12 };
        long undergroundAir = 0;
        long streamsBlocks = 0;
        long wildCavesBlocks = 0;
        Map<String, Integer> decorations = new TreeMap<>();
        for (int x : coordinates) {
            for (int z : coordinates) {
                // Use the provider's actual generated/populated blocks, with fresh worlds and identical seeds.
                Chunk chunk = world.getChunkFromChunkCoords(x, z);
                world.getChunkFromChunkCoords(x + 1, z);
                world.getChunkFromChunkCoords(x, z + 1);
                world.getChunkFromChunkCoords(x + 1, z + 1);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        for (int y = 0; y < 128; y++) {
                            Block block = chunk.getBlock(dx, y, dz);
                            String name = String.valueOf(Block.blockRegistry.getNameForObject(block));
                            if (name.startsWith("streams:")) {
                                streamsBlocks++;
                            }
                            if (name.toLowerCase(java.util.Locale.ROOT)
                                .startsWith("wildcaves")) {
                                wildCavesBlocks++;
                            }
                            digest.update(
                                (name + ":" + chunk.getBlockMetadata(dx, y, dz) + ";")
                                    .getBytes(StandardCharsets.UTF_8));
                            if (y >= 8 && y < 48) {
                                if ("minecraft:air".equals(name)) {
                                    undergroundAir++;
                                } else if (name.toLowerCase(java.util.Locale.ROOT)
                                    .matches(".*(stalag|stalac|crystal|mushroom|web).*")) {
                                        decorations
                                            .put(name, decorations.containsKey(name) ? decorations.get(name) + 1 : 1);
                                    }
                            }
                        }
                    }
                }
                StringBuilder hash = new StringBuilder();
                for (byte b : digest.digest()) {
                    hash.append(String.format("%02x", b & 255));
                }
                output.add("chunk=" + x + "," + z + ":" + hash);
            }
        }
        output.add("undergroundAir=" + undergroundAir);
        output.add("decorations=" + decorations);
        output.add("streamsBlocks=" + streamsBlocks);
        output.add("wildCavesBlocks=" + wildCavesBlocks);
        Files.write(new File("terrain-snapshot.txt").toPath(), output, StandardCharsets.UTF_8);
        ProductionFixture.LOG.info(
            "PRODUCTION_TERRAIN_SAMPLE: 25 Middle-earth chunks; undergroundAir={}; decorations={}",
            undergroundAir,
            decorations);
        ProductionFixture.LOG
            .info("PRODUCTION_COMPAT_SAMPLE: streamsBlocks={}; wildCavesBlocks={}", streamsBlocks, wildCavesBlocks);
        return new Sample(undergroundAir, wildCavesBlocks);
    }

    public static final class Sample {

        public final long caveAir;
        public final long wildCavesBlocks;

        private Sample(long caveAir, long wildCavesBlocks) {
            this.caveAir = caveAir;
            this.wildCavesBlocks = wildCavesBlocks;
        }
    }
}
