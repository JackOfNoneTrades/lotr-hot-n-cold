package hotncold.production;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.MinecraftForge;

import org.fentanylsolutions.hotncold.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import weather2.entity.EntityLightningBolt;

/** Observes Weather 2's real bolt and the client sound pipeline, including an ordinary explosion control. */
public final class WeatherLightningClientChecks {

    private final List<String> thunder = new ArrayList<>();
    private int explosions;

    public static void run(Minecraft mc) {
        boolean muted = Config.disableLightningExplosionSound;
        boolean nativeRain = Config.enableWeather2VanillaRain;
        int flash = mc.theWorld.lastLightningBolt;
        WeatherLightningClientChecks checks = new WeatherLightningClientChecks();
        MinecraftForge.EVENT_BUS.register(checks);
        try {
            for (boolean rainPatch : new boolean[] { false, true }) {
                Config.enableWeather2VanillaRain = rainPatch;
                List<String> original = checks.strike(mc, false);
                List<String> suppressed = checks.strike(mc, true);
                List<String> restored = checks.strike(mc, false);
                ProductionFixture.require(
                    original.equals(suppressed) && original.equals(restored),
                    "Weather 2 mute changed thunder, RNG, flash or bolt lifetime");
            }
            ProductionFixture.LOG.info(
                "PRODUCTION_WEATHER_LIGHTNING_PASSED: actual Weather 2 bolts off/on/off, thunder and ordinary explosion audio retained, flash/RNG/lifetime unchanged, independent of rain patch");
        } finally {
            Config.disableLightningExplosionSound = muted;
            Config.enableWeather2VanillaRain = nativeRain;
            mc.theWorld.lastLightningBolt = flash;
            MinecraftForge.EVENT_BUS.unregister(checks);
        }
    }

    private List<String> strike(Minecraft mc, boolean muted) {
        Config.disableLightningExplosionSound = muted;
        thunder.clear();
        explosions = 0;
        mc.theWorld.lastLightningBolt = 0;
        EntityLightningBolt bolt = new EntityLightningBolt(
            mc.theWorld,
            mc.thePlayer.posX + 4,
            mc.thePlayer.posY + 2,
            mc.thePlayer.posZ);
        Random random = ReflectionHelper.getPrivateValue(Entity.class, bolt, "rand", "field_70146_Z");
        random.setSeed(894321L);
        ReflectionHelper.setPrivateValue(EntityLightningBolt.class, bolt, 2, "boltLivingTime");
        bolt.boltVertex = 42;
        List<String> state = new ArrayList<>();
        for (int tick = 0; !bolt.isDead && tick < 200; tick++) {
            bolt.onUpdate();
            state.add(bolt.boltVertex + ":" + bolt.isDead + ":" + mc.theWorld.lastLightningBolt);
        }
        ProductionFixture.require(bolt.isDead, "Weather 2 bolt did not complete its lifetime");
        ProductionFixture.require(thunder.size() == 1, "Weather 2 thunder was removed or duplicated");
        ProductionFixture.require(explosions == (muted ? 0 : 1), "Weather 2 strike sound ignored mute setting");
        ProductionFixture.require(mc.theWorld.lastLightningBolt > 0, "Weather 2 lightning flash was removed");
        state.addAll(thunder);
        state.add("rng=" + random.nextLong());
        int before = explosions;
        // Ordinary server explosions deliver their sound through this vanilla packet handler.
        mc.getNetHandler()
            .handleSoundEffect(
                new S29PacketSoundEffect(
                    "random.explode",
                    mc.thePlayer.posX + 4,
                    mc.thePlayer.posY + 2,
                    mc.thePlayer.posZ,
                    4,
                    0.7F));
        ProductionFixture.require(explosions == before + 1, "Ordinary explosion audio was muted");
        return state;
    }

    @SubscribeEvent
    public void sound(PlaySoundEvent17 event) {
        if (event.result == null) {
            return;
        }
        if ("ambient.weather.thunder".equals(event.name)) {
            thunder.add(event.result.getVolume() + ":" + event.result.getPitch());
        } else if ("random.explode".equals(event.name)) {
            explosions++;
        }
    }
}
