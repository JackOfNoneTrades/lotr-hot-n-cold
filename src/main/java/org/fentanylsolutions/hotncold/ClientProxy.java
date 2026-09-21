package org.fentanylsolutions.hotncold;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import lotr.common.LOTRShields;
import lotr.common.entity.npc.LOTREntityNPC;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(cpw.mods.fml.common.event.FMLInitializationEvent event) {
        super.init(event);
        if (cpw.mods.fml.common.Loader.isModLoaded("weather2")) {
            org.fentanylsolutions.hotncold.compat.WeatherRainClient.initialize();
        }
    }

    @Override
    public void receiveRain(org.fentanylsolutions.hotncold.compat.WeatherRainSync.RainMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> message.apply(mc.theWorld));
    }

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
