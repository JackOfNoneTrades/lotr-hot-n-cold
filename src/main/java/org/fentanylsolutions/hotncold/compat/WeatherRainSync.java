package org.fentanylsolutions.hotncold.compat;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

/** Synchronizes the native weather phase; Weather 2's spatial storm simulation remains its own. */
public final class WeatherRainSync {

    private static SimpleNetworkWrapper network;

    public static void initialize() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel("hotncold_rain");
        network.registerMessage(Handler.class, RainMessage.class, 0, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new WeatherRainSync());
    }

    @SubscribeEvent
    public void tick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote || event.phase != TickEvent.Phase.END) return;
        boolean delayed = WeatherRainDelay.shouldDelay(world);
        WeatherRainDelay.advance(world);
        // Include transitions even between periodic updates (especially the first dry tick).
        if (world.getTotalWorldTime() % 20 == 0 || delayed != WeatherRainDelay.shouldDelay(world)
            || WeatherRainDelay.state(world).wetTicks == 1
            || (WeatherRainDelay.state(world).previousServerRain > 0
                && WeatherRainDelay.rawRainStrength(world, 1) <= 0)) {
            network.sendToDimension(new RainMessage(world), world.provider.dimensionId);
        }
        WeatherRainDelay.state(world).previousServerRain = WeatherRainDelay.rawRainStrength(world, 1);
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        send(event.player);
    }

    @SubscribeEvent
    public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        send(event.player);
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        send(event.player);
    }

    private static void send(net.minecraft.entity.player.EntityPlayer player) {
        if (player instanceof EntityPlayerMP) network.sendTo(new RainMessage(player.worldObj), (EntityPlayerMP) player);
    }

    public static final class RainMessage implements IMessage {

        private int dimension;
        private boolean enabled;
        private int delay;
        private float rain;
        private int wetTicks;

        public RainMessage() {}

        public RainMessage(World world) {
            dimension = world.provider.dimensionId;
            enabled = Config.enableWeather2VanillaRain;
            delay = Config.weather2RainDelaySeconds;
            rain = WeatherRainDelay.rawRainStrength(world, 1);
            wetTicks = WeatherRainDelay.state(world).wetTicks;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt();
            enabled = buffer.readBoolean();
            delay = buffer.readInt();
            rain = buffer.readFloat();
            wetTicks = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension);
            buffer.writeBoolean(enabled);
            buffer.writeInt(delay);
            buffer.writeFloat(rain);
            buffer.writeInt(wetTicks);
        }

        public void apply(World world) {
            if (world == null || world.provider.dimensionId != dimension) return;
            WeatherRainState state = WeatherRainDelay.state(world);
            state.synchronizedWeather = true;
            state.serverEnabled = enabled;
            state.serverDelay = delay;
            state.serverRain = rain;
            state.serverWetTicks = wetTicks;
            if (enabled && !WeatherRainDelay.hasLocalStorms(world)) {
                world.setRainStrength(rain);
                world.getWorldInfo()
                    .setRaining(world.isRaining());
            }
        }
    }

    public static final class Handler implements IMessageHandler<RainMessage, IMessage> {

        @Override
        public IMessage onMessage(RainMessage message, MessageContext context) {
            HotNCold.proxy.receiveRain(message);
            return null;
        }
    }
}
