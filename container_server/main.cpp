#include <iostream>

#include "net/Socket.hpp"
#include "protocol/ProtocolHandler.hpp"
#include "protocol/Translator.hpp"

int main(int argc, char** argv) {

    std::vector<char> v;
    char i = 42;
    Translator::writePrimitive(v, i);
    std::cout << v.size() << std::endl;
    for(char c : v) {
        std::cout << (int) c << " ";
    }
    std::cout << std::endl;

    std::vector<char>::const_iterator it = v.begin();
    std::cout << Translator::readPrimitive<char>(it) << std::endl;

    /*
    std::cerr << "creating socket" << std::endl;
    Socket s(8080);

    std::cerr << "listen + accept" << std::endl;
    Socket connection = s.listen(3).accept();
    std::vector<char> arr = {'h', 'e', 'y'};
    connection.write(arr);

    arr = std::move(connection.read(50));
    arr.push_back(0);
    std::cout << arr.data() << std::endl;

    */

    return 0;
}