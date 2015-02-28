package pt.up.fe.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 *      Multicast Socket
 */

public class Socket {
    MulticastSocket socket;
    InetAddress multicastGroup;

    public Socket(String socketAddress, int port) throws IOException {
        socket = new java.net.MulticastSocket(port);
        multicastGroup = InetAddress.getByName(socketAddress);

        socket.joinGroup(multicastGroup);
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
        try {
            socket.leaveGroup(multicastGroup);
        } catch (IOException exc) {
            System.out.println("An error has occurred while leaving the multicast group. Closing the socket anyway...");
        }

        socket.close();
    }
}
