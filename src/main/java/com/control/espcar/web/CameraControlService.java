package com.control.espcar.web;

import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
public class CameraControlService {
    /**
     * IP từng camera
     */
    public void sendCommand(
            String cameraIp,
            int port,
            String command
    ) {

        try {

            DatagramSocket socket =
                    new DatagramSocket();

            byte[] data =
                    command.getBytes();

            DatagramPacket packet =
                    new DatagramPacket(
                            data,
                            data.length,
                            InetAddress.getByName(cameraIp),
                            port
                    );

            socket.send(packet);

            socket.close();

            System.out.println(
                    "SEND " + command
                            + " -> "
                            + cameraIp
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
