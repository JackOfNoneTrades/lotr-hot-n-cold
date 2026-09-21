package org.fentanylsolutions.hotncold.mixins.late.campfirebackport;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.compat.MineFantasyCampfire;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import minefantasy.mf2.block.tileentity.TileEntityFirepit;

@Mixin(value = BlockCampfire.class, remap = false)
public abstract class MixinBlockCampfire {

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true, remap = true, require = 1)
    private void hotncold$feedFire(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        TileEntity tile = world.getTileEntity(x, y, z);
        ItemStack stack = player.getHeldItem();
        if (!(tile instanceof MineFantasyCampfire) || stack == null || stack.stackSize <= 0) {
            return;
        }
        MineFantasyCampfire fire = (MineFantasyCampfire) tile;
        if (!fire.hotncold$isFuelEnabled() || TileEntityFirepit.getItemBurnTime(stack) <= 0) {
            return;
        }
        if (!world.isRemote && fire.hotncold$addFuel(stack) && !player.capabilities.isCreativeMode) {
            ItemStack container = stack.getItem()
                .getContainerItem(stack);
            --stack.stackSize;
            if (stack.stackSize == 0) {
                player.setCurrentItemOrArmor(0, container);
            } else if (container != null && !player.inventory.addItemStackToInventory(container)) {
                player.dropPlayerItemWithRandomChoice(container, false);
            }
        }
        // Recognized fuel must never fall through into a cooking recipe, even when the fire is full.
        cir.setReturnValue(true);
    }

    @Inject(
        method = "updateCampfireBlockState(ILnet/minecraft/entity/player/EntityPlayer;Lconnor135246/campfirebackport/common/tileentity/TileEntityCampfire;)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private static void hotncold$requireFuelToLight(int mode, EntityPlayer player, TileEntityCampfire tile,
        CallbackInfoReturnable<Integer> cir) {
        MineFantasyCampfire fire = (MineFantasyCampfire) tile;
        if (fire.hotncold$isFuelEnabled() && (mode == 2 || (mode == 1 && fire.hotncold$getFuel() == 0))) {
            cir.setReturnValue(0);
        }
    }
}
