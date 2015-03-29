package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;

public class MC implements Runnable {
    private ProtocolController pc;
    private MessageReceiver rec;

    volatile boolean running = true;

    public MC(ProtocolController protocolController, MessageReceiver messageReceiver) {
        pc = protocolController;
        rec = messageReceiver;
    }

    @Override public void run() {
        UDPMulticast mcSocket = pc.getMCSocket();

        while (true) {
            if (!running)
                return;

            DatagramPacket packet = mcSocket.receive();

            String outStr = new String(packet.getData(), 0, packet.getLength());

            try {
                rec.parseMessage(outStr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void terminate() {
        running = false;
    }
}
