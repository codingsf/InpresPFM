#include "server/ContainerServer.hpp"
#include "utils/ProgramProperties.hpp"

int main(int argc, char** argv) {
    // Logging setup
    std::cout << std::showbase << std::boolalpha;
    std::cerr << std::showbase << std::boolalpha;

    // TODO Setup a file logger?
    // TODO an argument parser?

    ProgramProperties props(argc, argv);

    if(props.has("h") || props.has("help")) {
        std::cout << "Usage: " << argv[0] << " --port=<port> --parc-file=<parc_file> --user-file<user_file>" << std::endl;
        return 0;
    }

    unsigned short port = props.getAsUnsignedShort("port", 31060); // 31060 -> 31069
    std::string parc_file = props.get("parc-file", "data/parc.dat");
    std::string user_file = props.get("user-file", "data/users.csv");

    LOG << "Creating thread pool";
    ThreadPool pool(4, [] {
        LOG << Logger::Debug << "Starting thread from pool";
    });

    LOG << "Creating server";
    ContainerServer server(port, parc_file, user_file, pool);

    LOG << "Starting up";
    server.init().listen();

    LOG << "Application end";

    return 0;
}
