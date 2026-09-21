package hotncold.production;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;

import org.fentanylsolutions.hotncold.compat.MineFantasyCampfire;

import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import minefantasy.mf2.api.crafting.IBasicMetre;

public final class CampfireClientChecks {

    private static int ticks;
    private static boolean finished;

    public static boolean tick(Minecraft mc) {
        if (finished) {
            return true;
        }
        ProductionFixture.require(++ticks < 600, "Campfire fuel packet test timed out");
        TileEntity raw = mc.theWorld
            .getTileEntity(CampfireNetworkChecks.x, CampfireNetworkChecks.y, CampfireNetworkChecks.z);
        if (!(raw instanceof MineFantasyCampfire) || !(raw instanceof TileEntityCampfire)) {
            return false;
        }
        MineFantasyCampfire fuel = (MineFantasyCampfire) raw;
        TileEntityCampfire tile = (TileEntityCampfire) raw;
        IBasicMetre meter = (IBasicMetre) raw;
        if (CampfireNetworkChecks.stage == 1 && fuel.hotncold$getFuel() == 600 && !tile.isLit()) {
            ProductionFixture
                .require(fuel.hotncold$isFuelEnabled() && meter.shouldShowMetre(), "Client fuel display disabled");
            ProductionFixture.require(meter.getMetreScale(12000) == 600, "Client fuel meter amount is incorrect");
            ProductionFixture.require(
                meter.getLocalisedName()
                    .endsWith("0:30"),
                "Client fuel meter time is incorrect");
            CampfireNetworkChecks.stage = 2;
        } else if (CampfireNetworkChecks.stage == 3 && tile.isLit()
            && fuel.hotncold$getFuel() > 0
            && fuel.hotncold$getFuel() < 580) {
                CampfireNetworkChecks.stage = 4;
            } else if (CampfireNetworkChecks.stage == 5 && !tile.isLit() && fuel.hotncold$getFuel() > 0) {
                ProductionFixture.LOG.info(
                    "PRODUCTION_CAMPFIRE_CLIENT_PASSED: real tile packets delivered refuel, lit countdown, extinguish and MineFantasy HUD data");
                finished = true;
                return true;
            }
        return false;
    }
}
