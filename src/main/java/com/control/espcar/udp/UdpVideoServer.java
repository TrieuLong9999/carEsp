package com.control.espcar.udp;

import com.control.espcar.ws.VideoWebSocketHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class UdpVideoServer {

    private final VideoWebSocketHandler socketHandler;

    private final Map<String, FrameBuffer> frameBuffers = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() {
        System.out.println("🔥 UDP SERVER INIT CALLED");
        Thread t = new Thread(this::runUdp);
        t.setDaemon(true);
        t.start();
        System.out.println("[UDP] Server started on port 5000");
    }

    private void runUdp() {

        try (DatagramSocket socket = new DatagramSocket(5000)) {

            byte[] buffer = new byte[2048];

            while (true) {

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("RAW PACKET LEN = " + packet.getLength());
                processPacket(packet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processPacket(DatagramPacket packet) {
        System.out.println("RAW PACKET LEN = " + packet.getLength());
        try {

            int len = packet.getLength();

            if (len < 7) {
                System.out.println("[UDP] invalid packet size: " + len);
                return;
            }

            ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, len);
            bb.order(ByteOrder.BIG_ENDIAN);

            int cameraId = bb.get() & 0xFF;
            int frameId = bb.getShort() & 0xFFFF;
            int totalChunks = bb.getShort() & 0xFFFF;
            int chunkIndex = bb.getShort() & 0xFFFF;

            byte[] chunk = new byte[len - 7];
            bb.get(chunk);

            String key = cameraId + "_" + frameId;

            System.out.printf(
                    "[UDP] cam=%d frame=%d chunk=%d/%d size=%d\n",
                    cameraId, frameId, chunkIndex, totalChunks, chunk.length
            );

            FrameBuffer fb = frameBuffers.computeIfAbsent(key, k -> {
                FrameBuffer f = new FrameBuffer();
                f.totalChunks = totalChunks;
                f.chunks = new ConcurrentHashMap<>();
                f.lastUpdate = System.currentTimeMillis();
                return f;
            });

            fb.chunks.put(chunkIndex, chunk);
            fb.lastUpdate = System.currentTimeMillis();

            System.out.println("[UDP] received chunks: " + fb.chunks.size());

            if (fb.isComplete()) {

                System.out.println("[UDP] FRAME COMPLETE: " + key);

                byte[] image = fb.buildFrame();

                if (image != null && image.length > 0) {
                    socketHandler.broadcast(cameraId, image);
                    System.out.println("[UDP] BROADCAST OK");
                } else {
                    System.out.println("[UDP] frame build failed");
                }

                frameBuffers.remove(key);
            }

            cleanup();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {

        long now = System.currentTimeMillis();

        frameBuffers.entrySet().removeIf(e ->
                now - e.getValue().lastUpdate > 3000
        );
    }
}