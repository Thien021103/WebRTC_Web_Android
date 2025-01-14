# I. Clone từ link Github:
`https://github.com/Thien021103/WebRTC_Web_Android.git`

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
- Sửa địa chỉ IP trong file `local.properties`, ở lệnh dưới đây về địa chỉ IP của máy chạy signaling server:
```
SIGNALING_SERVER_IP_ADDRESS="ws://192.168.1.123:8000"
```
- Cài đặt và chạy chương trình trên điện thoại. Bấm vào `Ready to start session`