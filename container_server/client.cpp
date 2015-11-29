#include "client/ContainerClient.hpp"
#include "utils/ProgramProperties.hpp"

int main(int argc, char** argv) {
    Logger::instance
        .clearHandlers()
        .addHandler(Logger::consoleHandler(Logger::Warning | Logger::Error))
        .addHandler(Logger::fileHandler("client-debug.log"))
        .addHandler(Logger::fileHandler("client.log", Logger::Warning | Logger::Error));

    ProgramProperties props(argc, argv);

    if(props.has("h") || props.has("help")) {
        std::cout << "Usage: " << argv[0]
            << " --config=<config-file> --port=<port> --host=<host>" << std::endl;
        return 0;
    }

    unsigned short port = props.getAsUnsignedShort("containerserver.port", 31060);
    std::string host = props.get("containerserver.host", "localhost");

    std::shared_ptr<Socket> s(new Socket);
    s->connect(port, host);

    CMMPTranslator translator;
    ProtocolHandler<CMMPTranslator, PacketId> proto(translator);

    ContainerClient client(s, proto);

    client.init().mainLoop();

    LOG << "done";

    return 0;
}
