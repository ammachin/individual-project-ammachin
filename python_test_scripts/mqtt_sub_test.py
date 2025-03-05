import sys
import paho.mqtt.client as mqtt

# msg callback function
def on_message(client, data, msg):#
    print(f"{msg.topic}: {msg.payload.decode()}")

client = mqtt.Client()
client.on_message = on_message

if client.connect(host="localhost", port=1883, keepalive=60) != 0:
    print("Unable to connect to MQTT broker")
    sys.exit(1)

client.subscribe("phone_location")

# blocking call - continuously receive messages
client.loop_forever()