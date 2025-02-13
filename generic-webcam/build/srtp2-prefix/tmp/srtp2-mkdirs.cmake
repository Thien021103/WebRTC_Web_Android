# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/libsrtp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix/src/srtp2-build"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix/tmp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix/src/srtp2-stamp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix/src"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix/src/srtp2-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix/src/srtp2-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/srtp2-prefix/src/srtp2-stamp${cfgdir}") # cfgdir has leading slash
endif()
