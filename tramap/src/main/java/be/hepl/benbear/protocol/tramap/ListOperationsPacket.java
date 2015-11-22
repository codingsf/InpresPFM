package be.hepl.benbear.protocol.tramap;

import be.hepl.benbear.commons.protocol.Packet;

import java.time.Instant;

public class ListOperationsPacket implements Packet {

    public static final byte ID = 5;

    public enum Type {
        Society, Destination;
    }
    private final Instant start;

    private final Instant end;
    private final String society;
    private final String destination;
    public ListOperationsPacket(Instant start, Instant end, String str, Type type) {
        this.start = start;
        this.end = end;

        if(type == Type.Society) {
            this.society = str;
            this.destination = "";
        } else {
            this.society = "";
            this.destination = str;
        }
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }

    public String getSociety() {
        return society;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public byte getId() {
        return ID;
    }

}
