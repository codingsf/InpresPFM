#ifndef CONTAINER_SERVER_OUTPUTONERESPONSERESPONSEPACKET_HPP
#define CONTAINER_SERVER_OUTPUTONERESPONSERESPONSEPACKET_HPP

#include <string>

#include "cmmp/PacketId.hpp"
#include "protocol/Packet.hpp"

class OutputOneResponsePacket : public Packet<OutputOneResponsePacket> {

public:
    OutputOneResponsePacket(bool ok, std::string reason)
        : Packet(PacketId::OutputOneResponse),
          ok(ok),
          reason(reason) {}

    bool isOk() const { return ok; }
    const std::string& getReason() const { return reason; }

    static OutputOneResponsePacket decode(const std::vector<char>&);
    void encode(std::vector<char>&) const;

private:
    bool ok;
    std::string reason;

};

#endif //CONTAINER_SERVER_OUTPUTONERESPONSERESPONSEPACKET_HPP