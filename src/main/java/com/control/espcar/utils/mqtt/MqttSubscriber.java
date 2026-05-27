package com.control.espcar.utils.mqtt;

import com.control.espcar.service.interf.DeviceInfoService;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MqttSubscriber implements MqttCallback {
    private final MqttClient mqttClient;
    private final DeviceInfoService deviceService;

    public void init() throws MqttException {

        mqttClient.setCallback(this);

        // subscribe register topic
        mqttClient.subscribe("devices/+/register", 1);

        System.out.println("📡 Subscribed to register topic");
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("MQTT lost connection");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());

            System.out.println("📥 Register received: " + payload);

            deviceService.handleRegister(topic, payload);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
