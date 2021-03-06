#ifndef CONTAINER_SERVER_ADMIN_HPP
#define CONTAINER_SERVER_ADMIN_HPP

#include "csa/PacketId.hpp"
#include "csa/Translator.hpp"
#include "protocol/ProtocolHandler.hpp"
#include "server/ContainerServer.hpp"

using namespace csa;

class Admin {

public:
    Admin(const std::string&, const std::string&, ContainerServer&, unsigned short);
    ~Admin();

    void run();
    void close();

private:
    std::string username;
    std::string password;
    ContainerServer& server;
    ProtocolHandler<Translator, csa::PacketId> proto;
    Socket socket;
    std::shared_ptr<Socket> currentAdmin;
    std::thread thread;
    std::atomic_bool closed;

};

#endif
