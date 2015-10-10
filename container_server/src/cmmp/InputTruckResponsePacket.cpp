#include "cmmp/InputTruckResponsePacket.hpp"

const PacketId InputTruckResponsePacket::id = PacketId::InputTruckResponse;

InputTruckResponsePacket InputTruckResponsePacket::decode(std::vector<char>::const_iterator& it) {
    bool ok = readPrimitive<bool>(it);
    std::string reason;
    std::vector<Container> containers;

    if (ok) {
        uint32_t size = readPrimitive<uint32_t>(it);
        if (size) {
            std::string container_id;
            std::string destination;
            uint16_t x;
            uint16_t y;

            for(uint32_t i = 0; i < size; i++) {
                container_id = readString(it);
                destination = readString(it);
                x = readPrimitive<uint16_t>(it);
                y = readPrimitive<uint16_t>(it);

                containers.emplace_back(Container { container_id, destination, x, y });
            }
        }
    } else {
        reason = readString(it);
    }

    return InputTruckResponsePacket(ok, containers, reason);
}

void InputTruckResponsePacket::encode(std::vector<char>& v) const {
    writePrimitive(v, ok);
    if(!ok) {
        writeString(v, reason);
    } else {
        writePrimitive<uint32_t>(v, containers.size());
        for(const Container& container : containers) {
            writeString(v, container.id);
            writeString(v, container.destination);
            writePrimitive(v, container.x);
            writePrimitive(v, container.y);
        }
    }
}
