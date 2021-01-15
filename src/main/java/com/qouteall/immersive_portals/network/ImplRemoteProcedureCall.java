package com.qouteall.immersive_portals.network;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.qouteall.hiding_in_the_bushes.MyNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ImplRemoteProcedureCall {
    public static final Gson gson;
    
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    
    private static final ImmutableMap<Class, BiConsumer<PacketBuffer, Object>> serializerMap;
    private static final ImmutableMap<Type, Function<PacketBuffer, Object>> deserializerMap;
    
    private static final JsonParser jsonParser = new JsonParser();
    
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
        
        serializerMap = ImmutableMap.<Class, BiConsumer<PacketBuffer, Object>>builder()
            .put(ResourceLocation.class, (buf, o) -> buf.writeResourceLocation(((ResourceLocation) o)))
            .put(RegistryKey.class, (buf, o) -> buf.writeResourceLocation(((RegistryKey) o).func_240901_a_()))
            .put(BlockPos.class, (buf, o) -> buf.writeBlockPos(((BlockPos) o)))
            .put(Vector3d.class, (buf, o) -> {
                Vector3d vec = (Vector3d) o;
                buf.writeDouble(vec.x);
                buf.writeDouble(vec.y);
                buf.writeDouble(vec.z);
            })
            .put(UUID.class, (buf, o) -> buf.writeUniqueId(((UUID) o)))
            .put(Block.class, (buf, o) -> serializeByCodec(buf, Registry.BLOCK, o))
            .put(Item.class, (buf, o) -> serializeByCodec(buf, Registry.ITEM, o))
            .put(BlockState.class, (buf, o) -> serializeByCodec(buf, BlockState.field_235877_b_, o))
            .put(ItemStack.class, (buf, o) -> serializeByCodec(buf, ItemStack.field_234691_a_, o))
            .put(CompoundNBT.class, (buf, o) -> buf.writeCompoundTag(((CompoundNBT) o)))
            .put(ITextComponent.class, (buf, o) -> buf.writeTextComponent(((ITextComponent) o)))
            .build();
        
        deserializerMap = ImmutableMap.<Type, Function<PacketBuffer, Object>>builder()
            .put(ResourceLocation.class, buf -> buf.readResourceLocation())
            .put(
                new TypeToken<RegistryKey<World>>() {}.getType(),
                buf -> RegistryKey.func_240903_a_(
                    Registry.field_239699_ae_, buf.readResourceLocation()
                )
            )
            .put(
                new TypeToken<RegistryKey<Biome>>() {}.getType(),
                buf -> RegistryKey.func_240903_a_(
                    Registry.field_239720_u_, buf.readResourceLocation()
                )
            )
            .put(BlockPos.class, buf -> buf.readBlockPos())
            .put(Vector3d.class, buf ->
                new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
            )
            .put(UUID.class, buf -> buf.readUniqueId())
            .put(Block.class, buf -> deserializeByCodec(buf, Registry.BLOCK))
            .put(Item.class, buf -> deserializeByCodec(buf, Registry.ITEM))
            .put(BlockState.class, buf -> deserializeByCodec(buf, BlockState.field_235877_b_))
            .put(ItemStack.class, buf -> deserializeByCodec(buf, ItemStack.field_234691_a_))
            .put(CompoundNBT.class, buf -> buf.readCompoundTag())
            .put(ITextComponent.class, buf -> buf.readTextComponent())
            .build();
    }
    
    private static Object deserializeByCodec(PacketBuffer buf, Codec codec) {
        String jsonString = buf.readString();
        JsonElement jsonElement = jsonParser.parse(jsonString);
        
        return codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(
            false, e -> {throw new RuntimeException(e.toString());}
        );
    }
    
    private static Object deserialize(PacketBuffer buf, Type type) {
        Function<PacketBuffer, Object> deserializer = deserializerMap.get(type);
        if (deserializer == null) {
            String json = buf.readString();
            return gson.fromJson(json, type);
        }
        
        return deserializer.apply(buf);
    }
    
    private static void serialize(PacketBuffer buf, Object object) {
        BiConsumer<PacketBuffer, Object> serializer = serializerMap.get(object.getClass());
        
        if (serializer == null) {
            serializer = serializerMap.entrySet().stream().filter(
                e -> e.getKey().isAssignableFrom(object.getClass())
            ).findFirst().map(Map.Entry::getValue).orElse(null);
        }
        
        if (serializer == null) {
            String json = gson.toJson(object);
            buf.writeString(json);
            return;
        }
        
        serializer.accept(buf, object);
    }
    
    private static void serializeByCodec(PacketBuffer buf, Codec codec, Object object) {
        JsonElement result = (JsonElement) codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow(
            false, e -> {
                throw new RuntimeException(e.toString());
            }
        );
        
        String jsonString = gson.toJson(result);
        buf.writeString(jsonString);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static CCustomPayloadPacket createC2SPacket(
        String methodPath,
        Object... arguments
    ) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        
        serializeStringWithArguments(methodPath, arguments, buf);
        
        return new CCustomPayloadPacket(MyNetwork.id_ctsRemote, buf);
    }
    
    public static SCustomPayloadPlayPacket createS2CPacket(
        String methodPath,
        Object... arguments
    ) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        
        serializeStringWithArguments(methodPath, arguments, buf);
        
        return new SCustomPayloadPlayPacket(MyNetwork.id_stcRemote, buf);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static Runnable clientReadFunctionAndArguments(PacketBuffer buf) {
        String methodPath = buf.readString();
        
        Method method = getMethodByPath(methodPath);
        
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        
        Object[] arguments = new Object[genericParameterTypes.length];
        
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type parameterType = genericParameterTypes[i];
            Object obj = deserialize(buf, parameterType);
            arguments[i] = obj;
        }
        
        return () -> {
            try {
                method.invoke(null, arguments);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    public static Runnable serverReadFunctionAndArguments(ServerPlayerEntity player, PacketBuffer buf) {
        String methodPath = buf.readString();
        
        Method method = getMethodByPath(methodPath);
        
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        
        Object[] arguments = new Object[genericParameterTypes.length];
        arguments[0] = player;
        
        //the first argument is the player
        for (int i = 1; i < genericParameterTypes.length; i++) {
            Type parameterType = genericParameterTypes[i];
            Object obj = deserialize(buf, parameterType);
            arguments[i] = obj;
        }
        
        return () -> {
            try {
                method.invoke(null, arguments);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    private static void serializeStringWithArguments(
        String methodPath, Object[] arguments, PacketBuffer buf
    ) {
        buf.writeString(methodPath);
        
        for (Object argument : arguments) {
            serialize(buf, argument);
        }
    }
    
    private static Method getMethodByPath(String methodPath) {
        Method result = methodCache.get(methodPath);
        if (result != null) {
            return result;
        }
        
        //because it may throw exception, does not use computeIfAbsent
        Method method = findMethodByPath(methodPath);
        Validate.notNull(method);
        
        methodCache.put(methodPath, method);
        return method;
    }
    
    private static Method findMethodByPath(String methodPath) {
        int lastDotIndex = methodPath.lastIndexOf('.');
        
        Validate.isTrue(lastDotIndex != -1);
        String classPath = methodPath.substring(0, lastDotIndex);
        String methodName = methodPath.substring(lastDotIndex + 1);
        
        if (!classPath.contains("RemoteCallable")) {
            throw new RuntimeException("The class path must contain \"RemoteCallable\"");
        }
        
        Class<?> aClass;
        try {
            aClass = Class.forName(classPath);
        }
        catch (ClassNotFoundException e) {
            int dotIndex = classPath.lastIndexOf('.');
            if (dotIndex != -1) {
                String newClassPath =
                    classPath.substring(0, dotIndex) + "$" + classPath.substring(dotIndex + 1);
                try {
                    aClass = Class.forName(newClassPath);
                }
                catch (ClassNotFoundException e1) {
                    throw new RuntimeException("Cannot find class " + classPath, e);
                }
            }
            else {
                throw new RuntimeException("Cannot find class " + classPath, e);
            }
        }
        
        Method method = Arrays.stream(aClass.getMethods()).filter(
            m -> m.getName().equals(methodName)
        ).findFirst().orElseThrow(() -> new RuntimeException(
            "Cannot find method " + methodPath
        ));
        
        return method;
    }
    
}
