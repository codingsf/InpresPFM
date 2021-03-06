package be.hepl.benbear.commons.serialization;

import be.hepl.benbear.commons.checking.Sanity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// TODO Implement Collection<?> serializer and deserializer
public class BinarySerializer {

    /**
     * This is a singleton to allow implementors to register their
     * (de)serializers at initialization time (i.e. in a static block).
     *
     * A good implementation would probably use DI, but I'm not implementing
     * that by myself for a school project (at least not until everything else
     * is working)...
     */
    private static final BinarySerializer INSTANCE = new BinarySerializer();
    public static BinarySerializer getInstance() {
        return INSTANCE;
    }

    private final Map<Class<?>, Deserializer<?>> deserializers;
    private final Map<Class<?>, Serializer<?>> serializers;

    private BinarySerializer() {
        deserializers = new HashMap<>();
        serializers = new HashMap<>();

        registerSerializer(boolean.class, (o, dos) -> dos.write(o ? 1 : 0), bb -> bb.get() != 0);
        registerSerializer(byte.class, (o, dos) -> dos.write(o), ByteBuffer::get);
        registerSerializer(short.class, (o, dos) -> dos.writeShort(o), ByteBuffer::getShort);
        registerSerializer(int.class, (o, dos) -> dos.writeInt(o), ByteBuffer::getInt);
        registerSerializer(long.class, (o, dos) -> dos.writeLong(o), ByteBuffer::getLong);
        // TODO Check that works against the cpp implementation
        registerSerializer(float.class, BinarySerializer::serializeFloat, BinarySerializer::deserializeFloat);
        // Double not implemented
        registerSerializer(String.class, (String o, DataOutputStream dos) -> {
            if(o == null) {
                dos.writeInt(0);
                return;
            }
            byte[] bytes = o.getBytes();
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }, bb -> {
            int len = bb.getInt();
            if(len <= 0) {
                // That's what you get for using unsigned...
                return "";
            }
            byte[] bytes = new byte[len];
            bb.get(bytes);
            return new String(bytes);
        });
        registerSerializer(UUID.class, (UUID o, DataOutputStream dos) -> {
            if(o == null) {
                dos.writeByte(0);
                return;
            }
            dos.writeByte(1);
            dos.writeLong(o.getMostSignificantBits());
            dos.writeLong(o.getLeastSignificantBits());
        }, bb -> {
            if(bb.get() == 0) {
                return null;
            }
            return new UUID(bb.getLong(), bb.getLong());
        });

        // Register (de)serializers for array types
        Map<Class<?>, Serializer<?>> arraySerializers = new HashMap<>(serializers.size());
        Map<Class<?>, Deserializer<?>> arrayDeserializers = new HashMap<>(serializers.size());
        serializers.keySet().forEach(clazz -> {
            ArraySerializer<?> arraySerializer = new ArraySerializer<>(clazz);

            arraySerializers.put(arraySerializer.getArrayClass(), arraySerializer);
            arrayDeserializers.put(arraySerializer.getArrayClass(), arraySerializer);
        });
        serializers.putAll(arraySerializers);
        deserializers.putAll(arrayDeserializers);
    }

    private static void serializeFloat(float f, DataOutputStream dos) throws IOException {
        // TODO Check that works against the cpp implementation
        // Public domain implementation from
        // http://beej.us/guide/bgnet/output/html/singlepage/bgnet.html#serialization

        if (f == 0.0) {
            // get this special case out of the way
            dos.writeLong(0);
            return;
        }

        int bits = 32, expbits = 8;
        int significandbits = bits - expbits - 1; // -1 for sign bit
        double fnorm;
        int shift;
        long sign, exp, significand;

        // check sign and begin normalization
        if (f < 0) {
            sign = 1; fnorm = -f;
        } else {
            sign = 0; fnorm = f;
        }

        // get the normalized form of f and track the exponent
        shift = 0;
        while(fnorm >= 2.0) {
            fnorm /= 2.0; shift++;
        }
        while(fnorm < 1.0) {
            fnorm *= 2.0; shift--;
        }
        fnorm = fnorm - 1.0;

        // calculate the binary form (non-float) of the significand data
        significand = (long) (fnorm * ((1L << significandbits) + 0.5f));

        // get the biased exponent
        exp = shift + ((1<<(expbits-1)) - 1); // shift + bias

        // return the final answer
        dos.writeLong((sign << (bits-1)) | (exp << (bits - expbits - 1)) | significand);
    }

    private static float deserializeFloat(ByteBuffer bb) {
        // Public domain implementation from
        // http://beej.us/guide/bgnet/output/html/singlepage/bgnet.html#serialization

        long i = bb.getLong();
        int bits = 32, expbits = 8;
        float result;
        long shift;
        int bias;
        int significandbits = bits - expbits - 1; // -1 for sign bit

        if (i == 0) return 0.0f;

        // pull the significand
        result = i & (1L << significandbits) - 1; // mask
        result /= 1L << significandbits; // convert back to float
        result += 1.0f; // add the one back on

        // deal with the exponent
        bias = (1 << (expbits - 1)) - 1;
        shift = (i >> significandbits & (1L << expbits) - 1) - bias;
        while(shift > 0) {
            result *= 2.0; shift--;
        }
        while(shift < 0) {
            result /= 2.0; shift++;
        }

        // sign it
        result *= (i >> (bits - 1) & 1) != 0 ? -1.0 : 1.0;

        return result;
    }

    public synchronized <T> BinarySerializer registerSerializer(Class<T> clazz, Serializer<T> serializer, Deserializer<T> deserializer) {
        Sanity.noneNull(clazz, serializer, deserializer);

        deserializers.put(clazz, deserializer);
        serializers.put(clazz, serializer);
        return this;
    }

    public synchronized <T> Serializer<T> getSerializer(Class<T> clazz) {
        Serializer<T> serializer = (Serializer<T>) serializers.get(clazz);
        if(serializer == null) {
            throw new NoSerializerException("No serializer for class " + clazz.getName());
        }
        return serializer;
    }

    public synchronized <T> Deserializer<T> getDeserializer(Class<T> clazz) {
        Deserializer<T> deserializer = (Deserializer<T>) deserializers.get(clazz);
        if(deserializer == null) {
            throw new NoDeserializerException("No serializer for class " + clazz.getName());
        }
        return deserializer;
    }

    public <T> byte[] serialize(T object) throws IOException {
        Sanity.notNull(object, "object");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        getSerializer((Class<T>) object.getClass()).serialize(object, dos);

        return bos.toByteArray();
    }

    public <T> T deserialize(Class<T> clazz, ByteBuffer bb) {
        Sanity.noneNull(clazz, bb);

        return getDeserializer(clazz).deserialize(bb);
    }

}
