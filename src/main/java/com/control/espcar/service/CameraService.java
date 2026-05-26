package com.control.espcar.service;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CameraService {

    public static class Camera {
        public int cameraId;
        public String ip;
        public int controlPort;

        public Camera(int cameraId, String ip, int controlPort) {
            this.cameraId = cameraId;
            this.ip = ip;
            this.controlPort = controlPort;
        }

        public int getCameraId() { return cameraId; }
        public String getIp() { return ip; }
        public int getControlPort() { return controlPort; }
    }

    private final Map<Integer, Camera> cameras = Map.of(
            1, new Camera(1, "192.168.0.100", 6000),
            2, new Camera(2, "192.168.0.151", 6000)
    );

    public Camera getCamera(int id) {
        return cameras.get(id);
    }
    public Collection<Camera> findAll(){
        Collection<Camera> values = cameras.values();
        return values;
    }
}