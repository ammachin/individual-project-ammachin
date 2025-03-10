package com.unileeds.Android_App;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class MQTTClient {
    MqttAndroidClient client;
    MqttConnectOptions options = new MqttConnectOptions();
    String broker;
    String client_id;

    public MQTTClient(Context context) {
        broker = "tcp://192.168.0.22:1883";
        client_id = "android";

        client = new MqttAndroidClient(context, broker, client_id, Ack.AUTO_ACK, null, true, 0);
        client.setCallback(new MqttCallbackExtended() {
            // All debug messages
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("Mqtt Callback", "Connect complete");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d("Mqtt Callback", "Connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("Mqtt Callback", "Message arrived");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("Mqtt Callback", "Delivery complete");
            }
        });

        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
    }

    public void publish(String msg) {
        String topic = "phone_location";

        // Connecting is asynchronous, so we need to implement a callback
        client.connect(options, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("Mqtt", "Connection!");
                MqttMessage mqtt_msg = new MqttMessage();
                mqtt_msg.setPayload(msg.getBytes());
                mqtt_msg.setQos(1);
                mqtt_msg.setRetained(true); // We want there to be a fallback option
                client.publish(topic, mqtt_msg);
                Log.d("MQTT", "Msg published");
                client.disconnect();
                try {
                    client.close();
                } catch(IllegalArgumentException e) {
                    Log.d("Error", "MQTT connection error - don't need to worry I think");
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("Connection Failure", exception.toString());
            }
        });


    }

    public void close()  {
        try {
           client.close();
        } catch(IllegalArgumentException e) {
            Log.d("Error", "MQTT connection error - don't need to worry I think");
        }
    }

}
