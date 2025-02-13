# Install script for directory: /home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/home/thien-gay/WebRTC_Web_Android/generic-webcam/build/dist")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "1")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "FALSE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/usr/bin/objdump")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/mbedtls" TYPE FILE PERMISSIONS OWNER_READ OWNER_WRITE GROUP_READ WORLD_READ FILES
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/aes.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/aria.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/asn1.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/asn1write.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/base64.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/bignum.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/build_info.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/camellia.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ccm.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/chacha20.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/chachapoly.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/check_config.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/cipher.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/cmac.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/compat-2.x.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/config_psa.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/constant_time.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ctr_drbg.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/debug.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/des.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/dhm.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ecdh.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ecdsa.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ecjpake.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ecp.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/entropy.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/error.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/gcm.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/hkdf.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/hmac_drbg.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/legacy_or_psa.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/lms.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/mbedtls_config.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/md.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/md5.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/memory_buffer_alloc.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/net_sockets.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/nist_kw.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/oid.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/pem.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/pk.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/pkcs12.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/pkcs5.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/pkcs7.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/platform.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/platform_time.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/platform_util.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/poly1305.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/private_access.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/psa_util.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ripemd160.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/rsa.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/sha1.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/sha256.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/sha512.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ssl.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ssl_cache.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ssl_ciphersuites.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ssl_cookie.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/ssl_ticket.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/threading.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/timing.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/version.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/x509.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/x509_crl.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/x509_crt.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/mbedtls/x509_csr.h"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/psa" TYPE FILE PERMISSIONS OWNER_READ OWNER_WRITE GROUP_READ WORLD_READ FILES
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_builtin_composites.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_builtin_primitives.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_compat.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_config.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_driver_common.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_driver_contexts_composites.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_driver_contexts_primitives.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_extra.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_platform.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_se_driver.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_sizes.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_struct.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_types.h"
    "/home/thien-gay/WebRTC_Web_Android/generic-webcam/third_party/mbedtls/include/psa/crypto_values.h"
    )
endif()

