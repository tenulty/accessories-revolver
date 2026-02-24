package io.wispforest.accessories.endec;

import ;
import com.mojang.datafixers.util.Function3;
import io.netty.buffer.Unpooled;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.format.gson.GsonEndec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class MinecraftEndecs {
    private MinecraftEndecs() {}

    // --- MC Types ---

    public static final Endec<FriendlyByteBuf> PACKET_BYTE_BUF = Endec.BYTES
            .xmap(bytes -> {
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                buffer.writeBytes(bytes);

                return buffer;
            }, buffer -> {
                var bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);

                return bytes;
            });

    public static final Endec<ResourceLocation> IDENTIFIER = Endec.STRING.xmap(s -> {
        var location = ResourceLocation.m_135820_(s);

        if (location == null) throw new IllegalStateException("Unable to read the given string as a valid ResourceLocation! [Value: " + s + "]");

        return location;
    }, ResourceLocation::toString);
    public static final Endec<ItemStack> ITEM_STACK = CodecUtils.ofCodec(ItemStack.f_41582_);
    public static final Endec<Component> TEXT = GsonEndec.INSTANCE.xmap(Component.Serializer::m_130691_, Component.Serializer::m_130716_);

    public static final Endec<Vec3i> VEC3I = vectorEndec("Vec3i", Endec.INT, Vec3i::new, Vec3i::m_123341_, Vec3i::m_123342_, Vec3i::m_123343_);
    public static final Endec<Vec3> VEC3D = vectorEndec("Vec3d", Endec.DOUBLE, Vec3::new, Vec3::m_7096_, Vec3::m_7098_, Vec3::m_7094_);
    public static final Endec<Vector3f> VECTOR3F = vectorEndec("Vector3f", Endec.FLOAT, Vector3f::new, Vector3f::x, Vector3f::y, Vector3f::z);

    public static final Endec<BlockPos> BLOCK_POS = Endec
            .ifAttr(
                    SerializationAttributes.HUMAN_READABLE,
                    vectorEndec("BlockPos", Endec.INT, BlockPos::new, BlockPos::m_123341_, BlockPos::m_123342_, BlockPos::m_123343_)
            ).orElse(
                    Endec.LONG.xmap(BlockPos::m_122022_, BlockPos::m_121878_)
            );

    public static final Endec<ChunkPos> CHUNK_POS = Endec
            .ifAttr(
                    SerializationAttributes.HUMAN_READABLE,
                    Endec.INT.listOf().validate(ints -> {
                        if (ints.size() != 2) {
                            throw new IllegalStateException("ChunkPos array must have two elements");
                        }
                    }).xmap(
                            ints -> new ChunkPos(ints.get(0), ints.get(1)),
                            chunkPos -> List.of(chunkPos.f_45578_, chunkPos.f_45579_)
                    )
            )
            .orElse(Endec.LONG.xmap(ChunkPos::new, ChunkPos::m_45588_));

    public static final Endec<BlockHitResult> BLOCK_HIT_RESULT = StructEndecBuilder.of(
            VEC3D.fieldOf("pos", BlockHitResult::m_82450_),
            Endec.forEnum(Direction.class).fieldOf("side", BlockHitResult::m_82434_),
            BLOCK_POS.fieldOf("block_pos", BlockHitResult::m_82425_),
            Endec.BOOLEAN.fieldOf("inside_block", BlockHitResult::m_82436_),
            Endec.BOOLEAN.fieldOf("missed", $ -> $.m_6662_() == HitResult.Type.MISS),
            (pos, side, blockPos, insideBlock, missed) -> !missed
                    ? new BlockHitResult(pos, side, blockPos, insideBlock)
                    : BlockHitResult.m_82426_(pos, side, blockPos)
    );

    // --- Constructors for MC types ---

    public static ReflectiveEndecBuilder withExtra(ReflectiveEndecBuilder builder) {
        builder.register(PACKET_BYTE_BUF, FriendlyByteBuf.class);

        builder.register(IDENTIFIER, ResourceLocation.class)
                .register(ITEM_STACK, ItemStack.class)
                .register(TEXT, Component.class);

        builder.register(VEC3I, Vec3i.class)
                .register(VEC3D, Vec3.class)
                .register(VECTOR3F, Vector3f.class);

        builder.register(BLOCK_POS, BlockPos.class)
                .register(CHUNK_POS, ChunkPos.class);

        builder.register(BLOCK_HIT_RESULT, BlockHitResult.class);

        return builder;
    }

    public static <T> Endec<T> ofRegistry(Registry<T> registry) {
        return IDENTIFIER.xmap(registry::m_7745_, registry::m_7981_);
    }

    public static <T> Endec<TagKey<T>> unprefixedTagKey(ResourceKey<? extends Registry<T>> registry) {
        return IDENTIFIER.xmap(id -> TagKey.m_203882_(registry, id), TagKey::f_203868_);
    }

    public static <T> Endec<TagKey<T>> prefixedTagKey(ResourceKey<? extends Registry<T>> registry) {
        return Endec.STRING.xmap(
                s -> {
                    var location = ResourceLocation.m_135820_(s.substring(1));

                    if (location == null) throw new IllegalStateException("Unable to read the given string as a valid ResourceLocation! [Value: " + s + "]");

                    return TagKey.m_203882_(registry, location);
                },
                tag -> "#" + tag.f_203868_()
        );
    }

    private static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, Function3<C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter) {
        return componentEndec.listOf().validate(ints -> {
            if (ints.size() != 3) {
                throw new IllegalStateException(name + " array must have three elements");
            }
        }).xmap(
                components -> constructor.apply(components.get(0), components.get(1), components.get(2)),
                vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector))
        );
    }

    public static <E extends Enum<E> & StringRepresentable> Endec<E> forEnumStringRepresentable(Class<E> enumClass) {
        return Endec.ifAttr(
                SerializationAttributes.HUMAN_READABLE,
                Endec.STRING.xmap(name -> Arrays.stream(enumClass.getEnumConstants()).filter(e -> e.m_7912_().equals(name)).findFirst().get(), StringRepresentable::m_7912_)
        ).orElse(
                Endec.VAR_INT.xmap(ordinal -> enumClass.getEnumConstants()[ordinal], Enum::ordinal)
        );
    }


    public static final ReflectiveEndecBuilder INSTANCE = withExtra(new ReflectiveEndecBuilder());
}
