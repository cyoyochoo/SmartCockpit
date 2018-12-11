package com.hsae.smartcockpit.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

public class SubscribeActionToken implements IMqttToken {
    private String[] topic;

    public SubscribeActionToken(String[] topic) {
        this.topic = topic;
    }

    @Override
    public void waitForCompletion() throws MqttException {

    }

    @Override
    public void waitForCompletion(long timeout) throws MqttException {

    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public MqttException getException() {
        return null;
    }

    @Override
    public void setActionCallback(IMqttActionListener listener) {

    }

    @Override
    public IMqttActionListener getActionCallback() {
        return null;
    }

    @Override
    public IMqttAsyncClient getClient() {
        return null;
    }

    @Override
    public String[] getTopics() {
        return topic;
    }

    @Override
    public void setUserContext(Object userContext) {

    }

    @Override
    public Object getUserContext() {
        return null;
    }

    @Override
    public int getMessageId() {
        return 0;
    }

    @Override
    public int[] getGrantedQos() {
        return new int[0];
    }

    @Override
    public boolean getSessionPresent() {
        return false;
    }

    @Override
    public MqttWireMessage getResponse() {
        return null;
    }
}
