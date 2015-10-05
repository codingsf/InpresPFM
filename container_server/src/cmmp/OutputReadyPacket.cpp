#include "cmmp/OutputReadyPacket.hpp"

const PacketId OutputReadyPacket::id = PacketId::OutputReady;

OutputReadyPacket OutputReadyPacket::decode(std::vector<char>::const_iterator& it) {
    std::string license = readString(it);
    std::string destination = readString(it);
    uint32_t capacity = readPrimitive<uint32_t>(it);

    return OutputReadyPacket(license, destination, capacity);
}

void OutputReadyPacket::encode(std::vector<char>& v) const {
    writeString(v, license);
    writeString(v, destination);
    writePrimitive(v, capacity);
}
