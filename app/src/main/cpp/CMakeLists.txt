cmake_minimum_required(VERSION 3.10.2)
project("neoterminal_native")

add_library(neoterminal_native SHARED terminal_backend.cpp)

find_library(log-lib log)
target_link_libraries(neoterminal_native ${log-lib})
