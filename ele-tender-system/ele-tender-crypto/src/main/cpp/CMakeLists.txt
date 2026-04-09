cmake_minimum_required(VERSION 3.20)

project(eletender_crypto_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(OpenSSL REQUIRED)
find_package(JNI REQUIRED)

add_library(crypto_core STATIC
    src/crypto_core.cpp
)
target_include_directories(crypto_core PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)
target_link_libraries(crypto_core PUBLIC OpenSSL::Crypto)

add_library(eletender_crypto_native SHARED
    src/jni_crypto_native.cpp
)
target_include_directories(eletender_crypto_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)
target_link_libraries(eletender_crypto_native PRIVATE
    crypto_core
    OpenSSL::Crypto
    JNI::JNI
)

add_executable(crypto_core_test
    ../../test/cpp/crypto_core_test.cpp
)
target_include_directories(crypto_core_test PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)
target_link_libraries(crypto_core_test PRIVATE
    crypto_core
    OpenSSL::Crypto
)

enable_testing()
add_test(NAME crypto_core_test COMMAND crypto_core_test)
