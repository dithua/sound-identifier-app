/*
    sha1.cpp - source code of
    ============
    SHA-1 in C++
    ============
    100% Public Domain.
    Original C Code
        -- Steve Reid <steve@edmweb.com>
    Small changes to fit into bglibs
        -- Bruce Guenter <bruce@untroubled.org>
    Translation to simpler C++ Code
        -- Volker Diels-Grabsch <v@njh.eu>
    Safety fixes
        -- Eugene Hopkinson <slowriot at voxelstorm dot com>
*/
// Original GitHub repository https://github.com/vog/sha1/
// Modified by George Vasios
#pragma once

#include <sstream>
#include <iomanip>
#include <fstream>

class SHA1 {
public:
    SHA1();
    void update(const std::string &s);
    void update(std::istream &is);
    std::string final();
private:
    uint32_t digest[5];
    std::string buffer;
    uint64_t transforms;
};
