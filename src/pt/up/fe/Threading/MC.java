package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;

public class MC implements Runnable {
    private ProtocolController pc;
    private MessageReceiver rec;

    private UDPMulticast mcSocket;

    volatile boolean running = true;

    public MC(ProtocolController protocolController, MessageReceiver messageReceiver) {
        pc = protocolController;
        rec = messageReceiver;
    }

    @Override public void run() {
        mcSocket = pc.getMCSocket();

        while (true) {
            if (!running)
                return;

            try {
                DatagramPacket packet = mcSocket.receive();

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

    public void terminate() {
        running = false;

        mcSocket.close();
    }
}
