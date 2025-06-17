# WebRTC Web Android Project

This project implements a WebRTC-based solution for web and Android platforms, integrating camera functionality, a signaling server, and an admin panel. Below are the setup and build instructions.

## Cloning the Repository
Clone the repository using one of the following methods:

```bash
git clone --recursive https://github.com/Thien021103/WebRTC_Web_Android.git
```

OR

```bash
git clone https://github.com/Thien021103/WebRTC_Web_Android.git
cd WebRTC_Web_Android
git submodule update --init --recursive
```

Or just extract and use the given zip folder.

## Running on Camera

### Without MIPS Toolchain (Linux Demo)
The camera requires a proprietary MIPS toolchain and libraries, which are not included due to licensing restrictions. A demo with similar functionality is available in `generic-webcam/examples/new_exam` and runs on a standard Linux computer.

**Steps:**
1. Clone the repository as described in [Cloning the Repository](#cloning-the-repository).
2. Set up the third party libraries as described in [Modifications for libwebsockets with mbedtls](#modifications-for-libwebsockets).
3. Set up the signaling server as described in [Signaling Server](#signaling-server).
4. Navigate to the `generic-webcam` directory:
   ```bash
   cd WebRTC_Web_Android/generic-webcam
   ```
5. Build the demo:
   ```bash
   ./build.sh
   ```
   OR
   ```bash
   cmake -S . -B build && cmake --build build
   ```
6. Run the demo, replacing `<cameraId>` with the appropriate camera ID (e.g., `0` for the default camera):
   ```bash
   ./build/examples/new_exam/newexam <cameraId>
   ```

### With MIPS Toolchain (Camera Hardware)
If you have the MIPS toolchain and required libraries, follow these steps to build and run on the camera hardware.

**Steps:**
1. Edit `generic-webcam/CMakeLists.txt` to uncomment and configure the toolchain settings:
   ```cmake
   set(CMAKE_CXX_STANDARD 11)
   set(CMAKE_C_STANDARD 99)
   set(CMAKE_C_COMPILER "/opt/mips-gcc472-glibc216-64bit/bin/mips-linux-uclibc-gnu-gcc")
   set(CMAKE_CXX_COMPILER "/opt/mips-gcc472-glibc216-64bit/bin/mips-linux-uclibc-gnu-g++")
   set(CMAKE_TOOLCHAIN_FILE ${CMAKE_CURRENT_SOURCE_DIR}/toolchain.cmake)
   ```
   > **Note**: Update the paths to match your toolchain installation.
2. Edit `generic-webcam/examples/CMakeLists.txt` to enable the camera-specific example:
   - Change:
     ```cmake
     add_subdirectory(new_exam)
     # add_subdirectory(static_cam)
     ```
     to:
     ```cmake
     # add_subdirectory(new_exam)
     add_subdirectory(static_cam)
     ```
3. Place your compiled libraries and headers in `generic-webcam/pps_dua`.
4. Place the camera-specific code in `generic-webcam/examples/static_cam`.
5. Navigate to `generic-webcam`:
   ```bash
   cd WebRTC_Web_Android/generic-webcam
   ```
6. Build the project:
   ```bash
   ./build.sh
   ```
   OR
   ```bash
   cmake -S . -B build && cmake --build build
   ```
7. Copy the built `cam` executable from `generic-webcam/build/examples/static_cam/` to the camera (e.g., via SD card).
8. On the camera’s console, run:
   ```bash
   ./cam <cameraId> <wifi> <password>
   ```
   - Replace `<cameraId>`, `<wifi>`, and `<password>` with the camera ID, Wi-Fi SSID, and password, respectively (e.g., `./cam 0 MyWiFi MyPassword`).

## Android App
To run the Android app:

1. Clone the repository as described in [Cloning the Repository](#cloning-the-repository).
2. Open Android Studio and load the `WebRTC_Web_Android/webrtc-android` directory.
3. Build and run the app using Android Studio’s standard run configuration.

## Signaling Server
Run the signaling server locally using Docker:

1. Navigate to the server directory:
   ```bash
   cd WebRTC_Web_Android/signaling-server
   ```
2. Start the server:
   ```bash
   docker-compose up -d --build
   ```
   > **Note**: Ensure Docker and Docker Compose are installed.

## Admin Panel
Run the admin panel locally:

1. Navigate to the admin panel directory:
   ```bash
   cd WebRTC_Web_Android/admin-panel
   ```
2. Start the development server:
   ```bash
   npm run dev
   ```
   > **Note**: Ensure Node.js and npm are installed.

## Modifications for libwebsockets
To build `libwebsockets` with `mbedtls`, apply the following changes:

1. In `generic-webcam/third_party/libwebsockets/lib/core/context.c`:
   - Replace:
     ```c
     #if defined(MBEDTLS_VERSION_C)
     ```
     with:
     ```c
     #if 0
     ```
2. In `generic-webcam/third_party/libwebsockets/lib/tls/mbedtls/wrapper/platform/ssl_pm.c`:
   - Replace:
     ```c
     mbedtls_net_init(&ssl_pm->fd);
     mbedtls_net_init(&ssl_pm->cl_fd);
     ```
     with:
     ```c
     ssl_pm->fd.fd = -1;
     ssl_pm->cl_fd.fd = -1;
     ```
3. In `generic-webcam/third_party/mbedtls/library/alignment.h`:
   - Ensure the following functions are marked `inline` (add if missing):
     ```c
     inline uint16_t mbedtls_get_unaligned_uint16(const void *p);
     inline void mbedtls_put_unaligned_uint16(void *p, uint16_t x);
     inline uint32_t mbedtls_get_unaligned_uint32(const void *p);
     inline void mbedtls_put_unaligned_uint32(void *p, uint32_t x);
     inline uint64_t mbedtls_get_unaligned_uint64(const void *p);
     inline void mbedtls_put_unaligned_uint64(void *p, uint64_t x);
     ```
4. In `generic-webcam/third_party/mbedtls/library/common.h`:
   - Ensure the following function is marked `inline` (add if missing):
     ```c
     inline void mbedtls_xor(unsigned char *r, const unsigned char *a, const unsigned char *b, size_t n);
     ```
5. Navigate to `generic-webcam` and rebuild:
   ```bash
   cd WebRTC_Web_Android/generic-webcam
   cmake -S . -B build
   cmake --build build
   ```

## Dependencies
This project depends on the following external libraries:
- [libpeer](https://github.com/sepfy/libpeer)
- [libwebsockets](https://github.com/warmcat/libwebsockets)
- [webrtc-android](https://github.com/GetStream/webrtc-android)