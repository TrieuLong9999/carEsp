package com.control.espcar.db;

import com.control.espcar.entity.DeviceType;
import com.control.espcar.repository.DeviceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DeviceTypeRepository deviceTypeRepository;

    @Override
    public void run(String... args) throws Exception {


        if (deviceTypeRepository.count() <= 0) {
            // ===== RC CAR =====
            List<DeviceType> allInit = new ArrayList<>();

            DeviceType rcCar = new DeviceType();
            rcCar.setTypeId("RC_CAR_CAMERA");
            rcCar.setTypeName("RC Car camera");
            rcCar.setDescription("Remote control car with motor + steering + camera esp");

            rcCar.setDefaultCapabilities(List.of(
                    "UDP_STREAM",
                    "GPIO_CONTROL",
                    "MQTT_CONNECT"
            ));

            allInit.add(rcCar);


            deviceTypeRepository.saveAll(allInit);
        }

    }
}
