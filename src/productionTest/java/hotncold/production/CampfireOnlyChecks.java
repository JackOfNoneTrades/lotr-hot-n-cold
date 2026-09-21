package hotncold.production;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.fentanylsolutions.hotncold.compat.MineFantasyCampfire;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import cpw.mods.fml.common.Loader;

/** Can run with Campfire Backport installed and MineFantasy absent. */
public final class CampfireOnlyChecks {

    public static void run() {
        ProductionFixture.require(!Loader.isModLoaded("minefantasy2"), "This check requires MineFantasy to be absent");
        WorldServer world = DimensionManager.getWorld(0);
        world.setBlock(8, 200, 8, CampfireBackportBlocks.campfire);
        TileEntityCampfire tile = (TileEntityCampfire) world.getTileEntity(8, 200, 8);
        ProductionFixture.require(!(tile instanceof MineFantasyCampfire), "Integration loaded without both mods");
        tile.updateEntity();
        ProductionFixture.require(tile.isLit(), "Campfire without MineFantasy lost its normal behavior");
        ProductionFixture.LOG.info("PRODUCTION_CAMPFIRE_ONLY_PASSED: no MineFantasy dependency or fuel patch loaded");
    }
}
