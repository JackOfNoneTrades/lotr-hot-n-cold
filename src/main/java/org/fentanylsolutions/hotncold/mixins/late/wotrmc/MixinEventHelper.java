package org.fentanylsolutions.hotncold.mixins.late.wotrmc;

import net.minecraftforge.event.entity.living.LivingEvent;

import org.fentanylsolutions.hotncold.HotNCold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.LOTREventHandler;
import wotrmc.coremod.WOTRMCHooks;

@Mixin(value = LOTREventHandler.class, remap = false)
public class MixinEventHelper {

    @Redirect(
        method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At(
            value = "INVOKE",
            target = "Lwotrmc/coremod/WOTRMCHooks;handleEntityFrostImmunity(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)Z"),
        require = 0)
    private boolean hotncold$overrideWOTRMCFrost(LivingEvent.LivingUpdateEvent event) {
        if (HotNCold.mobsImmuneToFrost.contains(event.entity.getClass())) {
            return false;
        }
        return WOTRMCHooks.handleEntityFrostImmunity(event);
    }
}
