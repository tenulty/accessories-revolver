package io.wispforest.accessories.endec.format.nbt;

import io.wispforest.endec.*;
import io.wispforest.endec.util.RecursiveDeserializer;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class NbtDeserializer extends RecursiveDeserializer<Tag> implements SelfDescribedDeserializer<Tag> {

    protected NbtDeserializer(Tag element) {
        super(element);
    }

    public static NbtDeserializer of(Tag element) {
        return new NbtDeserializer(element);
    }

    private <N extends Tag> N getAs(Tag element, Class<N> clazz) {
        if (clazz.isInstance(element)) {
            return clazz.cast(element);
        } else {
            throw new IllegalStateException("Expected a " + clazz.getSimpleName() + ", found a " + element.getClass().getSimpleName());
        }
    }

    // ---

    @Override
    public byte readByte(SerializationContext ctx) {
        return this.getAs(this.getValue(), ByteTag.class).m_7063_();
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return this.getAs(this.getValue(), ShortTag.class).m_7053_();
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return this.getAs(this.getValue(), IntTag.class).m_7047_();
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return this.getAs(this.getValue(), LongTag.class).m_7046_();
    }

    @Override
    public float readFloat(SerializationContext ctx) {
        return this.getAs(this.getValue(), FloatTag.class).m_7057_();
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return this.getAs(this.getValue(), DoubleTag.class).m_7061_();
    }

    // ---

    @Override
    public int readVarInt(SerializationContext ctx) {
        return this.getAs(this.getValue(), NumericTag.class).m_7047_();
    }

    @Override
    public long readVarLong(SerializationContext ctx) {
        return this.getAs(this.getValue(), NumericTag.class).m_7046_();
    }

    // ---

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        return this.getAs(this.getValue(), ByteTag.class).m_7063_() != 0;
    }

    @Override
    public String readString(SerializationContext ctx) {
        return this.getAs(this.getValue(), StringTag.class).m_7916_();
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        return this.getAs(this.getValue(), ByteArrayTag.class).m_128227_();
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        if (this.isReadingStructField()) {
            return Optional.of(endec.decode(ctx, this));
        } else {
            var struct = this.struct();
            return struct.field("present", ctx, Endec.BOOLEAN)
                    ? Optional.of(struct.field("value", ctx, endec))
                    : Optional.empty();
        }
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        //noinspection unchecked
        return new io.wispforest.accessories.endec.format.nbt.NbtDeserializer.Sequence<>(ctx, elementEndec, this.getAs(this.getValue(), CollectionTag.class));
    }

    @Override
    public <V> Deserializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return new io.wispforest.accessories.endec.format.nbt.NbtDeserializer.Map<>(ctx, valueEndec, this.getAs(this.getValue(), CompoundTag.class));
    }

    @Override
    public Deserializer.Struct struct() {
        return new io.wispforest.accessories.endec.format.nbt.NbtDeserializer.Struct(this.getAs(this.getValue(), CompoundTag.class));
    }

    // ---

    @Override
    public <S> void readAny(SerializationContext ctx, Serializer<S> visitor) {
        this.decodeValue(ctx, visitor, this.getValue());
    }

    private <S> void decodeValue(SerializationContext ctx, Serializer<S> visitor, Tag value) {
        switch (value.m_7060_()) {
            case Tag.f_178194_ -> visitor.writeByte(ctx, ((ByteTag) value).m_7063_());
            case Tag.f_178195_ -> visitor.writeShort(ctx, ((ShortTag) value).m_7053_());
            case Tag.f_178196_ -> visitor.writeInt(ctx, ((IntTag) value).m_7047_());
            case Tag.f_178197_ -> visitor.writeLong(ctx, ((LongTag) value).m_7046_());
            case Tag.f_178198_ -> visitor.writeFloat(ctx, ((FloatTag) value).m_7057_());
            case Tag.f_178199_ -> visitor.writeDouble(ctx, ((DoubleTag) value).m_7061_());
            case Tag.f_178201_ -> visitor.writeString(ctx, value.m_7916_());
            case Tag.f_178200_ -> visitor.writeBytes(ctx, ((ByteArrayTag) value).m_128227_());
            case Tag.f_178204_, Tag.f_178205_, Tag.f_178202_ -> {
                var list = (CollectionTag<?>) value;
                try (var sequence = visitor.sequence(ctx, Endec.<Tag>of(this::decodeValue, (ctx1, deserializer) -> null), list.size())) {
                    list.forEach(sequence::element);
                }
            }
            case Tag.f_178203_ -> {
                var compound = (CompoundTag) value;
                try (var map = visitor.map(ctx, Endec.<Tag>of(this::decodeValue, (ctx1, deserializer) -> null), compound.m_128440_())) {
                    for (var key : compound.m_128431_()) {
                        map.entry(key, compound.m_128423_(key));
                    }
                }
            }
            default ->
                    throw new IllegalArgumentException("Non-standard, unrecognized Tag implementation cannot be decoded");
        }
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final Iterator<Tag> elements;
        private final int size;

        private Sequence(SerializationContext ctx, Endec<V> valueEndec, List<Tag> elements) {
            this.ctx = ctx;
            this.valueEndec = valueEndec;

            this.elements = elements.iterator();
            this.size = elements.size();
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.elements.hasNext();
        }

        @Override
        public V next() {
            return NbtDeserializer.this.frame(
                    this.elements::next,
                    () -> this.valueEndec.decode(this.ctx, NbtDeserializer.this),
                    false
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final CompoundTag compound;
        private final Iterator<String> keys;
        private final int size;

        private Map(SerializationContext ctx, Endec<V> valueEndec, CompoundTag compound) {
            this.ctx = ctx;
            this.valueEndec = valueEndec;

            this.compound = compound;
            this.keys = compound.m_128431_().iterator();
            this.size = compound.m_128440_();
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.keys.hasNext();
        }

        @Override
        public java.util.Map.Entry<String, V> next() {
            var key = this.keys.next();
            return NbtDeserializer.this.frame(
                    () -> this.compound.m_128423_(key),
                    () -> java.util.Map.entry(key, this.valueEndec.decode(this.ctx, NbtDeserializer.this)),
                    false
            );
        }
    }

    public class Struct implements Deserializer.Struct {

        private final CompoundTag compound;

        public Struct(CompoundTag compound) {
            this.compound = compound;
        }

        @Override
        public <F> @Nullable F field(String name, SerializationContext ctx, Endec<F> endec) {
            if (!this.compound.m_128441_(name)) {
                throw new IllegalStateException("Field '" + name + "' was missing from serialized data, but no default value was provided");
            }

            return NbtDeserializer.this.frame(
                    () -> this.compound.m_128423_(name),
                    () -> endec.decode(ctx, NbtDeserializer.this),
                    true
            );
        }

        @Override
        public <F> @Nullable F field(String name, SerializationContext ctx, Endec<F> endec, @Nullable F defaultValue) {
            if (!this.compound.m_128441_(name)) return defaultValue;
            return NbtDeserializer.this.frame(
                    () -> this.compound.m_128423_(name),
                    () -> endec.decode(ctx, NbtDeserializer.this),
                    true
            );
        }
    }
}
