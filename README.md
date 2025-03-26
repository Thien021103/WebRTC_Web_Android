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
- Go to generic-webcam/third_party/libwebsockets/lib/core/context.c
