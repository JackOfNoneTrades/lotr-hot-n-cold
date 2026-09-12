package hotncold.fixture;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "hotncold_client_fixture",
    name = "Hot N Cold integrated-client test fixture",
    version = "test-only",
    dependencies = "required-after:hotncold")
public final class ClientIntegrationFixture {

    private static final Logger LOG = LogManager.getLogger("Hot N Cold client fixture");
    private boolean launchRequested;
    private int playableTicks;
    private ClientLightningFixture lightning;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("hotncold.fixture.clientSmokeTest")) {
            if (Boolean.getBoolean("hotncold.fixture.clientLightningTest")) {
                lightning = new ClientLightningFixture();
                MinecraftForge.EVENT_BUS.register(lightning);
                FMLCommonHandler.instance()
                    .bus()
                    .register(lightning);
            }
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!launchRequested && minecraft.currentScreen instanceof GuiMainMenu) {
            launchIntegratedWorld(minecraft);
            return;
        }
        if (!launchRequested || !minecraft.isIntegratedServerRunning()
            || minecraft.theWorld == null
            || minecraft.thePlayer == null) {
            return;
        }
        playableTicks++;
        if (playableTicks < 40) {
            return;
        }
        if (lightning != null && !lightning.tick(minecraft)) {
            return;
        }

        String terrain = System.getProperty("hotncold.fixture.clientTerrain", "new");
        LOG.info("CLIENT_INTEGRATED_FIXTURE_PASSED: {} terrain loaded in a playable integrated server", terrain);
        minecraft.shutdown();
    }

    private void launchIntegratedWorld(Minecraft minecraft) {
        String worldName = System.getProperty("hotncold.fixture.clientWorld", "hotncold-client-fixture");
        String terrain = System.getProperty("hotncold.fixture.clientTerrain", "new");
        File levelFile = new File(new File(new File(minecraft.mcDataDir, "saves"), worldName), "level.dat");
        boolean existingWorld = levelFile.isFile();
        boolean expectExisting = "existing".equalsIgnoreCase(terrain);
        if (existingWorld != expectExisting) {
            throw new AssertionError(
                "Client terrain fixture expected " + terrain
                    + " terrain at "
                    + levelFile
                    + " but existing="
                    + existingWorld);
        }

        launchRequested = true;
        minecraft.gameSettings.pauseOnLostFocus = false;
        WorldSettings settings = new WorldSettings(
            74839274923L,
            WorldSettings.GameType.CREATIVE,
            true,
            false,
            WorldType.DEFAULT);
        settings.enableCommands();
        minecraft.launchIntegratedServer(worldName, "Hot N Cold Test", settings);
    }
}
