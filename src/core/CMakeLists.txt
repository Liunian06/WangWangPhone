cmake_minimum_required(VERSION 3.10)
project(WangWangJiCore)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(include)

file(GLOB_RECURSE SOURCES "src/*.cpp")

add_library(wwj_core SHARED ${SOURCES})

# Platform specific definitions would go here
if(ANDROID)
    # Android specific setup
elseif(IOS)
    # iOS specific setup
endif()