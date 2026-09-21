package org.fentanylsolutions.hotncold.mixins.late.campfirebackport;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.MineFantasyCampfire;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import minefantasy.mf2.api.crafting.IBasicMetre;
import minefantasy.mf2.block.tileentity.TileEntityFirepit;
import minefantasy.mf2.item.food.FoodListMF;

@Mixin(value = TileEntityCampfire.class, remap = false)
public abstract class MixinTileEntityCampfire extends TileEntity implements MineFantasyCampfire, IBasicMetre {

    @Unique
    private int hotncold$fuel;
    @Unique
    private boolean hotncold$clientEnabled;

    @Shadow
    public abstract boolean isLit();

    @Shadow
    public abstract void markForClient();

    @Override
    public boolean hotncold$isFuelEnabled() {
        return worldObj != null && worldObj.isRemote ? hotncold$clientEnabled : Config.enableMineFantasyCampfires;
    }

    @Override
    public int hotncold$getFuel() {
        return hotncold$fuel;
    }

    @Override
    public boolean hotncold$addFuel(ItemStack stack) {
        if (!hotncold$isFuelEnabled() || worldObj == null
            || worldObj.isRemote
            || stack == null
            || stack.stackSize <= 0) {
            return false;
        }
        int amount = TileEntityFirepit.getItemBurnTime(stack);
        if (amount <= 0 || hotncold$fuel >= FUEL_CAPACITY) {
            return false;
        }
        hotncold$fuel += Math.min(amount, FUEL_CAPACITY - hotncold$fuel);
        markDirty();
        markForClient();
        return true;
    }

    @Inject(method = "updateEntity", at = @At("HEAD"), remap = true, require = 1)
    private void hotncold$checkFuelBeforeCooking(CallbackInfo ci) {
        if (!hotncold$isFuelEnabled()) {
            return;
        }
        if (worldObj.isRemote) {
            if (isLit() && hotncold$fuel > 0) {
                --hotncold$fuel;
            }
        } else if (isLit() && hotncold$fuel == 0) {
            hotncold$extinguish();
        }
    }

    @Inject(method = "burnOutOverTime", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$consumeFuel(CallbackInfo ci) {
        if (hotncold$isFuelEnabled()) {
            // Rain may have extinguished the campfire earlier in this same update.
            if (!worldObj.isRemote && isLit() && hotncold$fuel > 0) {
                --hotncold$fuel;
                markDirty();
                if (hotncold$fuel == 0) {
                    hotncold$extinguish();
                }
                if (hotncold$fuel % 20 == 0) {
                    markForClient();
                }
            }
            ci.cancel();
        }
    }

    @Inject(method = "cook", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$requireFuelToCook(int inventoryCount, CallbackInfo ci) {
        if (hotncold$isFuelEnabled() && hotncold$fuel == 0) {
            // Also applies if another mod cancels the extinguishing event.
            ci.cancel();
        }
    }

    @Unique
    private void hotncold$extinguish() {
        BlockCampfire.updateCampfireBlockState(0, null, (TileEntityCampfire) (Object) this);
    }

    @Inject(method = "resetLife", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$keepFuelOnIgnition(boolean cooldown, CallbackInfo ci) {
        if (hotncold$isFuelEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "canBeReignited", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$noFreeRefills(CallbackInfoReturnable<Boolean> cir) {
        if (hotncold$isFuelEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBurnOut", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$signalFiresUseFuel(CallbackInfoReturnable<Boolean> cir) {
        if (hotncold$isFuelEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getLife", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$displayRemainingFuel(CallbackInfoReturnable<Integer> cir) {
        if (hotncold$isFuelEnabled()) {
            cir.setReturnValue(hotncold$fuel);
        }
    }

    @Inject(method = { "getStartingLife", "getBaseBurnOutTimer" }, at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$displayFuelCapacity(CallbackInfoReturnable<Integer> cir) {
        if (hotncold$isFuelEnabled()) {
            cir.setReturnValue(FUEL_CAPACITY);
        }
    }

    @Inject(method = "readFromNBTIfItExists", at = @At("TAIL"), require = 1)
    private void hotncold$readFuel(NBTTagCompound tag, CallbackInfo ci) {
        if (tag.hasKey(FUEL_TAG, 99)) {
            hotncold$fuel = MathHelper.clamp_int(tag.getInteger(FUEL_TAG), 0, FUEL_CAPACITY);
        }
        hotncold$clientEnabled = tag.getBoolean(ENABLED_TAG);
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = true, require = 1)
    private void hotncold$writeFuel(NBTTagCompound tag, CallbackInfo ci) {
        tag.setInteger(FUEL_TAG, hotncold$fuel);
        tag.setBoolean(ENABLED_TAG, hotncold$isFuelEnabled());
    }

    @Redirect(
        method = "drop",
        at = @At(
            value = "INVOKE",
            target = "Lconnor135246/campfirebackport/common/recipes/CampfireRecipe;getOutput()Lnet/minecraft/item/ItemStack;"),
        require = 1)
    private ItemStack hotncold$possiblyBurnFood(CampfireRecipe recipe) {
        ItemStack output = recipe.getOutput();
        if (hotncold$isFuelEnabled() && !worldObj.isRemote
            && output != null
            && output.getItem() instanceof ItemFood
            && worldObj.rand.nextFloat() < Config.campfireFoodBurnChance) {
            return new ItemStack(FoodListMF.burnt_food, output.stackSize);
        }
        return output;
    }

    @Override
    public int getMetreScale(int size) {
        return hotncold$fuel * size / FUEL_CAPACITY;
    }

    @Override
    public boolean shouldShowMetre() {
        return hotncold$isFuelEnabled() && hotncold$fuel > 0;
    }

    @Override
    public String getLocalisedName() {
        int seconds = hotncold$fuel / 20;
        return StatCollector.translateToLocal("forge.fuel.name") + " "
            + seconds / 60
            + ":"
            + (seconds % 60 < 10 ? "0" : "")
            + seconds % 60;
    }
}
