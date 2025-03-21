"""
Class for subscribing to MQTT topic
"""

import sys
import paho.mqtt.client as mqtt

class MQTTSub:
    host = "localhost"
    port = 1883
    keep_alive = 60
    topic = "phone_location"
    current_msg = ""

    client = mqtt.Client()

    """
    Message callback function.
    """
    def on_message(self, client, data, msg):
        self.current_msg = msg.payload.decode()
        print("Message received!")
        self.client.disconnect()

    """
    Connect and subscribe to MQTT topic.
    """
    def connect(self):
        self.client.on_message = self.on_message

        if self.client.connect(host=self.host, port=self.port, keepalive=self.keep_alive) != 0:
            print("Unable to connect to MQTT broker")
            return 
        
        self.client.subscribe(self.topic)

        # blocking call - only exits when message is received
        self.client.loop_forever()
