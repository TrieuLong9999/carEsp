package com.control.espcar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@Table(name = "DEVICE_TYPE")
@AllArgsConstructor
@NoArgsConstructor
public class DeviceType {
    @Id
    private String typeId; // ESP32_CAM, RC_CAR, SENSOR_NODE
    private String typeName;

    private String description;
    // capability mặc định của type
    @ElementCollection
    @CollectionTable(
            name = "DEVICE_TYPE_CAPABILITIES",
            joinColumns = @JoinColumn(name = "TYPE_ID")
    )
    @Column(name = "CAPABILITY")
    private List<String> defaultCapabilities;
}
