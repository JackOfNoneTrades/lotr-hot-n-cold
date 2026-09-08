package hotncold.production;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ProductionClient extends ProductionFixture.ServerProxy {

    private boolean launched;
    private int playableTicks;
    private int visibilityTicks;
    private int failedMenuTicks;
    private ProductionGearScreen gearScreen;
    private LightningClientChecks lightningChecks;

    @Override
    public void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        if ("lightning".equals(ProductionFixture.profile())) {
            lightningChecks = new LightningClientChecks();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(lightningChecks);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!launched && mc.currentScreen instanceof GuiMainMenu) {
            launched = true;
            // An unattended Xvfb window must not pause its integrated server when it lacks focus.
            mc.gameSettings.pauseOnLostFocus = false;
            boolean existing = new File(mc.mcDataDir, "saves/acceptance/level.dat").isFile();
            ProductionFixture.LOG.info("PRODUCTION_WORLD_OPEN: existing={}", existing);
            WorldSettings settings = new WorldSettings(
                74839274923L,
                WorldSettings.GameType.CREATIVE,
                true,
                false,
                WorldType.DEFAULT);
            settings.enableCommands();
            mc.launchIntegratedServer("acceptance", "Production acceptance", settings);
        }
        boolean failedMenu = launched && mc.currentScreen instanceof GuiMainMenu
            && mc.theWorld == null
            && (mc.getIntegratedServer() == null || mc.getIntegratedServer()
                .isServerStopped());
        ProductionFixture.require(
            !failedMenu || ++failedMenuTicks < 100,
            "Integrated server returned to the menu without loading the acceptance world; see the FML startup error");
        if (ProductionFixture.serverChecksPassed && mc.theWorld != null && mc.thePlayer != null) {
            if (lightningChecks != null && !lightningChecks.tick(mc)) {
                return;
            }
            if (ProductionFixture.visibilityNPCData != null && ProductionFixture.visibilityStage != 4) {
                ProductionFixture.require(
                    ++visibilityTicks < 1200,
                    "Client did not receive configured armor/shield or empty shield update");
                net.minecraft.entity.Entity entity = mc.theWorld.getEntityByID(ProductionFixture.visibleEntityId);
                if (visibilityTicks % 100 == 0) {
                    ProductionFixture.LOG.info(
                        "PRODUCTION_CLIENT_GEAR_DIAGNOSTIC: stage={}, entityId={}, clientDimension={}, entity={}",
                        ProductionFixture.visibilityStage,
                        ProductionFixture.visibleEntityId,
                        mc.theWorld.provider.dimensionId,
                        entity);
                }
                if (entity instanceof lotr.common.entity.npc.LOTREntityNPC) {
                    lotr.common.entity.npc.LOTREntityNPC npc = (lotr.common.entity.npc.LOTREntityNPC) entity;
                    if (visibilityTicks % 100 == 0) {
                        ProductionFixture.LOG.info(
                            "PRODUCTION_CLIENT_GEAR_DIAGNOSTIC: helmet={}, shield={}",
                            npc.getEquipmentInSlot(4),
                            npc.npcShield);
                    }
                    if (ProductionFixture.visibilityStage == 1
                        && npc.npcShield == lotr.common.LOTRShields.ALIGNMENT_ROHAN
                        && npc.getEquipmentInSlot(4) != null
                        && npc.getEquipmentInSlot(4)
                            .getItem() == ProductionFixture.visibilityHelmet) {
                        ProductionFixture.LOG.info(
                            "PRODUCTION_CLIENT_GEAR_PASSED: client entity has helmet={} and configured shield after tracking saved NPC",
                            net.minecraft.item.Item.itemRegistry.getNameForObject(ProductionFixture.visibilityHelmet));
                        gearScreen = new ProductionGearScreen(npc, ProductionFixture.visibilityLabel);
                        mc.displayGuiScreen(gearScreen);
                        ProductionFixture.visibilityStage = 2;
                    } else if (ProductionFixture.visibilityStage == 3 && npc.npcShield == null && gearScreen.rendered) {
                        ProductionFixture.LOG
                            .info("PRODUCTION_CLIENT_EMPTY_SHIELD_PASSED: live shield update reached client");
                        ProductionFixture.visibilityStage = 4;
                    }
                }
                return;
            }
            if (++playableTicks < 100) {
                return;
            }
            ProductionFixture.require(gearScreen == null || gearScreen.rendered, "Equipment preview never rendered");
            ProductionFixture.passed();
            mc.shutdown();
        }
    }
}
