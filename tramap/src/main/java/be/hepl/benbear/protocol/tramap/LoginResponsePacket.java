package be.hepl.benbear.protocol.tramap;

import be.hepl.benbear.commons.protocol.Packet;

public class LoginResponsePacket implements Packet {

    public static final byte ID = 8;

    private final boolean ok;
    private final String reason;

    public LoginResponsePacket(boolean ok, String reason) {
        this.ok = ok;
        this.reason = reason;
    }

    public boolean isOk() {
        return ok;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public byte getId() {
        return ID;
    }
}
