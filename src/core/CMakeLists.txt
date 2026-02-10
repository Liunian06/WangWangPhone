cmake_minimum_required(VERSION 3.10)
project(WangWangJiCore)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(include)

file(GLOB_RECURSE SOURCES "src/*.cpp")

# 查找 SQLite3
find_package(SQLite3 REQUIRED)

# 查找 OpenSSL (用于 RSA 签名验证)
find_package(OpenSSL REQUIRED)

add_library(wwj_core SHARED ${SOURCES})

# 链接 SQLite3 和 OpenSSL
target_link_libraries(wwj_core PRIVATE SQLite::SQLite3 OpenSSL::Crypto)

# Platform specific definitions would go here
if(ANDROID)
    # Android specific setup
    # SQLite 已内置于 Android 系统中
    # OpenSSL 需要通过 Android NDK 或预编译库提供
    target_compile_definitions(wwj_core PRIVATE PLATFORM_ANDROID)
elseif(IOS)
    # iOS specific setup
    # SQLite 已内置于 iOS 系统中
    # OpenSSL 需要通过 CocoaPods 或预编译库提供
    target_compile_definitions(wwj_core PRIVATE PLATFORM_IOS)
elseif(WIN32)
    # Windows specific setup
    target_compile_definitions(wwj_core PRIVATE PLATFORM_WINDOWS)
elseif(EMSCRIPTEN)
    # Web/WASM specific setup
    target_compile_definitions(wwj_core PRIVATE PLATFORM_WEB)
endif()