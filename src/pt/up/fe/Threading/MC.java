package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

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

        try {
            mcSocket.join();
        } catch (SocketException e) {
            //  We have, mostlikely, already joined it.
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
                DatagramPacket packet = mcSocket.receive();

                if (!running)
                    return;

                String outStr = new String(packet.getData(), 0, packet.getLength());

                try {
                    rec.setSender(packet.getAddress());
                    rec.parseMessage(outStr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {

            }
        }
    }

    public void terminate() throws IOException {
        running = false;

        mcSocket.leave();
        mcSocket.close();
    }
}
