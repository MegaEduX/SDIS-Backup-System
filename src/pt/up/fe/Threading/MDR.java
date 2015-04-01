package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Observable;

public class MDR extends Observable implements Runnable {
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

        try {
            mdrSocket.join();
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
                DatagramPacket packet = mdrSocket.receive();

                String outStr = new String(packet.getData(), 0, packet.getLength());

                try {
                    rec.addObserver((Observable obj, Object arg) -> {
                        setChanged();
                        notifyObservers(arg);
                    });

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

        mdrSocket.leave();
        mdrSocket.close();
    }
}
