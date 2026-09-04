package hotncold.production;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.DimensionManager;

import org.fentanylsolutions.hotncold.Config;

import cpw.mods.fml.common.Loader;
import lotr.common.LOTRDimension;
import lotr.common.world.biome.LOTRBiome;

/** Generation assertions on the merged product, with optional dependencies genuinely optional. */
public final class WorldgenChecks {

    private WorldgenChecks() {}

    public static void run() throws Exception {
        TerrainChecks.Sample sample = TerrainChecks.run();
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        boolean greg = Loader.isModLoaded("gregcaves") && Config.enableGregCavesMiddleEarth;
        if (Loader.isModLoaded("gregcaves")) {
            Object provider = world.theChunkProviderServer.currentChunkProvider;
            Field field = provider.getClass()
                .getDeclaredField("hotncold$gregCavesGenerator");
            field.setAccessible(true);
            Object generator = field.get(provider);
            ProductionFixture.require((generator != null) == greg, "Greg Caves generator does not match config");
            if (greg) {
                ProductionFixture.require(
                    generator.getClass()
                        .getName()
                        .equals("mods.tesseract.gregcaves.world.MapGenGregCaves"),
                    "Wrong cave generator");
                ProductionFixture.require(sample.caveAir > 0, "No actual cave air generated");
            }
        }
        boolean wild = Loader.isModLoaded("wildcaves3") && Config.enableWildCavesMiddleEarth;
        if (wild) {
            Class<?> wc = Class.forName("wildCaves.WorldGenWildCaves");
            java.util.List<?> blacklist = (java.util.List<?>) wc.getField("dimensionBlacklist")
                .get(null);
            wild = !blacklist.contains(world.provider.dimensionId);
        }
        ProductionFixture.require(
            (sample.wildCavesBlocks > 0) == wild,
            "Wild Caves decoration output does not match config/blacklist");
        ProductionFixture.LOG.info(
            "PRODUCTION_CAVES_PASSED: gregActive={}, caveAir={}, wildActive={}, decorationBlocks={}",
            greg,
            sample.caveAir,
            wild,
            sample.wildCavesBlocks);
        if (Loader.isModLoaded("streams")) {
            Class<?> compat = Class.forName("org.fentanylsolutions.hotncold.compat.StreamsCompat");
            Field field = compat.getDeclaredField("middleEarthGenerator");
            field.setAccessible(true);
            Field allowed = compat.getDeclaredField("allowedBiomes");
            allowed.setAccessible(true);
            boolean active = Config.enableStreamsMiddleEarth && !((java.util.Set<?>) allowed.get(null)).isEmpty();
            ProductionFixture.require(
                (field.get(null) != null) == active,
                "Streams generator does not match enabled/allowlist settings");
            if (active) {
                StreamsChecks.run();
            }
            testStreamsBiomePolicy(compat, allowed);
            ProductionFixture.LOG.info("PRODUCTION_STREAMS_CONFIG_PASSED: active={}", active);
        } else {
            ProductionFixture.LOG.info("PRODUCTION_STREAMS_ABSENT_PASSED: terrain generated without Streams/Farseek");
        }
    }

    private static void testStreamsBiomePolicy(Class<?> compat, Field allowed) throws Exception {
        Method find = compat.getDeclaredMethod("findMiddleEarthBiome", String.class);
        find.setAccessible(true);
        LOTRBiome shire = LOTRDimension.MIDDLE_EARTH.biomeList[3];
        ProductionFixture.require(find.invoke(null, " SHIRE ") == shire, "Biome-name lookup lost WOTR replacement");
        ProductionFixture.require(find.invoke(null, "3") == shire, "Numeric biome lookup lost WOTR replacement");
        ProductionFixture.require(find.invoke(null, "missing-biome") == null, "Unknown biome should be rejected");
        Method policy = compat
            .getMethod("isAllowedComponent", IBlockAccess.class, StructureBoundingBox.class, boolean.class);
        BiomeGenBase[] biome = { shire };
        WorldProvider provider = new WorldProvider() {

            @Override
            public String getDimensionName() {
                return "policy-test-only";
            }

            @Override
            public BiomeGenBase getBiomeGenForCoords(int x, int z) {
                return biome[0];
            }
        };
        provider.dimensionId = LOTRDimension.MIDDLE_EARTH.dimensionID;
        Class<?> blockAccess = Class.forName("farseek.world.BlockAccess");
        // Only the policy helper is controlled here. The water-block assertion above uses real terrain.
        Object access = Proxy
            .newProxyInstance(blockAccess.getClassLoader(), new Class<?>[] { blockAccess }, (proxy, method, args) -> {
                if (method.getName()
                    .equals("worldProvider")) {
                    return provider;
                }
                throw new AssertionError("Unexpected access method: " + method.getName());
            });
        StructureBoundingBox box = new StructureBoundingBox(0, 0, 0, 1, 255, 1);
        Object original = allowed.get(null);
        boolean enabled = Config.enableStreamsMiddleEarth;
        try {
            Config.enableStreamsMiddleEarth = true;
            allowed.set(null, Collections.singleton(shire));
            ProductionFixture.require((Boolean) policy.invoke(null, access, box, false), "Allowed biome rejected");
            biome[0] = LOTRBiome.mordor;
            ProductionFixture
                .require(!(Boolean) policy.invoke(null, access, box, false), "Unlisted land biome accepted");
            // Avoid LOTRBiome.river's name collision with vanilla BiomeGenBase.river in reobfuscation.
            biome[0] = java.util.Arrays.stream(LOTRDimension.MIDDLE_EARTH.biomeList)
                .filter(candidate -> candidate instanceof lotr.common.world.biome.LOTRBiomeGenRiver)
                .findFirst()
                .get();
            ProductionFixture
                .require((Boolean) policy.invoke(null, access, box, true), "River mouth connector rejected");
            ProductionFixture
                .require(!(Boolean) policy.invoke(null, access, box, false), "Unlisted river accepted upstream");
            allowed.set(null, Collections.emptySet());
            ProductionFixture
                .require(!(Boolean) policy.invoke(null, access, box, true), "Empty list must reject even connectors");
            allowed.set(null, Collections.singleton(shire));
            biome[0] = shire;
            Config.enableStreamsMiddleEarth = false;
            ProductionFixture.require(
                !(Boolean) policy.invoke(null, access, box, false),
                "Disabled compatibility accepted Middle-earth component");
            provider.dimensionId = 0;
            ProductionFixture.require(
                (Boolean) policy.invoke(null, access, box, false),
                "Middle-earth switch affected Overworld policy");
        } finally {
            allowed.set(null, original);
            Config.enableStreamsMiddleEarth = enabled;
        }
        ProductionFixture.LOG.info(
            "PRODUCTION_STREAMS_POLICY_PASSED: name/ID, WOTR replacement, allowlist, connector exception, empty list, disabled and other dimensions");
    }
}
