package com.control.espcar.udp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FrameBuffer {

    public int totalChunks;
    public Map<Integer, byte[]> chunks = new ConcurrentHashMap<>();
    public long lastUpdate;

    public boolean isComplete() {
        return chunks.size() == totalChunks;
    }

    public byte[] buildFrame() {

        try {

            int totalSize = chunks.values()
                    .stream()
                    .mapToInt(b -> b.length)
                    .sum();

            byte[] frame = new byte[totalSize];

            int offset = 0;

            for (int i = 0; i < totalChunks; i++) {

                byte[] chunk = chunks.get(i);

                if (chunk == null) {
                    return null;
                }

                System.arraycopy(chunk, 0, frame, offset, chunk.length);
                offset += chunk.length;
            }

            return frame;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}