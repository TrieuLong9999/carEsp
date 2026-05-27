package com.control.espcar.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Table(name = "DEVICE_INFO")
public class DeviceInfo {
    @Id
    private String id;

    @Column(name = "DEVICE_NAME")
    private String deviceName;

    // liên kết type
    @ManyToOne
    @JoinColumn(name = "TYPE_ID")
    private DeviceType deviceType;

    // firmware
    @JoinColumn(name = "FIRMWARE_VERSION")
    private String firmwareVersion;

    // network
    @JoinColumn(name = "IP_ADDRESS")
    private String ipAddress;

    @JoinColumn(name = "MAC_ADDRESS")
    private String macAddress;


    // override capability (nếu device khác type)
    @ElementCollection
    @CollectionTable(
            name = "DEVICE_OVERRIDE_CAPABILITIES",
            joinColumns = @JoinColumn(name = "DEVICE_ID")
    )
    @Column(name = "CAPABILITY")
    private List<String> overrideCapabilities;

    // runtime state (JSON)
    @Column(columnDefinition = "TEXT")
    private String stateJson;

    // config từ server (JSON)
    @Column(columnDefinition = "TEXT")
    private String configJson;

    private Instant createdAt;

    private Instant updatedAt;

}
