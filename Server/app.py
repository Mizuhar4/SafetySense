from flask import Flask, request, jsonify
from flask_cors import CORS
from pymongo import MongoClient
import datetime
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

app = Flask(__name__)
CORS(app)

client = MongoClient('mongodb://localhost:27017/')
db = client['air_quality_db']
collection = db.sensor_data

def send_email(subject, body, recipient_email):
    sender_email = "b.fernandez07@ufromail.cl"  
    sender_password = "Pascualin1"  
    message = MIMEMultipart()
    message["From"] = sender_email
    message["To"] = recipient_email
    message["Subject"] = subject
    message.attach(MIMEText(body, 'plain'))
    server = smtplib.SMTP('smtp.gmail.com', 587)
    server.starttls()
    server.login(sender_email, sender_password)
    server.sendmail(sender_email, recipient_email, message.as_string())
    server.quit()

def classify_pm25(pm25):
    if pm25 > 250.5:
        return "Peligrosa"
    elif pm25 > 150.4:
        return "Muy insalubre"
    elif pm25 > 55.4:
        return "Insalubre"
    elif pm25 > 35.4:
        return "Insalubre para grupos sensibles"
    elif pm25 > 12.0:
        return "Moderada"
    else:
        return "Buena"

def classify_pm10(pm10):
    if pm10 > 425:
        return "Peligrosa"
    elif pm10 > 354:
        return "Muy insalubre"
    elif pm10 > 254:
        return "Insalubre"
    elif pm10 > 154:
        return "Insalubre para grupos sensibles"
    elif pm10 > 54:
        return "Moderada"
    else:
        return "Buena"

@app.route('/sensor/calidad', methods=['POST'])
def receive_data():
    data = request.get_json()
    pm25 = data.get('pm25_standard', 0)
    pm10 = data.get('pm10_standard', 0)
    calidad_pm25 = classify_pm25(pm25)
    calidad_pm10 = classify_pm10(pm10)
    data['calidad_pm25'] = calidad_pm25
    data['calidad_pm10'] = calidad_pm10
    data['timestamp'] = datetime.datetime.now()
    collection.insert_one(data)
    
    # Envío de correo si PM2.5 supera los 50
    if pm25 > 50:
        send_email("Alerta de Calidad del Aire - PM2.5 Elevado",
                   f"Alerta de PM2.5: Nivel detectado de {pm25} µg/m³, lo cual es superior al umbral seguro.",
                   "b.fernandez07@ufromail.cl")
    
    return jsonify({"message": "Data received and stored", "calidad_pm25": calidad_pm25, "calidad_pm10": calidad_pm10}), 200

@app.route('/sensor/data', methods=['GET'])
def send_data():
    data = list(collection.find().sort('timestamp', -1).limit(1))
    for item in data:
        item['_id'] = str(item['_id'])
    return jsonify(data), 200

@app.route('/graphic', methods=['GET'])
def graphic():
    return app.send_static_file('graphic.html')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8081)
