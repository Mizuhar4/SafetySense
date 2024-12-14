# Final Project 
# Author: Benjamín Fernández

## Description

This project serves as the capstone for the Computing Workshop course and focuses on developing a monitoring system to control air quality and noise pollution in company warehouses. The system utilizes a mix of technologies including Android Studio, Arduino IDE, a Flask server, MongoDB database, and data visualizations through HTML graphs. Additionally, the system is capable of sending email alerts when dangerous levels of pollutants or noise are detected.

## Project Components

### Android Studio
- **Functionality**: The mobile application developed in Android Studio allows users to view real-time data on air quality and noise levels collected by sensors.
- **Interface**: Provides a user-friendly graphical interface for data visualization and alert reception.

### Arduino IDE
- **Sensors**: Employs sensors connected to an Arduino board to measure air quality and noise in the facilities.
- **Control**: Processes the data collected by the sensors and sends it to the Flask server.

### Flask Server
- **API**: Manages data requests and sends commands to the MongoDB database to store and retrieve data.
- **Alerts**: Assesses the incoming data and sends email alerts when levels exceed predefined thresholds.

### MongoDB
- **Data Storage**: Stores all air quality and noise data, allowing for historical and real-time analysis.
- **Data Management**: Facilitates quick and efficient data queries for monitoring and visualization.

### HTML Graphs
- **Visualization**: Presents detailed HTML graphs that help users better understand pollution patterns and levels over time.

### Email Notifications
- **Automatic Alerts**: Configured to send email notifications to administrators when sensors detect conditions that surpass safe levels.
