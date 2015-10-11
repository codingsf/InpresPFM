#ifndef CPP_COMMONS_PROTOCOLHANDLER_HPP
#define CPP_COMMONS_PROTOCOLHANDLER_HPP

#include <atomic>
#include <functional>
#include <map>
#include <queue>
#include <vector>

#include "net/Socket.hpp"
#include "protocol/ProtocolError.hpp"
#include "utils/Logger.hpp"

/**
 * Class abstracting the network implementation of a variable length protocol using limited framing.
 * <Translator> Class handling the transformation of a packet id and a vector of chars to useable packets
 * <Id>         Type of the packet ids (allows enum usage). The size of the id must be exactly 1 byte.
 */
template<class Translator, class Id>
class ProtocolHandler {

    using len_t = uint16_t;
    static const len_t MAX_LEN = UINT16_MAX;

public:
    ProtocolHandler(const Translator& translator) : translator(translator), closed(false) {
        static_assert(sizeof(Id) == 1, "Can only use ProtocolHandler with 1 byte ids");
    }

    void read(std::shared_ptr<Socket>);

    template<class P>
    P readSpecificPacket(std::shared_ptr<Socket>);

    template<class T>
    ProtocolHandler<Translator, Id>& write(std::shared_ptr<Socket>, const T&);

    void close() { closed = true; }

private:
    Translator translator;
    std::atomic_bool closed;

    std::pair<Id, std::vector<char>> readPacket(std::shared_ptr<Socket>);

    static const char FRAME_END;

};

template<class Translator, class Id>
const char ProtocolHandler<Translator, Id>::FRAME_END = 0x42;

template<class Translator, class Id>
std::pair<Id, std::vector<char>> ProtocolHandler<Translator, Id>::readPacket(std::shared_ptr<Socket> socket) {
    Id id;
    len_t len;
    std::vector<char> v;

    socket->accumulate(sizeof(len_t) + 1, v);
    id = (Id) v[0];
    // TODO Validate the id and close the connection on protocol error!
    len = *reinterpret_cast<const len_t*>(&v[1]) + 1; // + 1 => end frame marquer

    LOG << Logger::Debug << "Packet received: id:" << id << ":len:" << len << ":read:" << v.size();

    if(len < 1) {
        throw ProtocolError("Invalid length: " + std::to_string(len));
    }

    v.clear();
    v.reserve(len);
    socket->accumulate(len, v);

    if (v.back() != FRAME_END) {
        throw ProtocolError("Invalid frame end");
    }
    v.pop_back();
    return { id, v };
}

template<class Translator, class Id>
void ProtocolHandler<Translator, Id>::read(std::shared_ptr<Socket> socket) {
    // TODO Check if there is a thing for unpacking?
    const std::pair<Id, std::vector<char>>& tmp_packet = this->readPacket(socket);

    this->translator.decode(tmp_packet.first, tmp_packet.second, socket);
}

template<class Translator, class Id>
template<class P>
P ProtocolHandler<Translator, Id>::readSpecificPacket(std::shared_ptr<Socket> socket) {
    while(true) {
        // TODO Check if there is a thing for unpacking?
        const std::pair<Id, std::vector<char>>& tmp_packet = this->readPacket(socket);

        if(tmp_packet.first != P::id) {
            LOG << Logger::Warning << "Ignored invalid packet with id " << tmp_packet.first;
            continue;
        }

        auto it = tmp_packet.second.cbegin();
        P packet = P::decode(it);
        // Call the handlers, just in case
        packet.handle(socket);
        return packet;
    }
}

template<class Translator, class Id>
template<class T>
ProtocolHandler<Translator, Id>& ProtocolHandler<Translator, Id>::write(std::shared_ptr<Socket> socket, const T& item) {
    std::vector<char> v(sizeof(len_t) + 1, 0); // Reserve places for the id and the packet length

    v[0] = T::id;
    this->translator.encode(item, v);

    // Store packet length
    uint64_t long_len = v.size() - (sizeof(len_t) + 1);
    if(long_len > MAX_LEN) {
        throw std::runtime_error("Packet length too large for the protocol");
    }

    len_t len = static_cast<len_t>(long_len);
    const char* bytes = reinterpret_cast<const char*>(&len);
    for(size_t i = 0; i < sizeof(len_t); ++i) {
        v[i + 1] = bytes[i];
    }

    v.push_back(FRAME_END);

    LOG << Logger::Debug << "id:" << (int) v[0] << ":len:" << len << ":written:" << v.size();

    socket->write(v);

    return *this;
}

#endif
