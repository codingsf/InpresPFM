#ifndef CPP_COMMONS_SELECTORTHREAD_HPP
#define CPP_COMMONS_SELECTORTHREAD_HPP

#include "net/Selector.hpp"
#include "net/Socket.hpp"
#include "protocol/ProtocolHandler.hpp"
#include "threading/ThreadPool.hpp"

template<class Translator, class Id>
class SelectorThread {

public:
    SelectorThread(Selector&, ThreadPool&, ProtocolHandler<Translator, Id>&);

    void select();
    void handle(std::shared_ptr<Socket>);

    void close();

private:
    Selector& selector;
    ThreadPool& pool;
    ProtocolHandler<Translator, Id>& proto;
    std::atomic_bool closed;
    std::thread thread;

};

template<class Translator, class Id>
SelectorThread<Translator, Id>::SelectorThread(Selector& selector, ThreadPool& pool, ProtocolHandler<Translator, Id>& proto)
        : selector(selector),
          pool(pool),
          proto(proto),
          closed(false),
          thread(&SelectorThread::select, this) {}

template<class Translator, class Id>
void SelectorThread<Translator, Id>::select() {
    LOG << "Starting polling thread";
    while(!this->closed) {
        for(std::shared_ptr<Socket> socket : this->selector.select()) {
            this->handle(socket);
        }
    }
}

template<class Translator, class Id>
void SelectorThread<Translator, Id>::handle(std::shared_ptr<Socket> socket) {
    this->pool.submit([this, socket] () mutable {
        LOG << Logger::Debug << "reading on socket " << socket->getHandle();
        try {
            this->proto.read(socket);
        } catch(IOError e) {
            if(e.reset) {
                LOG << Logger::Debug << "Connection reset: " << e.what();
            } else {
                LOG << Logger::Error << "Error reading from socket " << socket->getHandle() << ": " << e.what();
            }
            return;
        } catch(ProtocolError e) {
            LOG << Logger::Error << "Protocol error while reading from " << socket->getHandle() << ": " << e.what();
        }

        if(!socket->isClosed()) {
            LOG << Logger::Debug << "socket not closed, readding to selector";
            this->selector.addSocket(socket);
        }
    });
}

template<class Translator, class Id>
void SelectorThread<Translator, Id>::close() {
    if(this->closed.exchange(true)) {
        return;
    }
    this->selector.interrupt();
    this->thread.join();
}

#endif
