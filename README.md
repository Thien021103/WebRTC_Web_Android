# I. Clone từ link Github:
*https://github.com/Thien021103/WebRTC_Web_Android.git*

# II. Chạy signaling server:
- Trên 1 máy trong mạng LAN, vào thư mục `/signaling-server` 
- Chạy:
  - `npm install`
  - `npm start`

# III. Chạy web client:
- Trên 1 máy trong mạng LAN, vào thư mục `/client` 
- Vào file `/client/client.js`, sửa địa chỉ IP trong dòng lệnh dưới đây về địa chỉ IP của máy chạy signaling server:
```
const websocket = new WebSocket('ws://127.0.0.1:8000/');
```
- Chạy: `python -m http.server --bind 127.0.0.1 8080`
- Vào `http://127.0.0.1:8080/` trên browser

# IV. Chạy chương trình Android:
- Vào Android Studio, mở thư mục `/webrtc-android`
- Cài đặt và chạy chương trình trên điện thoại. 
- Nhập địa chỉ IP của máy đang chạy signaling server vào trong TextField hiện ra
- Bấm vào `Start Signaling`
- Bấm vào `Ready to start session`

# V. Build:
- Go to `generic-webcam/third_party/libwebsockets/lib/core/context.c`
  - Change:
  ```
  #if defined(MBEDTLS_VERSION_C)
  ```
  ---> To below:
  ```  
  #if 0
  ```
- Go to `generic-webcam/third_party/libwebsockets/lib/tls/mbedtls/wrapper/platform/ssl_pm.c`
  - Change:
  ```
  mbedtls_net_init(&ssl_pm->fd);
  mbedtls_net_init(&ssl_pm->cl_fd);
  ```
  ---> To below:
  ```  
  ssl_pm->fd.fd = -1;
  ssl_pm->cl_fd.fd = -1;
  ```

- Go to `/generic-camera`, run
  ```
  cmake -S . -B build
  cmake --build build
  ```

- Go to `/generic-webcam/third_party/mbedtls/library/alignment.h`
  - Add `inline` to these functions:
  ```
  mbedtls_get_unaligned_uint16
  mbedtls_put_unaligned_uint16
  mbedtls_get_unaligned_uint32
  mbedtls_put_unaligned_uint32
  mbedtls_get_unaligned_uint64
  mbedtls_put_unaligned_uint64
  ```

- Go to `/generic-webcam/third_party/mbedtls/library/common.h`
  - Add `inline` to these functions:
  ```
  mbedtls_xor
  ```
