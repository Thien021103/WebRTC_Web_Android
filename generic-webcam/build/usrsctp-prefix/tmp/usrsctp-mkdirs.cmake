# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/usrsctp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix/src/usrsctp-build"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix/tmp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix/src/usrsctp-stamp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix/src"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix/src/usrsctp-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix/src/usrsctp-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/usrsctp-prefix/src/usrsctp-stamp${cfgdir}") # cfgdir has leading slash
endif()
