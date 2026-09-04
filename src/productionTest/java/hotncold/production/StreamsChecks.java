package hotncold.production;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.DimensionManager;

import lotr.common.LOTRDimension;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.Map;

/** Probes an untouched reference jar without compiling against its compatibility implementation. */
public final class StreamsChecks {

    private StreamsChecks() {}

    public static void run() throws Exception {
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        Class<?> compat = Class.forName("org.fentanylsolutions.hotncold.compat.StreamsCompat");
        Field field = compat.getDeclaredField("middleEarthGenerator");
        field.setAccessible(true);
        Object generator = field.get(null);
        ProductionFixture.require(generator != null, "Middle-earth Streams generator is absent");
        Object river = null;
        int regions = 0;
        // Streams tries one river per 16x16-chunk region. Actual provider generation selects it;
        // the fixture does not create rivers, change terrain, or bypass their validity checks.
        search: for (int radius = 0; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
                        continue;
                    }
                    world.getChunkFromChunkCoords(x * 16, z * 16);
                    regions++;
                    Map<?, ?> structures = (Map<?, ?>) generator.getClass()
                        .getMethod("structures")
                        .invoke(generator);
                    Iterator<?> values = structures.valuesIterator();
                    while (values.hasNext()) {
                        Option<?> option = (Option<?>) values.next();
                        if (option.isDefined()) {
                            river = option.get();
                            break search;
                        }
                    }
                    if (regions % 20 == 0) {
                        ProductionFixture.LOG.info("PRODUCTION_STREAMS_SEARCH: regions={}", regions);
                    }
                }
            }
        }
        ProductionFixture.require(river != null, "No valid natural Streams river found in " + regions + " regions");
        StructureBoundingBox box = (StructureBoundingBox) river.getClass()
            .getMethod("boundingBox")
            .invoke(river);
        ProductionFixture.LOG.info("PRODUCTION_STREAMS_RIVER_FOUND: regions={}, bounds={}", regions, box);
        int minX = box.minX >> 4;
        int minZ = box.minZ >> 4;
        int maxX = box.maxX >> 4;
        int maxZ = box.maxZ >> 4;
        ProductionFixture.require((maxX - minX + 1) * (maxZ - minZ + 1) <= 1024, "Unexpectedly large river bounds");
        for (int x = minX; x <= maxX + 1; x++) {
            for (int z = minZ; z <= maxZ + 1; z++) {
                world.getChunkFromChunkCoords(x, z);
            }
        }
        int blocks = 0;
        String first = null;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Chunk chunk = world.getChunkFromChunkCoords(x, z);
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        for (int y = 1; y < 128; y++) {
                            Block block = chunk.getBlock(dx, y, dz);
                            String name = String.valueOf(Block.blockRegistry.getNameForObject(block));
                            if (name.startsWith("streams:")) {
                                blocks++;
                                if (first == null) {
                                    first = (x * 16 + dx) + "," + y + "," + (z * 16 + dz) + " " + name;
                                }
                            }
                        }
                    }
                }
            }
        }
        ProductionFixture.require(blocks > 0, "Valid Streams structure did not produce flowing-water blocks");
        String result = "PRODUCTION_STREAMS_BLOCKS_PASSED: regions=" + regions
            + ", blocks="
            + blocks
            + ", first="
            + first;
        ProductionFixture.LOG.info(result);
        Files.write(new File("streams-result.txt").toPath(), Arrays.asList(result), StandardCharsets.UTF_8);
    }
}
