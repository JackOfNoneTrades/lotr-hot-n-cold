package org.fentanylsolutions.hotncold.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import org.fentanylsolutions.hotncold.HotNCold;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import lotr.common.LOTRShields;
import lotr.common.entity.npc.LOTREntityNPC;

/** LOTR's shield is neither ordinary inventory nor synchronized entity data. */
public final class LOTRNPCShieldSync {

    private static final String SHIELD_KEY = "hotncold:configuredShield";
    private static SimpleNetworkWrapper network;

    private LOTRNPCShieldSync() {}

    public static void init() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel("hotncold_shield");
        network.registerMessage(Handler.class, ShieldMessage.class, 0, Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(new LOTRNPCShieldSync());
    }

    public static void setConfiguredShield(LOTREntityNPC npc, LOTRShields shield) {
        npc.npcShield = shield;
        // Forge persists this compound automatically; no current config is consulted on reload.
        npc.getEntityData()
            .setString(SHIELD_KEY, shield == null ? "empty" : shield.name());
        if (network != null && npc.worldObj instanceof WorldServer) {
            for (EntityPlayer player : ((WorldServer) npc.worldObj).getEntityTracker()
                .getTrackingPlayers(npc)) {
                network.sendTo(new ShieldMessage(npc), (EntityPlayerMP) player);
            }
        }
    }

    public static void restoreConfiguredShield(LOTREntityNPC npc) {
        if (npc.getEntityData()
            .hasKey(SHIELD_KEY, 8)) {
            String name = npc.getEntityData()
                .getString(SHIELD_KEY);
            LOTRShields shield = LOTRShields.shieldForName(name);
            if (shield != null || "empty".equals(name)) {
                npc.npcShield = shield;
            }
        }
    }

    @SubscribeEvent
    public void construct(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof LOTREntityNPC) {
            final LOTREntityNPC npc = (LOTREntityNPC) event.entity;
            npc.registerExtendedProperties(SHIELD_KEY, new IExtendedEntityProperties() {

                @Override
                public void saveNBTData(NBTTagCompound tag) {}

                @Override
                public void loadNBTData(NBTTagCompound tag) {
                    restoreConfiguredShield(npc);
                }

                @Override
                public void init(Entity entity, World world) {}
            });
        }
    }

    @SubscribeEvent
    public void joinWorld(EntityJoinWorldEvent event) {
        if (!event.world.isRemote && event.entity instanceof LOTREntityNPC) {
            restoreConfiguredShield((LOTREntityNPC) event.entity);
        }
    }

    @SubscribeEvent
    public void startTracking(PlayerEvent.StartTracking event) {
        if (event.target instanceof LOTREntityNPC && event.entityPlayer instanceof EntityPlayerMP) {
            LOTREntityNPC npc = (LOTREntityNPC) event.target;
            if (npc.getEntityData()
                .hasKey(SHIELD_KEY, 8)) {
                network.sendTo(new ShieldMessage(npc), (EntityPlayerMP) event.entityPlayer);
            }
        }
    }

    public static final class ShieldMessage implements IMessage {

        private int entityId;
        private int dimension;
        private String shieldName;

        public ShieldMessage() {}

        private ShieldMessage(LOTREntityNPC npc) {
            entityId = npc.getEntityId();
            dimension = npc.worldObj.provider.dimensionId;
            shieldName = npc.npcShield == null ? "empty" : npc.npcShield.name();
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            dimension = buffer.readInt();
            shieldName = ByteBufUtils.readUTF8String(buffer);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeInt(dimension);
            ByteBufUtils.writeUTF8String(buffer, shieldName);
        }
    }

    public static final class Handler implements IMessageHandler<ShieldMessage, IMessage> {

        @Override
        public IMessage onMessage(ShieldMessage message, MessageContext context) {
            HotNCold.proxy.receiveNPCShield(message.entityId, message.dimension, message.shieldName);
            return null;
        }
    }
}
