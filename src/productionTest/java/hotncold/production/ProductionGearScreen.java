package hotncold.production;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;

import lotr.common.entity.npc.LOTREntityNPC;

/** Renders the actual tracked NPC with and without its helmet for visual inspection. */
public final class ProductionGearScreen extends GuiScreen {

    private final LOTREntityNPC npc;
    private final String label;
    public boolean rendered;

    public ProductionGearScreen(LOTREntityNPC npc, String label) {
        this.npc = npc;
        this.label = label;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        ItemStack helmet = npc.getEquipmentInSlot(4);
        ProductionFixture.require(helmet != null, "Tracked NPC lost its configured helmet");
        drawCenteredString(fontRendererObj, label + ": " + helmet.getDisplayName(), width / 4, 20, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Same NPC without helmet", width * 3 / 4, 20, 0xFFFFFF);
        int scale = Math.min(width / 8, (height - 60) / 3);
        GuiInventory.func_147046_a(width / 4, height - 30, scale, 15F, 0F, npc);
        // Only the client preview is temporarily changed; server state and received gear remain untouched.
        npc.setCurrentItemOrArmor(4, null);
        try {
            GuiInventory.func_147046_a(width * 3 / 4, height - 30, scale, 15F, 0F, npc);
        } finally {
            npc.setCurrentItemOrArmor(4, helmet);
        }
        if (!rendered) {
            ProductionFixture.LOG.info(
                "PRODUCTION_CLIENT_RENDER_PASSED: helmet={}, screenshot={}",
                net.minecraft.item.Item.itemRegistry.getNameForObject(helmet.getItem()),
                ScreenShotHelper
                    .saveScreenshot(
                        mc.mcDataDir,
                        "equipment-" + label + ".png",
                        mc.displayWidth,
                        mc.displayHeight,
                        mc.getFramebuffer())
                    .getUnformattedText());
            ProductionFixture.require(
                new java.io.File(mc.mcDataDir, "screenshots/equipment-" + label + ".png").isFile(),
                "Equipment preview screenshot was not saved");
            rendered = true;
        }
    }
}
