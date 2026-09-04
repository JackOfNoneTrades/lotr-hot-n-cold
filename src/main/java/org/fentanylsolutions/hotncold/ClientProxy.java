package org.fentanylsolutions.hotncold;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import lotr.common.LOTRShields;
import lotr.common.entity.npc.LOTREntityNPC;

public class ClientProxy extends CommonProxy {

    @Override
    public void receiveNPCShield(int entityId, int dimension, String shieldName) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            if (mc.theWorld == null || mc.theWorld.provider.dimensionId != dimension) {
                return;
            }
            Entity entity = mc.theWorld.getEntityByID(entityId);
            if (entity instanceof LOTREntityNPC) {
                LOTRShields shield = LOTRShields.shieldForName(shieldName);
                if (shield != null || "empty".equals(shieldName)) {
                    ((LOTREntityNPC) entity).npcShield = shield;
                }
            }
        });
    }

}
