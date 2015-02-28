package pt.up.fe.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *      Socket (from Lab 01)
 */

public class Socket {
    DatagramSocket socket;

    public Socket(int port) throws IOException {
        socket = new DatagramSocket(port);
    }

    public DatagramPacket receive(int packetLength) {
        byte[] buf = new byte[packetLength];

        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(packet);

            return packet;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean send(String buf, InetAddress address, int port) {
        DatagramPacket packet = new DatagramPacket(buf.getBytes(), buf.length(), address, port);

        try {
            socket.send(packet);

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void close() {
        socket.close();
    }
}
