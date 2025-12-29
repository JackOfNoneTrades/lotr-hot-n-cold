package org.fentanylsolutions.hotncold.mixins.late.lotr;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.fentanylsolutions.hotncold.HotNCold;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import lotr.common.LOTREventHandler;
import lotr.common.entity.npc.LOTREntityNPC;

@SuppressWarnings("unused")
@Mixin(value = LOTREventHandler.class, remap = false)
public class MixinEventHelper {

    @ModifyVariable(
        method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At(value = "STORE", ordinal = 11),
        name = "flag")
    private boolean modifyFrostFlag(boolean flag, LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.entityLiving;

        if (shouldBeImmuneToFrost(entity)) {
            return false;
        }

        return flag;
    }

    @Redirect(
        method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At(
            value = "FIELD",
            target = "Llotr/common/entity/npc/LOTREntityNPC;isImmuneToFrost:Z",
            opcode = Opcodes.GETFIELD))
    private boolean redirectIsImmuneToFrost(LOTREntityNPC npc) {
        if (npc.isImmuneToFrost) {
            return true;
        }
        return shouldBeImmuneToFrost(npc);
    }

    private boolean shouldBeImmuneToFrost(EntityLivingBase entity) {
        if (entity instanceof LOTREntityNPC && ((LOTREntityNPC) entity).isImmuneToFrost) {
            return true;
        }
        return HotNCold.mobsImmuneToFrost.contains(entity.getClass());
    }

    @WrapOperation(
        method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At(value = "CONSTANT", args = "classValue=lotr.common.world.biome.LOTRBiomeGenNearHarad$ImmuneToHeat"))
    private boolean hotncold$wrapImmuneToHeat(Object obj, Operation<Boolean> original,
        LivingEvent.LivingUpdateEvent event) {
        if (HotNCold.mobsImmuneToHeat.contains(event.entity.getClass())) {
            return true;
        }

        return original.call(obj);
    }

    @WrapOperation(
        method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At(value = "CONSTANT", args = "classValue=lotr.common.world.biome.LOTRBiomeGenForodwaith"))
    private boolean frostBiomeCheckWrap(Object obj, Operation<Boolean> original, LivingEvent.LivingUpdateEvent event) {

        if (HotNCold.frostBiomes.contains(((BiomeGenBase) obj).biomeID)) {
            return true;
        }

        return original.call(obj);
    }

    @WrapOperation(
        method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At(value = "CONSTANT", args = "classValue=lotr.common.world.biome.LOTRBiomeGenNearHarad"))
    private boolean heatBiomeCheckWrap(Object obj, Operation<Boolean> original, LivingEvent.LivingUpdateEvent event) {

        if (HotNCold.heatBiomes.contains(((BiomeGenBase) obj).biomeID)) {
            return true;
        }

        return original.call(obj);
    }
}
