package hotncold.production;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Records the real client's sound pipeline after server sound packets and the weather entity arrive. */
public final class LightningClientChecks {

    private int ticks;
    private int settledTicks;
    private int thunder;
    private int explosions;
    private boolean flash;
    private boolean finished;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void sound(PlaySoundEvent17 event) {
        if (LightningChecks.networkStage == 0 || finished || event.result == null) {
            return;
        }
        if ("ambient.weather.thunder".equals(event.name)) {
            thunder++;
        } else if ("random.explode".equals(event.name)) {
            explosions++;
        }
    }

    public boolean tick(Minecraft mc) {
        if (finished) {
            return true;
        }
        ProductionFixture.require(++ticks < 600, "Client lightning sound test timed out");
        if (ticks < 100) {
            return false;
        }
        if (LightningChecks.networkStage == 0) {
            LightningChecks.networkStage = 1;
        }
        if (LightningChecks.networkStage == 2) {
            flash |= mc.theWorld.lastLightningBolt > 0;
            if (thunder > 0 && ++settledTicks >= 40) {
                ProductionFixture.require(thunder == 1, "Client did not receive exactly one thunder sound");
                ProductionFixture.require(
                    explosions == (LightningChecks.startupMuted ? 0 : 1),
                    "Client lightning explosion sound did not follow the startup setting: " + explosions);
                ProductionFixture.require(flash, "Client lightning flash was lost");
                settledTicks = 0;
                LightningChecks.networkStage = 3;
            }
        } else if (LightningChecks.networkStage == 4 && ++settledTicks >= 40) {
            ProductionFixture.require(
                explosions == (LightningChecks.startupMuted ? 1 : 2),
                "Actual explosion sound failed to reach client: " + explosions);
            finished = true;
            ProductionFixture.LOG.info(
                "PRODUCTION_LIGHTNING_CLIENT_PASSED: startupMuted={}, thunder={}, explosionSounds={}, flash={}; real server weather entity and sound packets reached client audio pipeline",
                LightningChecks.startupMuted,
                thunder,
                explosions,
                flash);
        }
        return finished;
    }
}
