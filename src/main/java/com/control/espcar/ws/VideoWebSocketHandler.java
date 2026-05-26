package com.control.espcar.ws;

import com.control.espcar.service.CameraService;
import com.control.espcar.web.CameraControlService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VideoWebSocketHandler extends BinaryWebSocketHandler {

    private final CameraService cameraService;
    private final CameraControlService controlService;

    public VideoWebSocketHandler(
            CameraService cameraService,
            CameraControlService controlService
    ) {
        this.cameraService = cameraService;
        this.controlService = controlService;
    }

    // cameraId -> sessions
    private final Map<Integer, Set<WebSocketSession>> cameraSessions =
            new ConcurrentHashMap<>();

    // sessionId -> cameraId
    private final Map<String, Integer> sessionCameraMap =
            new ConcurrentHashMap<>();

    // tránh spam START/STOP
    private final Set<Integer> streamingCamera =
            ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("WS CONNECT: " + session.getId());
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {

        try {

            int cameraId = Integer.parseInt(message.getPayload().trim());

            Integer oldCameraId = sessionCameraMap.get(session.getId());

            // remove camera cũ
            if (oldCameraId != null) {
                Set<WebSocketSession> oldSet =
                        cameraSessions.get(oldCameraId);

                if (oldSet != null) {
                    oldSet.remove(session);
                }
            }

            boolean wasEmpty =
                    viewerCount(cameraId) == 0;

            // add camera mới
            cameraSessions
                    .computeIfAbsent(cameraId,
                            k -> ConcurrentHashMap.newKeySet())
                    .add(session);

            sessionCameraMap.put(session.getId(), cameraId);

            session.sendMessage(
                    new TextMessage("CONNECTED:" + cameraId)
            );

            System.out.println("VIEW camera " + cameraId);

            // =========================
            // START STREAM nếu là viewer đầu tiên
            // =========================
            if (wasEmpty && streamingCamera.add(cameraId)) {

                var cam = cameraService.getCamera(cameraId);

                if (cam != null) {
                    controlService.sendCommand(
                            cam.getIp(),
                            cam.getControlPort(),
                            "START"
                    );
                }
            }

        } catch (Exception e) {

            try {
                session.sendMessage(
                        new TextMessage("INVALID_CAMERA_ID")
                );
            } catch (Exception ignored) {}

            e.printStackTrace();
        }
    }

    public void broadcast(int cameraId, byte[] imageBytes) {

        Set<WebSocketSession> sessions =
                cameraSessions.get(cameraId);

        if (sessions == null || sessions.isEmpty()) return;

        sessions.removeIf(s -> !s.isOpen());

        BinaryMessage msg = new BinaryMessage(imageBytes);

        for (WebSocketSession s : sessions) {
            try {
                s.sendMessage(msg);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        Integer cameraId =
                sessionCameraMap.remove(session.getId());

        if (cameraId == null) return;

        Set<WebSocketSession> sessions =
                cameraSessions.get(cameraId);

        if (sessions != null) {
            sessions.remove(session);

            if (sessions.isEmpty()) {

                cameraSessions.remove(cameraId);

                // =========================
                // STOP STREAM khi không còn ai xem
                // =========================
                if (streamingCamera.remove(cameraId)) {

                    var cam = cameraService.getCamera(cameraId);

                    if (cam != null) {
                        controlService.sendCommand(
                                cam.getIp(),
                                cam.getControlPort(),
                                "STOP"
                        );
                    }
                }
            }
        }

        System.out.println("WS CLOSE: " + session.getId());
    }

    public int viewerCount(int cameraId) {
        Set<WebSocketSession> set = cameraSessions.get(cameraId);
        return set == null ? 0 : set.size();
    }

    public boolean hasViewer(int cameraId) {
        return viewerCount(cameraId) > 0;
    }
}