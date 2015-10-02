#ifndef CONTAINER_SERVER_INPUTTRUCKPACKET_HPP
#define CONTAINER_SERVER_INPUTTRUCKPACKET_HPP

#include <string>

#include "cmmp/PacketId.hpp"
#include "protocol/Packet.hpp"

class InputTruckPacket : public Packet<InputTruckPacket> {

public:
    InputTruckPacket(const std::string& license, const std::vector<std::pair<std::string, std::string>>& containers)
        : Packet(PacketId::InputTruck),
          license(license),
          containers(containers) {}

    const std::string& getLicense() const { return license; }
    const std::vector<std::pair<std::string, std::string>>& getContainers() const { return containers; }

    static InputTruckPacket decode(std::vector<char>::const_iterator&);
    void encode(std::vector<char>&) const;

private:
    std::string license;
    std::vector<std::pair<std::string, std::string>> containers;

};

#endif
