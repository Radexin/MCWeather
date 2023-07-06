package net.radexin.mcweather;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.awt.*;

@Mod(MCWeather.MOD_ID)
public class MCWeather
{
    public static final String MOD_ID = "mcweather";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int tickCounter = -1;

    public MCWeather()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static void updateWeather()
    {
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Real weather updating..."));
        Minecraft.getInstance().level.setRainLevel(1);
        Minecraft.getInstance().level.getLevelData().setRaining(true);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Check for terranew mod which is required for calculating position
        /*
        if (ModList.get().isLoaded("terranew"))
        {

        }
        */
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase == TickEvent.Phase.END) {
                //tickCounter++;
                if (tickCounter==-1||tickCounter>=6000)
                {
                    if (Minecraft.getInstance().level != null) {
                        tickCounter = 0;
                        updateWeather();
                    }
                }
            }
        }
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }
}
