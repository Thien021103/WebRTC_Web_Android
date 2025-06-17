# WebRTC Web Android Project

This project implements a WebRTC-based solution for web and Android platforms, integrating camera functionality, a signaling server, and an admin panel. Below are the setup and build instructions.

Extract and use the given zip folder.

## Running on Camera

### Without Camera Hardware (Linux Demo)
The camera requires a proprietary toolchain and libraries, which are not included due to licensing restrictions. A demo with similar functionality is available in `generic-webcam/examples/new_exam` and runs on a standard Linux computer. There are an executable for using with cam on the zip file.

**Steps:**
1. Set up the signaling server as described in [Signaling Server](#signaling-server).
2. Navigate to the `generic-webcam` directory:
   ```bash
   cd WebRTC_Web_Android/generic-webcam
   ```
3. Run the demo, replacing `<cameraId>` with the appropriate camera ID:
   ```bash
   ./newexam <cameraId>
   ```
   - Replace `<cameraId>` with the appropriate camera ID.
   > **Note**: This demo requires GStreamer, visit (https://gstreamer.freedesktop.org/download/#linux).
### With Camera Hardware
Follow these steps to run on the camera hardware.

**Steps:**
1. Set up the signaling server as described in [Signaling Server](#signaling-server).
2. Copy the built `cam` executable from `generic-webcam/` to the camera (e.g., via SD card).
3. On the camera's console, navigate to and run:
   ```bash
   ./cam <cameraId> <wifi> <password>
   ```
   - Replace `<cameraId>`, `<wifi>`, and `<password>` with the camera ID, Wi-Fi SSID, and password, respectively (e.g., `./cam 0 MyWiFi MyPassword`).

## Android App
To run the Android app:

1. Open Android Studio and load the `WebRTC_Web_Android/webrtc-android` directory.
2. Build and run the app using Android Studio's standard run configuration.

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

## Dependencies
This project depends on the following external libraries:
- [libpeer](https://github.com/sepfy/libpeer)
- [webrtc-android](https://github.com/GetStream/webrtc-android)