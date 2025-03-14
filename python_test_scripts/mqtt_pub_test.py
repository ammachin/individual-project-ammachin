import sys
import paho.mqtt.client as mqtt

client = mqtt.Client()

# 1883 - default MQTT port
# 60 - time alive (default)
if client.connect("localhost", 1883, 60) != 0:
    print("Unable to connect to MQTT broker")
    sys.exit(1)

client.publish("phone_location", "(4.0, 1.0, 0.5)", 0)
print("sent!") # For some reason, adding this print makes everything work?
client.disconnect()