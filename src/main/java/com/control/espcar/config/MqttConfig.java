package com.control.espcar.config;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MqttConfig {
    private final MqttProperties props;

    @Bean
    public MqttClient mqttClient() throws MqttException {

        MqttClient client = new MqttClient(
                props.getBrokerUrl(),
                props.getClientId()
        );

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(props.isCleanSession());
        options.setKeepAliveInterval(props.getKeepAlive());

        if (props.getUsername() != null && !props.getUsername().isEmpty()) {
            options.setUserName(props.getUsername());
            options.setPassword(props.getPassword().toCharArray());
        }

        client.connect(options);

        System.out.println("✅ MQTT connected");

        return client;
    }
}
