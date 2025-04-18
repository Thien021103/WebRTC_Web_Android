cmake_minimum_required(VERSION 3.1)

# -------------------------------------------------------------------------------------

# Config Toolchain
# set(CMAKE_CXX_STANDARD 11)
# set(CMAKE_C_STANDARD 99)
# set(CMAKE_C_COMPILER    "/opt/mips-gcc472-glibc216-64bit/bin/mips-linux-uclibc-gnu-gcc")
# set(CMAKE_CXX_COMPILER  "/opt/mips-gcc472-glibc216-64bit/bin/mips-linux-uclibc-gnu-g++")

# set(CMAKE_TOOLCHAIN_FILE ${CMAKE_CURRENT_SOURCE_DIR}/toolchain.cmake)

# -------------------------------------------------------------------------------------

project(peer)

option(ENABLE_TESTS "Enable tests" OFF)
option(BUILD_SHARED_LIBS "Build shared libraries" OFF)
option(ADDRESS_SANITIZER "Build with AddressSanitizer." OFF)
option(MEMORY_SANITIZER "Build with MemorySanitizer." OFF)
option(THREAD_SANITIZER "Build with ThreadSanitizer." OFF)
option(UNDEFINED_BEHAVIOR_SANITIZER "Build with UndefinedBehaviorSanitizer." OFF)

include(ExternalProject)

include_directories(${CMAKE_BINARY_DIR}/dist/include)

link_directories(${CMAKE_BINARY_DIR}/dist/lib)

set(DEP_LIBS "srtp2" "mbedtls" "mbedcrypto" "mbedx509" "websockets")
# Extended debug information (symbols, source code, and macro definitions)
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -g3")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g3")

function(enableSanitizer SANITIZER)
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O0 -g -fsanitize=${SANITIZER} -fno-omit-frame-pointer -fno-optimize-sibling-calls" PARENT_SCOPE)
  set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -fsanitize=${SANITIZER}" PARENT_SCOPE)
endfunction()

if(ADDRESS_SANITIZER)
  enableSanitizer("address")
endif()

if(MEMORY_SANITIZER)
  enableSanitizer("memory")
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fsanitize-memory-track-origins")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize-memory-track-origins")
endif()

if(THREAD_SANITIZER)
  enableSanitizer("thread")
endif()

if(UNDEFINED_BEHAVIOR_SANITIZER)
  enableSanitizer("undefined")
endif()

add_definitions("-Wunused-variable -Werror=sequence-point -Werror=pointer-sign -Werror=return-type -DHTTP_DO_NOT_USE_CUSTOM_CONFIG -DMQTT_DO_NOT_USE_CUSTOM_CONFIG")

add_subdirectory(src)
add_subdirectory(examples)

if(ENABLE_TESTS)
  add_subdirectory(tests)
endif()

ExternalProject_Add(mbedtls
  SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/mbedtls
  CMAKE_ARGS
    -DCMAKE_C_FLAGS="-fPIC"
    -DENABLE_TESTING=off
    -DENABLE_PROGRAMS=off
    -DBUILD_SHARED_LIBS=OFF # Ensure static
    -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/dist
    -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
)
file(READ ${CMAKE_CURRENT_SOURCE_DIR}/third_party/mbedtls/include/mbedtls/mbedtls_config.h INPUT_CONTENT)
string(REPLACE "//#define MBEDTLS_SSL_DTLS_SRTP" "#define MBEDTLS_SSL_DTLS_SRTP" MODIFIED_CONTENT ${INPUT_CONTENT})
file(WRITE ${CMAKE_CURRENT_SOURCE_DIR}/third_party/mbedtls/include/mbedtls/mbedtls_config.h ${MODIFIED_CONTENT})

ExternalProject_Add(srtp2
  SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/libsrtp
  CMAKE_ARGS
    -DCMAKE_C_FLAGS="-fPIC"
    -DTEST_APPS=off
    -DBUILD_SHARED_LIBS=OFF # Ensure static
    -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/dist
    -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
)

ExternalProject_Add(libwebsockets
  SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/libwebsockets
  CMAKE_ARGS
    -DCMAKE_C_FLAGS="-fPIC" #-D_POSIX_C_SOURCE=200112L -D_REENTRANT -D_UCLIBC_ -D_XOPEN_SOURCE=700 -D_GNU_SOURCE -std=c99 -DIFA_FLAGS=8 -Wno-error=sign-conversion"
    # -DCMAKE_INCLUDE_PATH=${CMAKE_BINARY_DIR}/dist/include
    # -DLWS_MBEDTLS_INCLUDE_DIRS=${CMAKE_BINARY_DIR}/dist/include ${CMAKE_BINARY_DIR}/dist/include/mbedtls 
    # -DLWS_MBEDTLS_LIBRARIES=${CMAKE_BINARY_DIR}/dist/lib/libmbedcrypto.a ${CMAKE_BINARY_DIR}/dist/lib/libmbedx509.a ${CMAKE_BINARY_DIR}/dist/lib/libmbedtls.a 
    -DBUILD_SHARED_LIBS=OFF # Ensure static
    -DLWS_WITH_SHARED=OFF
    -DLWS_WITH_STATIC=ON
    -DLWS_WITH_MBEDTLS=ON    
    -DLWS_WITH_LIBCAP=OFF
    -DLWS_HAVE_LINUX_IPV6_H=OFF
    -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/dist
    -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
  DEPENDS mbedtls
)

message(STATUS "CMAKE_BINARY_DIR is: ${CMAKE_BINARY_DIR}")
message(STATUS "CMAKE_CURRENT_SOURCE_DIR is: ${CMAKE_CURRENT_SOURCE_DIR}")
message(STATUS "CMAKE_TOOLCHAIN_FILE is: ${CMAKE_TOOLCHAIN_FILE}")
