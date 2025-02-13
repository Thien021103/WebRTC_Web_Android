# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix/src/mbedtls-build"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix/tmp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix/src/mbedtls-stamp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix/src"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix/src/mbedtls-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix/src/mbedtls-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/mbedtls-prefix/src/mbedtls-stamp${cfgdir}") # cfgdir has leading slash
endif()
