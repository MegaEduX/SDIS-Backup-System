package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;

public class MDR implements Runnable {
    private ProtocolController pc;
    private MessageReceiver rec;

    volatile boolean running = true;

    public MDR(ProtocolController protocolController, MessageReceiver messageReceiver) {
        pc = protocolController;
        rec = messageReceiver;
    }

    @Override public void run() {
        UDPMulticast mdrSocket = pc.getMDRSocket();

        while (true) {
            if (!running)
                return;

            DatagramPacket packet = mdrSocket.receive();

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
