package pt.up.fe.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPUnicast {
    DatagramSocket socket;

    public UDPUnicast(int port) throws IOException {
        socket = new DatagramSocket(port);
    }

    public DatagramPacket receive() {
        byte[] buf = new byte[256];

        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(packet);
            return packet;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean send(String buf, InetAddress address, int port) {
        byte[] bytes = buf.getBytes();

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);

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
