project(lib)

file(GLOB SRCS "*.c")

file(GLOB HEADERS "peer.h" "peer_connection.h" "peer_signaling.h")

add_library(peer
  ${SRCS}
)

add_dependencies(peer mbedtls srtp2 libwebsockets)

find_package(PkgConfig REQUIRED)

target_link_libraries(peer ${DEP_LIBS})

message(STATUS "DEP_LIBS in src is: ${DEP_LIBS}")

set_target_properties(peer PROPERTIES PUBLIC_HEADER "${HEADERS}")

install(TARGETS peer
  LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib/
  PUBLIC_HEADER DESTINATION ${CMAKE_INSTALL_PREFIX}/include/
)

