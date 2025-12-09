cmake_minimum_required(VERSION 3.22.1)

project("packethunter")

# C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add source files
add_library(packethunter SHARED
    packet_parser.cpp
    native_interface.cpp
    packet_forwarder.cpp
    tun2socks_bridge.cpp
    lwip_tun2socks.c
)

# Find libraries
find_library(log-lib log)
find_library(android-lib android)

# Link libraries
target_link_libraries(packethunter
    ${log-lib}
    ${android-lib}
)

# Include directories
target_include_directories(packethunter PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
