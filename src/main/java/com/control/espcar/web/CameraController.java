package com.control.espcar.web;


import com.control.espcar.entity.CameraInfo;
import com.control.espcar.service.CameraService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    /**
     * Danh sách camera
     */
    @GetMapping
    public Collection<CameraService.Camera> getAll() {

        return cameraService
                .findAll();
    }

    /**
     * Camera theo ID
     */
    @GetMapping("/{cameraId}")
    public CameraService.Camera getCamera(
            @PathVariable Integer cameraId
    ) {

        return cameraService.getCamera(cameraId);
    }
}
