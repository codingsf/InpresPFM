package be.hepl.benbear.commons.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

// TODO Events for each packet and connection closed
public class ProtocolHandler {

    private static final byte FRAME_END = 0x42;

    private final Map<Byte, Class<?>> packetsById;
    private final Map<Class<?>, Byte> idsByClass;
    private final BinarySerializer serializer;

    public ProtocolHandler() {
        packetsById = new HashMap<>();
        idsByClass = new HashMap<>();
        serializer = BinarySerializer.getInstance();
    }

    public synchronized <T> ProtocolHandler registerPacket(byte id, Class<T> packetClass) {
        if(packetsById.containsKey(id) || idsByClass.containsKey(packetClass)) {
            throw new IllegalStateException("A packet with id = " + id + " or class = " + packetClass.getName() + " is already registered");
        }

        packetsById.put(id, packetClass);
        idsByClass.put(packetClass, id);
        ObjectSerializer<T> objectSerializer = new ObjectSerializer<>(serializer, packetClass);
        serializer.registerSerializer(packetClass, objectSerializer, objectSerializer);
        return this;
    }

    public <T> T read(InputStream is) throws IOException {
        byte[] b = accumulate(is, 3);
        Class<T> packetClass = (Class<T>) packetsById.get(b[0]);
        if(packetClass == null) {
            throw new ProtocolException("No mapping for packet id " + b[0]);
        }

        int len = b[1] << 8 | b[2];
        byte[] bytes = accumulate(is, len + 1);

        if(bytes[bytes.length - 1] != FRAME_END) {
            throw new ProtocolException("Invalid frame end");
        }

        ByteBuffer bb = ByteBuffer.wrap(bytes, 0, bytes.length - 1);
        return serializer.deserialize(packetClass, bb);
    }

    public <T> T readSpecific(InputStream is, Class<T> packetClass) throws IOException {
        T read;

        while((read = read(is)).getClass() != packetClass);

        return read;
    }

    public <T> ProtocolHandler write(OutputStream os, T packet) throws IOException {
        byte[] bytes = serializer.serialize(packet);
        os.write(idsByClass.get(packet.getClass()));
        os.write(bytes.length & 0xff);
        os.write(bytes.length >> 8 & 0xff);
        os.write(bytes);
        os.write(FRAME_END);

        return this;
    }

    private byte[] accumulate(InputStream is, int len) throws IOException {
        byte[] b = new byte[len];
        int read = 0;
        while(read < len) {
            read += is.read(b, read, len - read);
        }
        return b;
    }

}
