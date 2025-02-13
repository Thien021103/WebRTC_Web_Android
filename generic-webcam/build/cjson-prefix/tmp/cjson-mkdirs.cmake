# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/cJSON"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix/src/cjson-build"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix/tmp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix/src/cjson-stamp"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix/src"
  "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix/src/cjson-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix/src/cjson-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/cjson-prefix/src/cjson-stamp${cfgdir}") # cfgdir has leading slash
endif()
