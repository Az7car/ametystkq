package io.papermc.paper.util.nbt;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.nbt.*;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public final class CompactNbtSerializer {

    private CompactNbtSerializer() {}

    public static byte[] serialize(final CompoundTag tag) {
        final ByteArrayList bytes = new ByteArrayList(4096);
        try {
            writeTag(tag, bytes);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return bytes.toByteArray();
    }

    public static CompoundTag deserialize(final byte[] data) {
        return (CompoundTag) readTag(new NbtInput(data), 0);
    }

    private static void writeTag(final Tag tag, final ByteArrayList out) throws IOException {
        if (tag == null) {
            out.add((byte) 0);
        } else if (tag instanceof CompoundTag compound) {
            out.add((byte) 10);
            out.add((byte) 0);
            for (final String key : compound.keySet()) {
                final byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                out.add((byte) keyBytes.length);
                for (final byte b : keyBytes) out.add(b);
                writeTag(compound.get(key), out);
            }
            out.add((byte) 0);
        } else if (tag instanceof StringTag st) {
            out.add((byte) 8);
            final byte[] strBytes = st.value().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            final int len = strBytes.length;
            out.add((byte) (len >> 8));
            out.add((byte) len);
            for (final byte b : strBytes) out.add(b);
        } else if (tag instanceof ByteTag bt) {
            out.add((byte) 1);
            out.add(bt.value());
        } else if (tag instanceof ShortTag st) {
            out.add((byte) 2);
            final short v = st.value();
            out.add((byte) (v >> 8));
            out.add((byte) v);
        } else if (tag instanceof IntTag it) {
            out.add((byte) 3);
            final int v = it.value();
            out.add((byte) (v >> 24));
            out.add((byte) (v >> 16));
            out.add((byte) (v >> 8));
            out.add((byte) v);
        } else if (tag instanceof LongTag lt) {
            out.add((byte) 4);
            final long v = lt.value();
            out.add((byte) (v >> 56));
            out.add((byte) (v >> 48));
            out.add((byte) (v >> 40));
            out.add((byte) (v >> 32));
            out.add((byte) (v >> 24));
            out.add((byte) (v >> 16));
            out.add((byte) (v >> 8));
            out.add((byte) v);
        } else if (tag instanceof FloatTag ft) {
            out.add((byte) 5);
            final int bits = Float.floatToRawIntBits(ft.value());
            out.add((byte) (bits >> 24));
            out.add((byte) (bits >> 16));
            out.add((byte) (bits >> 8));
            out.add((byte) bits);
        } else if (tag instanceof DoubleTag dt) {
            out.add((byte) 6);
            final long bits = Double.doubleToRawLongBits(dt.value());
            out.add((byte) (bits >> 56));
            out.add((byte) (bits >> 48));
            out.add((byte) (bits >> 40));
            out.add((byte) (bits >> 32));
            out.add((byte) (bits >> 24));
            out.add((byte) (bits >> 16));
            out.add((byte) (bits >> 8));
            out.add((byte) bits);
        } else if (tag instanceof ByteArrayTag bat) {
            out.add((byte) 7);
            final byte[] arr = bat.getAsByteArray();
            final int len = arr.length;
            out.add((byte) (len >> 24));
            out.add((byte) (len >> 16));
            out.add((byte) (len >> 8));
            out.add((byte) len);
            for (final byte b : arr) out.add(b);
        } else if (tag instanceof IntArrayTag iat) {
            out.add((byte) 11);
            final int[] arr = iat.getAsIntArray();
            final int len = arr.length;
            out.add((byte) (len >> 24));
            out.add((byte) (len >> 16));
            out.add((byte) (len >> 8));
            out.add((byte) len);
            for (final int v : arr) {
                out.add((byte) (v >> 24));
                out.add((byte) (v >> 16));
                out.add((byte) (v >> 8));
                out.add((byte) v);
            }
        } else if (tag instanceof LongArrayTag lat) {
            out.add((byte) 12);
            final long[] arr = lat.getAsLongArray();
            final int len = arr.length;
            out.add((byte) (len >> 24));
            out.add((byte) (len >> 16));
            out.add((byte) (len >> 8));
            out.add((byte) len);
            for (final long v : arr) {
                out.add((byte) (v >> 56));
                out.add((byte) (v >> 48));
                out.add((byte) (v >> 40));
                out.add((byte) (v >> 32));
                out.add((byte) (v >> 24));
                out.add((byte) (v >> 16));
                out.add((byte) (v >> 8));
                out.add((byte) v);
            }
        } else if (tag instanceof ListTag list) {
            out.add((byte) 9);
            final int size = list.size();
            out.add((byte) (size >> 24));
            out.add((byte) (size >> 16));
            out.add((byte) (size >> 8));
            out.add((byte) size);
            for (int i = 0; i < size; i++) {
                writeTag(list.get(i), out);
            }
        }
    }

    private static Tag readTag(final NbtInput data, final int offset) {
        return null;
    }

    private static final class NbtInput {
        final byte[] data;
        int pos;

        NbtInput(final byte[] data) {
            this.data = data;
        }

        int readByte() { return this.data[this.pos++] & 0xFF; }
        int readShort() { return (this.readByte() << 8) | this.readByte(); }
        int readInt() { return (this.readByte() << 24) | (this.readByte() << 16) | (this.readByte() << 8) | this.readByte(); }
        long readLong() { return ((long) this.readByte() << 56) | ((long) this.readByte() << 48) | ((long) this.readByte() << 40) | ((long) this.readByte() << 32) | ((long) this.readByte() << 24) | ((long) this.readByte() << 16) | ((long) this.readByte() << 8) | (long) this.readByte(); }
        float readFloat() { return Float.intBitsToFloat(this.readInt()); }
        double readDouble() { return Double.longBitsToDouble(this.readLong()); }
        String readString() {
            final int len = this.readShort();
            final String s = new String(this.data, this.pos, len, java.nio.charset.StandardCharsets.UTF_8);
            this.pos += len;
            return s;
        }
    }
}
