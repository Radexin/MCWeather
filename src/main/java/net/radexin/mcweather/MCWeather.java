package net.radexin.mcweather;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.json.JsonObject;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public static String requestWeather(float x, float y) throws IOException {
        URL url = new URL("http://api.weatherapi.com/v1/current.json?key=&q="+x+","+y+"&aqi=no");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder responseData = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                responseData.append(inputLine);
            }
            in.close();

            return responseData.toString();
        } else {
            throw new IOException("Error: " + responseCode);
        }
    }

    public static void updateWeather()
    {
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Real weather updating..."));
        try {
            Gson g = new Gson();
            JsonObject weather = g.fromJson(requestWeather(1F,1F), JsonObject.class);
            String rawTime = weather.getJsonObject("location").getString("localtime");
            String condition = weather.getJsonObject("condition").getString("text");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                tickCounter++;
                if (tickCounter==-1||tickCounter>=6000)
                {
                    if (Minecraft.getInstance().level != null) {
                        tickCounter = 0;
                        updateWeather();
                    }
                }
            }
        }

        /*@SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            if (Minecraft.getInstance().level != null) {
                FriendlyByteBuf buffer = new FriendlyByteBuf(new ByteBuf() {
                    @Override
                    public int capacity() {
                        return 256;
                    }

                    @Override
                    public ByteBuf capacity(int newCapacity) {
                        return null;
                    }

                    @Override
                    public int maxCapacity() {
                        return 256;
                    }

                    @Override
                    public ByteBufAllocator alloc() {
                        return null;
                    }

                    @Override
                    public ByteOrder order() {
                        return null;
                    }

                    @Override
                    public ByteBuf order(ByteOrder endianness) {
                        return null;
                    }

                    @Override
                    public ByteBuf unwrap() {
                        return null;
                    }

                    @Override
                    public boolean isDirect() {
                        return false;
                    }

                    @Override
                    public boolean isReadOnly() {
                        return false;
                    }

                    @Override
                    public ByteBuf asReadOnly() {
                        return null;
                    }

                    @Override
                    public int readerIndex() {
                        return 0;
                    }

                    @Override
                    public ByteBuf readerIndex(int readerIndex) {
                        return null;
                    }

                    @Override
                    public int writerIndex() {
                        return 0;
                    }

                    @Override
                    public ByteBuf writerIndex(int writerIndex) {
                        return null;
                    }

                    @Override
                    public ByteBuf setIndex(int readerIndex, int writerIndex) {
                        return null;
                    }

                    @Override
                    public int readableBytes() {
                        return 0;
                    }

                    @Override
                    public int writableBytes() {
                        return 0;
                    }

                    @Override
                    public int maxWritableBytes() {
                        return 0;
                    }

                    @Override
                    public boolean isReadable() {
                        return false;
                    }

                    @Override
                    public boolean isReadable(int size) {
                        return false;
                    }

                    @Override
                    public boolean isWritable() {
                        return false;
                    }

                    @Override
                    public boolean isWritable(int size) {
                        return false;
                    }

                    @Override
                    public ByteBuf clear() {
                        return null;
                    }

                    @Override
                    public ByteBuf markReaderIndex() {
                        return null;
                    }

                    @Override
                    public ByteBuf resetReaderIndex() {
                        return null;
                    }

                    @Override
                    public ByteBuf markWriterIndex() {
                        return null;
                    }

                    @Override
                    public ByteBuf resetWriterIndex() {
                        return null;
                    }

                    @Override
                    public ByteBuf discardReadBytes() {
                        return null;
                    }

                    @Override
                    public ByteBuf discardSomeReadBytes() {
                        return null;
                    }

                    @Override
                    public ByteBuf ensureWritable(int minWritableBytes) {
                        return null;
                    }

                    @Override
                    public int ensureWritable(int minWritableBytes, boolean force) {
                        return 0;
                    }

                    @Override
                    public boolean getBoolean(int index) {
                        return false;
                    }

                    @Override
                    public byte getByte(int index) {
                        return 0;
                    }

                    @Override
                    public short getUnsignedByte(int index) {
                        return 0;
                    }

                    @Override
                    public short getShort(int index) {
                        return 0;
                    }

                    @Override
                    public short getShortLE(int index) {
                        return 0;
                    }

                    @Override
                    public int getUnsignedShort(int index) {
                        return 0;
                    }

                    @Override
                    public int getUnsignedShortLE(int index) {
                        return 0;
                    }

                    @Override
                    public int getMedium(int index) {
                        return 0;
                    }

                    @Override
                    public int getMediumLE(int index) {
                        return 0;
                    }

                    @Override
                    public int getUnsignedMedium(int index) {
                        return 0;
                    }

                    @Override
                    public int getUnsignedMediumLE(int index) {
                        return 0;
                    }

                    @Override
                    public int getInt(int index) {
                        return 0;
                    }

                    @Override
                    public int getIntLE(int index) {
                        return 0;
                    }

                    @Override
                    public long getUnsignedInt(int index) {
                        return 0;
                    }

                    @Override
                    public long getUnsignedIntLE(int index) {
                        return 0;
                    }

                    @Override
                    public long getLong(int index) {
                        return 0;
                    }

                    @Override
                    public long getLongLE(int index) {
                        return 0;
                    }

                    @Override
                    public char getChar(int index) {
                        return 0;
                    }

                    @Override
                    public float getFloat(int index) {
                        return 0;
                    }

                    @Override
                    public double getDouble(int index) {
                        return 0;
                    }

                    @Override
                    public ByteBuf getBytes(int index, ByteBuf dst) {
                        return null;
                    }

                    @Override
                    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf getBytes(int index, byte[] dst) {
                        return null;
                    }

                    @Override
                    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf getBytes(int index, ByteBuffer dst) {
                        return null;
                    }

                    @Override
                    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
                        return null;
                    }

                    @Override
                    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public CharSequence getCharSequence(int index, int length, Charset charset) {
                        return null;
                    }

                    @Override
                    public ByteBuf setBoolean(int index, boolean value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setByte(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setShort(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setShortLE(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setMedium(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setMediumLE(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setInt(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setIntLE(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setLong(int index, long value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setLongLE(int index, long value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setChar(int index, int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setFloat(int index, float value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setDouble(int index, double value) {
                        return null;
                    }

                    @Override
                    public ByteBuf setBytes(int index, ByteBuf src) {
                        return null;
                    }

                    @Override
                    public ByteBuf setBytes(int index, ByteBuf src, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf setBytes(int index, byte[] src) {
                        return null;
                    }

                    @Override
                    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf setBytes(int index, ByteBuffer src) {
                        return null;
                    }

                    @Override
                    public int setBytes(int index, InputStream in, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public ByteBuf setZero(int index, int length) {
                        return null;
                    }

                    @Override
                    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
                        return 0;
                    }

                    @Override
                    public boolean readBoolean() {
                        return false;
                    }

                    @Override
                    public byte readByte() {
                        return 0;
                    }

                    @Override
                    public short readUnsignedByte() {
                        return 0;
                    }

                    @Override
                    public short readShort() {
                        return 0;
                    }

                    @Override
                    public short readShortLE() {
                        return 0;
                    }

                    @Override
                    public int readUnsignedShort() {
                        return 0;
                    }

                    @Override
                    public int readUnsignedShortLE() {
                        return 0;
                    }

                    @Override
                    public int readMedium() {
                        return 0;
                    }

                    @Override
                    public int readMediumLE() {
                        return 0;
                    }

                    @Override
                    public int readUnsignedMedium() {
                        return 0;
                    }

                    @Override
                    public int readUnsignedMediumLE() {
                        return 0;
                    }

                    @Override
                    public int readInt() {
                        return 0;
                    }

                    @Override
                    public int readIntLE() {
                        return 0;
                    }

                    @Override
                    public long readUnsignedInt() {
                        return 0;
                    }

                    @Override
                    public long readUnsignedIntLE() {
                        return 0;
                    }

                    @Override
                    public long readLong() {
                        return 0;
                    }

                    @Override
                    public long readLongLE() {
                        return 0;
                    }

                    @Override
                    public char readChar() {
                        return 0;
                    }

                    @Override
                    public float readFloat() {
                        return 0;
                    }

                    @Override
                    public double readDouble() {
                        return 0;
                    }

                    @Override
                    public ByteBuf readBytes(int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf readSlice(int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf readRetainedSlice(int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf readBytes(ByteBuf dst) {
                        return null;
                    }

                    @Override
                    public ByteBuf readBytes(ByteBuf dst, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf readBytes(byte[] dst) {
                        return null;
                    }

                    @Override
                    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf readBytes(ByteBuffer dst) {
                        return null;
                    }

                    @Override
                    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
                        return null;
                    }

                    @Override
                    public int readBytes(GatheringByteChannel out, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public CharSequence readCharSequence(int length, Charset charset) {
                        return null;
                    }

                    @Override
                    public int readBytes(FileChannel out, long position, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public ByteBuf skipBytes(int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeBoolean(boolean value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeByte(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeShort(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeShortLE(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeMedium(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeMediumLE(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeInt(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeIntLE(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeLong(long value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeLongLE(long value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeChar(int value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeFloat(float value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeDouble(double value) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeBytes(ByteBuf src) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeBytes(ByteBuf src, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeBytes(byte[] src) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf writeBytes(ByteBuffer src) {
                        return null;
                    }

                    @Override
                    public int writeBytes(InputStream in, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public int writeBytes(FileChannel in, long position, int length) throws IOException {
                        return 0;
                    }

                    @Override
                    public ByteBuf writeZero(int length) {
                        return null;
                    }

                    @Override
                    public int writeCharSequence(CharSequence sequence, Charset charset) {
                        return 0;
                    }

                    @Override
                    public int indexOf(int fromIndex, int toIndex, byte value) {
                        return 0;
                    }

                    @Override
                    public int bytesBefore(byte value) {
                        return 0;
                    }

                    @Override
                    public int bytesBefore(int length, byte value) {
                        return 0;
                    }

                    @Override
                    public int bytesBefore(int index, int length, byte value) {
                        return 0;
                    }

                    @Override
                    public int forEachByte(ByteProcessor processor) {
                        return 0;
                    }

                    @Override
                    public int forEachByte(int index, int length, ByteProcessor processor) {
                        return 0;
                    }

                    @Override
                    public int forEachByteDesc(ByteProcessor processor) {
                        return 0;
                    }

                    @Override
                    public int forEachByteDesc(int index, int length, ByteProcessor processor) {
                        return 0;
                    }

                    @Override
                    public ByteBuf copy() {
                        return null;
                    }

                    @Override
                    public ByteBuf copy(int index, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf slice() {
                        return null;
                    }

                    @Override
                    public ByteBuf retainedSlice() {
                        return null;
                    }

                    @Override
                    public ByteBuf slice(int index, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf retainedSlice(int index, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuf duplicate() {
                        return null;
                    }

                    @Override
                    public ByteBuf retainedDuplicate() {
                        return null;
                    }

                    @Override
                    public int nioBufferCount() {
                        return 0;
                    }

                    @Override
                    public ByteBuffer nioBuffer() {
                        return null;
                    }

                    @Override
                    public ByteBuffer nioBuffer(int index, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuffer internalNioBuffer(int index, int length) {
                        return null;
                    }

                    @Override
                    public ByteBuffer[] nioBuffers() {
                        return new ByteBuffer[0];
                    }

                    @Override
                    public ByteBuffer[] nioBuffers(int index, int length) {
                        return new ByteBuffer[0];
                    }

                    @Override
                    public boolean hasArray() {
                        return false;
                    }

                    @Override
                    public byte[] array() {
                        return new byte[0];
                    }

                    @Override
                    public int arrayOffset() {
                        return 0;
                    }

                    @Override
                    public boolean hasMemoryAddress() {
                        return false;
                    }

                    @Override
                    public long memoryAddress() {
                        return 0;
                    }

                    @Override
                    public String toString(Charset charset) {
                        return null;
                    }

                    @NotNull
                    @Override
                    public String toString(int index, int length, Charset charset) {
                        return null;
                    }

                    @Override
                    public int hashCode() {
                        return 0;
                    }

                    @Override
                    public boolean equals(Object obj) {
                        return false;
                    }

                    @Override
                    public int compareTo(ByteBuf buffer) {
                        return 0;
                    }

                    @Override
                    public String toString() {
                        return null;
                    }

                    @Override
                    public ByteBuf retain(int increment) {
                        return null;
                    }

                    @Override
                    public ByteBuf retain() {
                        return null;
                    }

                    @Override
                    public ByteBuf touch() {
                        return null;
                    }

                    @Override
                    public ByteBuf touch(Object hint) {
                        return null;
                    }

                    @Override
                    public int refCnt() {
                        return 0;
                    }

                    @Override
                    public boolean release() {
                        return false;
                    }

                    @Override
                    public boolean release(int decrement) {
                        return false;
                    }
                }); // Create an empty buffer
                CompoundTag emptyTag = new CompoundTag(); // Create an empty compound tag
                Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer = tagOutput -> {
                };
                Minecraft.getInstance().level.getChunk(event.getChunk().getPos().x,event.getChunk().getPos().z).replaceWithPacketData(buffer, emptyTag, consumer);
                Supplier<FullChunkStatus> supplier = () -> FullChunkStatus.INACCESSIBLE;
                Minecraft.getInstance().level.getChunk(event.getChunk().getPos().x,event.getChunk().getPos().z).setFullStatus(supplier);
                Minecraft.getInstance().level.getChunk(event.getChunk().getPos().x,event.getChunk().getPos().z).setUnsaved(false);
                buffer.clear();
            }
        }*/
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }
}
