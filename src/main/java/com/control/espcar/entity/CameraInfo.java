package com.control.espcar.entity;

import lombok.Data;

@Data
public class CameraInfo {

    private Integer cameraId;

    private String name;

    private String ip;

    private Integer controlPort;

    private Boolean online;
}
