package com.hsae.smartcockpit.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttListener {
    void connectComplete(boolean reconnect, String serverURI);
    void connectionLost(Throwable cause);
    void messageArrived(String topic, MqttMessage message);
    void deliveryComplete(IMqttDeliveryToken token);
    void onSuccess(IMqttToken asyncActionToken);
    void onFailure(IMqttToken asyncActionToken, Throwable exception);
}
