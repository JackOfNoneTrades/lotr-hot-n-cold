package org.fentanylsolutions.hotncold.mixins.late.lotr;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.fentanylsolutions.hotncold.HotNCold;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.LOTREventHandler;
import lotr.common.entity.npc.LOTREntityNPC;

@Mixin(value = LOTREventHandler.class, remap = false)
public class MixinEventHelper {

    @ModifyVariable(
        method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At(value = "STORE", ordinal = 11 // Adjust this if needed
        ),
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
        System.out.println(
            "Class " + entity.getClass()
                + " found in mobsImmuneToFrost: "
                + HotNCold.mobsImmuneToFrost.contains(entity.getClass()));
        return HotNCold.mobsImmuneToFrost.contains(entity.getClass());
    }
}
