package com.control.espcar.service.impl;

import com.control.espcar.entity.DeviceInfo;
import com.control.espcar.entity.DeviceType;
import com.control.espcar.repository.DeviceInfoRepository;
import com.control.espcar.repository.DeviceTypeRepository;
import com.control.espcar.service.interf.DeviceInfoService;
import com.control.espcar.utils.mqtt.MqttService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor

public class DeviceInfoServiceImpl implements DeviceInfoService {
    @Autowired
    DeviceInfoRepository deviceInfoRepository;
    @Autowired
    DeviceTypeRepository deviceTypeRepository;

    private final MqttService mqttService;

    private final ObjectMapper objectMapper;

    @Override
    public void handleRegister(String topic, String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            String deviceId = (String) data.get("deviceId");
            String typeId = (String) data.get("typeId");
            DeviceType type = deviceTypeRepository.findById(typeId)
                    .orElseThrow(() -> new RuntimeException("Unknown type"));

            DeviceInfo device = deviceInfoRepository.findById(deviceId)
                    .orElse(new DeviceInfo());

            device.setId(deviceId);
            device.setDeviceType(type);
            device.setDeviceName((String) data.get("deviceName"));
            device.setFirmwareVersion((String) data.get("firmware"));
            device.setIpAddress((String) data.get("ipAddress"));
            device.setMacAddress((String) data.get("macAddress"));
            device.setConfigJson((String) data.get("configJson"));

            deviceInfoRepository.save(device);
            // gửi config lại ESP
            sendConfig(device);
        }catch (Exception e){

        }
    }
    private void sendConfig(DeviceInfo device) throws Exception {
        Map<String, Object> config = new HashMap<>();

        config.put("configVersion", 1);

        Map<String, Object> streamConfig = new HashMap<>();
        streamConfig.put("server", "192.168.0.101");
        streamConfig.put("udpPort", 5000);
        config.put("stream", streamConfig);

        String json = objectMapper.writeValueAsString(config);
        String topic = "devices/" + device.getId() + "/config";
        mqttService.publish(topic, json);
        System.out.println("📤 Config sent to " + device.getId());
    }
}
