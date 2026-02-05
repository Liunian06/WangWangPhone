cmake_minimum_required(VERSION 3.10)
project(WangWangJiCore)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(include)

file(GLOB_RECURSE SOURCES "src/*.cpp")

# 查找 SQLite3
find_package(SQLite3 REQUIRED)

add_library(wwj_core SHARED ${SOURCES})

# 链接 SQLite3
target_link_libraries(wwj_core PRIVATE SQLite::SQLite3)

# Platform specific definitions would go here
if(ANDROID)
    # Android specific setup
    # SQLite 已内置于 Android 系统中
    target_compile_definitions(wwj_core PRIVATE PLATFORM_ANDROID)
elseif(IOS)
    # iOS specific setup
    # SQLite 已内置于 iOS 系统中
    target_compile_definitions(wwj_core PRIVATE PLATFORM_IOS)
elseif(WIN32)
    # Windows specific setup
    target_compile_definitions(wwj_core PRIVATE PLATFORM_WINDOWS)
elseif(EMSCRIPTEN)
    # Web/WASM specific setup
    target_compile_definitions(wwj_core PRIVATE PLATFORM_WEB)
endif()