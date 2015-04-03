package pt.up.fe.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;

public class UDPMulticast {
    MulticastSocket socket;

    InetAddress address;
    int port;

    public UDPMulticast(String addr, int port) throws IOException {
        this.address = InetAddress.getByName(addr);
        this.port = port;

        socket = new MulticastSocket(port);

        socket.setLoopbackMode(true);  //  true to disable - WTF?!
    }

    public void join() throws IOException {
        socket.joinGroup(this.address);
    }

    public DatagramPacket receive() {
        byte[] buf = new byte[72000];   //  More than enough, but we aren't RAM-constrained.

        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(packet);
            return packet;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean send(String buf) {
        byte[] bytes = buf.getBytes();

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);

        try {
            socket.send(packet);

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    public boolean sendRaw(byte[] buf) {
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);

        try {
            socket.send(packet);

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    public void leave() throws IOException {
        socket.leaveGroup(this.address);
    }

    public void close() {
        socket.close();
    }
}
