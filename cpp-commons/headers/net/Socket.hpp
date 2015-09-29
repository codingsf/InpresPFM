#ifndef CPP_COMMONS_SOCKET_HPP
#define CPP_COMMONS_SOCKET_HPP

#include <array>
#include <atomic>
#include <cstring>
#include <string>
#include <vector>

#include <netdb.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>

#include "net/IOError.hpp"
#include "utils/Logger.hpp"

/**
 * A socket class wraps C low level socket code into a thread safe class.
 * An instance of this class cannot be copied or moved, accept returns a
 * shared_ptr for that reason.
 */
class Socket {

public:
    Socket() : server(false), handle(-1) {}
    Socket(const Socket&) = delete;
    Socket(Socket&&) = delete;
    ~Socket();

    /**
     * Connects this socket to a specific hostname and port.
     * @param port
     * @param hostname
     * @return the instance for chaining
     */
    Socket& connect(unsigned short, std::string);

    /**
     * Binds the current socket to a port and all interfaces
     * @param port
     * @return the instance for chaining
     */
    Socket& bind(unsigned short);

    /**
     * Binds the current socket to a port and a hostname.
     * @param port
     * @param hostname
     * @return the instance for chaining
     */
    Socket& bind(unsigned short, std::string);

    /**
     * Accepts a new connection.
     * @return a new socket bound to the new connection
     */
    std::shared_ptr<Socket> accept();

    /**
     * Writes the byte vector onto this socket.
     * @param vector a byte vector
     * @return the number of bytes actually written
     */
    long write(const std::vector<char>&);

    /**
     * Reads a certain amount of bytes from this socket.
     * @param count the amount of bytes to try to read
     * @return the bytes actually read
     */
    std::vector<char> read(unsigned int);

    /**
     * Reads from this socket into a provided vector until its filled
     * with the needed amount of bytes.
     * @param count the amount of bytes wanted
     * @param the vector to fill
     */
    void accumulate(unsigned int, std::vector<char>&);

    /**
     * Gets the low level handle associated with this instance.
     */
    int getHandle() const { return this->handle; }

    /**
     * Closes the connection handled by this instance.
     */
    void close();

    /**
     * Returns this socket got closed.
     */
    bool isClosed() const { return this->handle < 0; }

    /**
     * Returns whether this socket is a server socket (bind) or a client
     * socket (connect or returned by accept).
     */
    bool isServer() const { return this->server; }

private:
    bool server;
    int handle;
    std::recursive_mutex handleMutex;
    struct sockaddr addr;
    unsigned int addrLen;

    struct sockaddr_in setupPort(unsigned short);
    struct sockaddr_in setupHostAndPort(unsigned short, std::string);
    Socket& setupSocket(const struct sockaddr_in, bool bind);
    void error(const std::string&, int);
    void checkOpen() const;

};

#endif
