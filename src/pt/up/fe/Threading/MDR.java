package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;

public class MDR implements Runnable {
    private ProtocolController pc;
    private MessageReceiver rec;

    private UDPMulticast mdrSocket;

    volatile boolean running = true;

    public MDR(ProtocolController protocolController, MessageReceiver messageReceiver) {
        pc = protocolController;
        rec = messageReceiver;
    }

    @Override public void run() {
        mdrSocket = pc.getMDRSocket();

        while (true) {
            if (!running)
                return;

            try {
                DatagramPacket packet = mdrSocket.receive();

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

        mdrSocket.close();
    }
}
