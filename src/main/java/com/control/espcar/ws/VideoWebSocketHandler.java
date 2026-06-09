package com.control.espcar.ws;


import com.control.espcar.entity.DeviceInfo;
import com.control.espcar.repository.DeviceInfoRepository;
import com.control.espcar.utils.mqtt.MqttService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VideoWebSocketHandler extends BinaryWebSocketHandler {

    // streamId -> sessions
    private final Map<String, Set<WebSocketSession>> streamSessions =
            new ConcurrentHashMap<>();

    // sessionId -> streamId
    private final Map<String, String> sessionStreamMap =
            new ConcurrentHashMap<>();

    // streamId đang chạy
    private final Set<String> activeStreams =
            ConcurrentHashMap.newKeySet();

    private final MqttService mqttService;

    private final DeviceInfoRepository deviceInfoRepository;

    public VideoWebSocketHandler(MqttService mqttService, DeviceInfoRepository deviceInfoRepository) {
        this.mqttService = mqttService;
        this.deviceInfoRepository = deviceInfoRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("WS CONNECT: " + session.getId());
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {

//        String streamId = message.getPayload().trim();

        try {
            ObjectMapper mapper = new ObjectMapper();

            WsMessage wsMessage =
                    mapper.readValue(message.getPayload(), WsMessage.class);

            String oldStream = sessionStreamMap.get(session.getId());

            if("CAR_CONTROL".equals(wsMessage.getType())){

                handleCarControl(wsMessage);
                return;
            }
            // remove khỏi stream cũ nếu đổi stream
            if (oldStream != null && !oldStream.equals(wsMessage.getStreamId())) {

                Set<WebSocketSession> oldSet = streamSessions.get(oldStream);

                if (oldSet != null) {
                    oldSet.remove(session);

                    // check STOP stream cũ
                    if (oldSet.isEmpty()) {
                        stopStreamIfNeeded(wsMessage.getStreamId());
                    }
                }
            }

            // add session vào stream mới
            streamSessions
                    .computeIfAbsent(wsMessage.getStreamId(), k -> ConcurrentHashMap.newKeySet())
                    .add(session);

            sessionStreamMap.put(session.getId(), wsMessage.getStreamId());

            session.sendMessage(new TextMessage("CONNECTED:" + wsMessage.getStreamId()));

            System.out.println("VIEW STREAM " + wsMessage.getStreamId());

            // START STREAM khi từ 0 → 1
            if (viewerCount(wsMessage.getStreamId()) == 1) {
                startStreamIfNeeded(wsMessage.getStreamId());
            }

        } catch (Exception e) {
            try {
                session.sendMessage(new TextMessage("ERROR"));
            } catch (Exception ignored) {}

            e.printStackTrace();
        }
    }

    // =========================
    // STREAM CONTROL
    // =========================

    private void startStreamIfNeeded(String streamId) {

        if (activeStreams.add(streamId)) {
            DeviceInfo deviceInfo = deviceInfoRepository.findById(Long.parseLong(streamId)).orElse(null);
            if(deviceInfo == null) return;
            System.out.println("START STREAM " + streamId);

            mqttService.publish(
                    "devices/" + deviceInfo.getSerialNumber() + "/control",
                    """
                    {"cmd":"START_STREAM"}
                    """
            );
        }
    }

    private void stopStreamIfNeeded(String streamId) {

        if (viewerCount(streamId) == 0 && activeStreams.remove(streamId)) {

            DeviceInfo deviceInfo = deviceInfoRepository.findById(Long.parseLong(streamId)).orElse(null);
            if(deviceInfo == null) return;
            System.out.println("STOP STREAM " + streamId);

            mqttService.publish(
                    "devices/" + deviceInfo.getSerialNumber() + "/control",
                    """
                    {"cmd":"STOP_STREAM"}
                    """
            );

            streamSessions.remove(streamId);
        }
    }

    // =========================
    // BROADCAST FRAME
    // =========================

    public void broadcast(String streamId, byte[] imageBytes) {

        Set<WebSocketSession> sessions = streamSessions.get(streamId);

        if (sessions == null || sessions.isEmpty()) return;

        sessions.removeIf(s -> !s.isOpen());

        BinaryMessage msg = new BinaryMessage(imageBytes);

        for (WebSocketSession s : sessions) {
            try {
                s.sendMessage(msg);
            } catch (Exception e) {
                sessions.remove(s);
            }
        }
    }

    // =========================
    // CLEANUP SESSION
    // =========================

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        String streamId = sessionStreamMap.remove(session.getId());

        if (streamId == null) return;

        Set<WebSocketSession> sessions = streamSessions.get(streamId);

        if (sessions != null) {

            sessions.remove(session);

            System.out.println("WS CLOSE: " + session.getId());

            // chỉ STOP khi thật sự hết viewer
            stopStreamIfNeeded(streamId);
        }
        sentControlDevice(streamId, "DESTROY");

    }

    // =========================
    // UTILS
    // =========================

    public int viewerCount(String streamId) {
        Set<WebSocketSession> sessions = streamSessions.get(streamId);
        return sessions == null ? 0 : sessions.size();
    }

    public boolean hasViewer(String streamId) {
        return viewerCount(streamId) > 0;
    }

    private void handleCarControl(WsMessage wsMessage){
        String caseCmd = wsMessage.getCmd();
        String serialNumber = wsMessage.getDeviceId();
        switch (caseCmd){
            case "HEADLIGHT_ON","BLINK_ON", "BLINK_OFF", "HEADLIGHT_OFF":
                sentControlDevice(wsMessage.getStreamId(), wsMessage.getCmd());
            break;
        }
    }

    private void sentControlDevice(String streamId, String cmd) {

        DeviceInfo deviceInfo = deviceInfoRepository.findById(Long.parseLong(streamId)).orElse(null);
        if(deviceInfo == null) return;
        System.out.println("STOP STREAM " + streamId);

        String cmdQuery = "{\"cmd\":\""+cmd+"\"}";
        mqttService.publish(
                "devices/" + deviceInfo.getSerialNumber() + "/control",
                cmdQuery
        );

        streamSessions.remove(streamId);

    }

}