package hotncold.production;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import org.fentanylsolutions.hotncold.compat.MineFantasyCampfire;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Creates an actual tracked campfire beside the connected player, then lights and extinguishes it. */
public final class CampfireNetworkChecks {

    public static volatile int stage;
    public static volatile int x, y, z;
    private TileEntityCampfire tile;

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ProductionFixture.serverChecksPassed || stage >= 5) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) {
            return;
        }
        if (stage == 0) {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            WorldServer world = (WorldServer) player.worldObj;
            x = MathHelper.floor_double(player.posX) + 2;
            y = MathHelper.floor_double(player.posY);
            z = MathHelper.floor_double(player.posZ);
            world.setBlock(x, y - 1, z, Blocks.stone);
            world.setBlock(x, y + 2, z, Blocks.stone);
            world.setBlock(x, y, z, CampfireBackportBlocks.campfire_base, 2, 3);
            tile = (TileEntityCampfire) world.getTileEntity(x, y, z);
            ProductionFixture.require(
                ((MineFantasyCampfire) tile).hotncold$addFuel(new ItemStack(Items.stick)),
                "Could not fuel the network test campfire");
            stage = 1;
        } else if (stage == 2) {
            BlockCampfire.updateCampfireBlockState(1, null, tile);
            stage = 3;
        } else if (stage == 4) {
            BlockCampfire.updateCampfireBlockState(0, null, tile);
            stage = 5;
        }
    }
}
