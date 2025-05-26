#include <WiFi.h>
#include <WiFiManager.h>
#include <WebSocketsClient.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#define BUTTON_PIN 12
#define SERVO_PIN 14
#define CONTROLLER_ID "con1"
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
WebSocketsClient webSocket;
Servo doorServo;
WiFiManager wifiManager;

bool wsConnected = false;
bool wsConfigured = false;
String CONTROLLER_TOKEN = "";
String WEBRTC_STATE = "Impossible";
String DOOR_STATE = "Locked";

volatile bool buttonPressed = false;

void IRAM_ATTR handleButtonPress() {
  buttonPressed = true;
}

void updateDisplay(String status, String token = "") {
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println("Door Lock Status");
  display.println(WiFi.status() == WL_CONNECTED ? "WiFi: Connected" : "WiFi: Disconnected");
  display.println(wsConnected ? "WebSocket: Connected" : "WebSocket: Disconnected");
  display.println("Status: " + status);
  if (token.length() > 0) {
    display.println("Token: " + token.substring(0, min(token.length(), 20U)));
  }
  display.display();
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  String message = String((char*)payload);

  switch(type) {
    case WStype_DISCONNECTED:
      Serial.println("WebSocket Disconnected");
      CONTROLLER_TOKEN = "";
      wsConnected = false;
      break;
    case WStype_CONNECTED:
      Serial.println("WebSocket Connected");
      CONTROLLER_TOKEN = "";
      wsConnected = true;
      webSocket.sendTXT("CONNECT controller " + String(CONTROLLER_ID));
      break;
    case WStype_TEXT:
      Serial.println("Received: " + message);
      if (message == "UNLOCK " + String(CONTROLLER_ID)) {
        Serial.println("Unlocking door...");
        updateDisplay("Unlocking ...");
        DOOR_STATE = "Unlocked";
        doorServo.write(90); // Unlock position
        delay(2000);        // Simulate unlock duration
        doorServo.write(0);  // Back to locked position
        Serial.println("Door locked again");
        DOOR_STATE = "Locked";
        webSocket.sendTXT("LOCK controller " + CONTROLLER_TOKEN);
      }
      else if (message == "LOCK " + String(CONTROLLER_TOKEN)) {
        if (DOOR_STATE == "Unlocked") {
          Serial.println("Locking door...");
          doorServo.write(0);  // Back to locked position
        }
      }
      else if (message.startsWith("STATE ")) {
        int index = message.indexOf(' ');
        if (index != -1 && index + 1 < message.length()) {
          WEBRTC_STATE = message.substring(index + 1);
          updateDisplay("State " + WEBRTC_STATE);
        }
      }
      else if (message.startsWith("TOKEN ")) {
        // Extract token
        int index = message.indexOf(' ');
        if (index != -1 && index + 1 < message.length()) {
          CONTROLLER_TOKEN = message.substring(index + 1);
          if (CONTROLLER_TOKEN.length() > 0) {
            Serial.println("Received TOKEN: " + CONTROLLER_TOKEN);
          } else {
            Serial.println("Error: Empty token received");
            webSocket.disconnect();
          }
        } else {
          Serial.println("Error: Invalid TOKEN message format");
          webSocket.disconnect();
        }
      }
      break;
    default:
      break;
  }
}

void setup() {
  Serial.begin(115200);
  
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(BUTTON_PIN), handleButtonPress, FALLING);
 
  // Initial locked position  
  doorServo.attach(SERVO_PIN);
  doorServo.write(0); 
  DOOR_STATE = "Locked";

  String WEBRTC_STATE = "Impossible";

  // Initialize OLED
  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println("OLED failed");
    for(;;);
  }
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println("Initializing...");
  display.display();

  wifiManager.setTimeout(180);
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(5000);
  wsConfigured = true;

  webSocket.beginSSL("thientranduc.id.vn", 444, "/");
}

void loop() {

  // Check WiFi status
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected. Attempting to reconnect...");

    wsConnected = false; // Reset WebSocket flag
    
    if (wifiManager.autoConnect("ESP32_DoorLock")) {
      Serial.println("Reconnected to WiFi");
      Serial.print("IP Address: ");
      Serial.println(WiFi.localIP());
    } else {
      Serial.println("Failed to reconnect to WiFi");
      delay(5000); // Wait before retrying to avoid spamming
      return; // Skip the rest of the loop
    }
  }

  // Configure WebSocket if not already done
  if (!wsConfigured) {
    webSocket.onEvent(webSocketEvent);
    webSocket.setReconnectInterval(5000);
    wsConfigured = true;
  }

  webSocket.loop();

  if (buttonPressed) {
    buttonPressed = false;
    Serial.println("Button pressed!");
    // Retry Websocket connection
    if (!wsConnected) {
      Serial.println("Attempting WebSocket reconnection...");
      updateDisplay("Connecting ...");
      webSocket.beginSSL("thientranduc.id.vn", 444, "/");
      if(wsConnected && CONTROLLER_TOKEN.length() > 0) {
        webSocket.sendTXT("NOTIFY controller " + CONTROLLER_TOKEN);
        updateDisplay("Notifying");
      }
    } else if (CONTROLLER_TOKEN.length() > 0) {
      webSocket.sendTXT("NOTIFY controller " + CONTROLLER_TOKEN);
      updateDisplay("Notifying");
    }
  }
}