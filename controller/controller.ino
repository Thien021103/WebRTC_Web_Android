#include <WiFi.h>
#include <WiFiManager.h>
#include <WebSocketsClient.h>
#include <ESP32Servo.h>

#define BUTTON_PIN 4
#define SERVO_PIN 5
#define CONTROLLER_ID "DOOR_001"

WebSocketsClient webSocket;
Servo doorServo;
WiFiManager wifiManager;

bool wsConnected = false;
bool wsConfigured = false;

volatile bool buttonPressed = false;

void IRAM_ATTR handleButtonPress() {
  buttonPressed = true;
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  String message = String((char*)payload);

  switch(type) {
    case WStype_DISCONNECTED:
      Serial.println("WebSocket Disconnected");
      wsConnected = false;
      break;
    case WStype_CONNECTED:
      Serial.println("WebSocket Connected");
      wsConnected = true;
      break;
    case WStype_TEXT:
      Serial.println("Received: " + message);
      if (message == "UNLOCK " + String(CONTROLLER_ID)) {
        Serial.println("Unlocking door...");
        doorServo.write(90); // Unlock position
        delay(2000);        // Simulate unlock duration
        doorServo.write(0);  // Back to locked position
        Serial.println("Door locked again");
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

  doorServo.attach(SERVO_PIN);
  doorServo.write(0); // Initial locked position
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

      // Reinitialize WebSocket connection
      webSocket.begin("thientranduc.id.vn", 444, "/");
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
      Serial.println("Attempting WebSocket connection...");
      webSocket.begin("thientranduc.id.vn", 444, "/");
      if(wsConnected) {
        webSocket.sendTXT("CONNECT controller" + String(CONTROLLER_ID));
      }
    } else {
      webSocket.sendTXT("CONNECT controller" + String(CONTROLLER_ID));
    }
  }
}