#include <Wire.h>
#include "RAK12039_PMSA003I.h" 
#include <WiFi.h>
#include <HTTPClient.h>


#define WIFI_SSID "Natalia"
#define WIFI_PASSWORD "beatriz580"
#define SERVER_URL "http://3.215.122.64:8081/sensor/calidad"

RAK_PMSA003I PMSA003I;
#define SET_PIN WB_IO6 

unsigned long previousMillis = 0;
const long interval = 30000; //  30 segundos los datos por la pauta

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(100);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nConectado a WiFi");

  Wire.begin();
  pinMode(WB_IO2, OUTPUT);
  digitalWrite(WB_IO2, HIGH);

  pinMode(SET_PIN, OUTPUT);
  digitalWrite(SET_PIN, HIGH);

  while (!PMSA003I.begin()) {
    Serial.println("Fallo con el sensor PMSA003I.");
    delay(1000);
  }
  Serial.println("PMSA003I inicializado");
}

void loop() {
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    PMSA_Data_t data;
    bool pmsa_success = PMSA003I.readDate(&data);

    String jsonData = "{";
    if (pmsa_success) {
      jsonData += "\"pm10_standard\":" + String(data.pm10_standard) + ",";
      jsonData += "\"pm25_standard\":" + String(data.pm25_standard) + ",";
      jsonData += "\"pm100_standard\":" + String(data.pm100_standard) + ",";
      jsonData += "\"pm10_env\":" + String(data.pm10_env) + ",";
      jsonData += "\"pm25_env\":" + String(data.pm25_env) + ",";
      jsonData += "\"pm100_env\":" + String(data.pm100_env);
    } else {
      jsonData += "\"error\":\"No se pudieron leer los datos del PMSA003I\"";
    }
    jsonData += "}";

    Serial.println("Datos enviados al servidor:");
    Serial.println(jsonData);

    if (WiFi.status() == WL_CONNECTED) {
      HTTPClient http;
      http.begin(SERVER_URL);
      http.addHeader("Content-Type", "application/json");

      int httpResponseCode = http.POST(jsonData);
      if (httpResponseCode > 0) {
        Serial.println("Datos enviados exitosamente al servidor.");
      } else {
        Serial.print("Error al enviar datos: ");
        Serial.println(httpResponseCode);
      }
      http.end();
    } else {
      Serial.println("WiFi desconectado.");
    }
  }
}
