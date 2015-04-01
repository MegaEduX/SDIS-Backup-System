package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;

public class MDB implements Runnable {
    private ProtocolController pc;
    private MessageReceiver rec;

    private UDPMulticast mdbSocket;

    volatile boolean running = true;

    public MDB(ProtocolController protocolController, MessageReceiver messageReceiver) {
        pc = protocolController;
        rec = messageReceiver;
    }

    @Override public void run() {
        mdbSocket = pc.getMDBSocket();

        try {
            mdbSocket.join();
        } catch (IOException e) {
            System.out.println("Multicast Join Failed.");

            e.printStackTrace();

            running = false;

            return;
        }

        while (true) {
            if (!running)
                return;

            try {
                DatagramPacket packet = mdbSocket.receive();

                String outStr = new String(packet.getData(), 0, packet.getLength());

                try {
                    rec.parseMessage(outStr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {

            }
        }
    }

    public void terminate() throws IOException {
        running = false;

        mdbSocket.leave();
        mdbSocket.close();
    }
}
