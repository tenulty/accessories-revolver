package io.wispforest.accessories.endec.format.nbt;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SelfDescribedSerializer;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.util.RecursiveSerializer;
import io.wispforest.endec.util.VarInts;
import net.minecraft.nbt.*;

import java.util.Optional;

public class NbtSerializer extends RecursiveSerializer<Tag> implements SelfDescribedSerializer<Tag> {

    protected Tag prefix;

    protected NbtSerializer(Tag prefix) {
        super(EndTag.f_128534_);
        this.prefix = prefix;
    }

    public static NbtSerializer of(Tag prefix) {
        return new NbtSerializer(prefix);
    }

    public static NbtSerializer of() {
        return of(null);
    }

    // ---

    @Override
    public void writeByte(SerializationContext ctx, byte value) {
        this.consume(ByteTag.m_128266_(value));
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        this.consume(ShortTag.m_129258_(value));
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        this.consume(IntTag.m_128679_(value));
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        this.consume(LongTag.m_128882_(value));
    }

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        this.consume(FloatTag.m_128566_(value));
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        this.consume(DoubleTag.m_128500_(value));
    }

    // ---

    @Override
    public void writeVarInt(SerializationContext ctx, int value) {
        this.consume(switch (VarInts.getSizeInBytesFromInt(value)) {
            case 0, 1 -> ByteTag.m_128266_((byte) value);
            case 2 -> ShortTag.m_129258_((short) value);
            default -> IntTag.m_128679_(value);
        });
    }

    @Override
    public void writeVarLong(SerializationContext ctx, long value) {
        this.consume(switch (VarInts.getSizeInBytesFromLong(value)) {
            case 0, 1 -> ByteTag.m_128266_((byte) value);
            case 2 -> ShortTag.m_129258_((short) value);
            case 3, 4 -> IntTag.m_128679_((int) value);
            default -> LongTag.m_128882_(value);
        });
    }

    // ---

    @Override
    public void writeBoolean(SerializationContext ctx, boolean value) {
        this.consume(ByteTag.m_128273_(value));
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        this.consume(StringTag.m_129297_(value));
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        this.consume(new ByteArrayTag(bytes));
    }

    @Override
    public <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional) {
        if (this.isWritingStructField()) {
            optional.ifPresent(v -> endec.encode(ctx, this, v));
        } else {
            try (var struct = this.struct()) {
                struct.field("present", ctx, Endec.BOOLEAN, optional.isPresent());
                optional.ifPresent(value -> struct.field("value", ctx, endec, value));
            }
        }
    }

    // ---

    @Override
    public <E> Serializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec, int size) {
        return new io.wispforest.accessories.endec.format.nbt.NbtSerializer.Sequence<>(ctx, elementEndec);
    }

    @Override
    public <V> Serializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec, int size) {
        return new io.wispforest.accessories.endec.format.nbt.NbtSerializer.Map<>(ctx, valueEndec);
    }

    @Override
    public Struct struct() {
        return new io.wispforest.accessories.endec.format.nbt.NbtSerializer.Map<>(null, null);
    }

    // ---

    private class Map<V> implements Serializer.Map<V>, Struct {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final CompoundTag result;

        private Map(SerializationContext ctx, Endec<V> valueEndec) {
            this.ctx = ctx;
            this.valueEndec = valueEndec;

            if (NbtSerializer.this.prefix != null) {
                if (NbtSerializer.this.prefix instanceof CompoundTag prefixMap) {
                    this.result = prefixMap;
                } else {
                    throw new IllegalStateException("Incompatible prefix of type " + NbtSerializer.this.prefix.getClass().getSimpleName() + " provided for NBT map/struct");
                }
            } else {
                this.result = new CompoundTag();
            }
        }

        @Override
        public void entry(String key, V value) {
            NbtSerializer.this.frame(encoded -> {
                this.valueEndec.encode(this.ctx, NbtSerializer.this, value);
                this.result.m_128365_(key, encoded.require("map value"));
            }, false);
        }

        @Override
        public <F> Struct field(String name, SerializationContext ctx, Endec<F> endec, F value) {
            NbtSerializer.this.frame(encoded -> {
                endec.encode(ctx, NbtSerializer.this, value);
                if (encoded.wasEncoded()) {
                    this.result.m_128365_(name, encoded.get());
                }
            }, true);

            return this;
        }

        @Override
        public void end() {
            NbtSerializer.this.consume(this.result);
        }
    }

    private class Sequence<V> implements Serializer.Sequence<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final ListTag result;

        private Sequence(SerializationContext ctx, Endec<V> valueEndec) {
            this.ctx = ctx;
            this.valueEndec = valueEndec;

            if (NbtSerializer.this.prefix != null) {
                if (NbtSerializer.this.prefix instanceof ListTag prefixList) {
                    this.result = prefixList;
                } else {
                    throw new IllegalStateException("Incompatible prefix of type " + NbtSerializer.this.prefix.getClass().getSimpleName() + " provided for NBT sequence");
                }
            } else {
                this.result = new ListTag();
            }
        }

        @Override
        public void element(V element) {
            NbtSerializer.this.frame(encoded -> {
                this.valueEndec.encode(this.ctx, NbtSerializer.this, element);
                this.result.add(encoded.require("sequence element"));
            }, false);
        }

        @Override
        public void end() {
            NbtSerializer.this.consume(this.result);
        }
    }
}
