package pt.up.fe.Threading;

import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Observable;

public class MDR extends Observable implements Runnable {
    private ProtocolController pc;
    private MessageReceiver rec;

    private UDPMulticast mdrSocket;

    volatile boolean acceptPackets = true;

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

            acceptPackets = false;

            return;
        }

        while (true) {
            try {
                DatagramPacket packet = mdrSocket.receive();

                if (!acceptPackets)
                    continue;   //  Drop everything we don't need.

                try {
                    rec.addObserver((Observable obj, Object arg) -> {
                        setChanged();
                        notifyObservers(arg);
                    });

                    byte[] fixedArray = Arrays.copyOf(packet.getData(), packet.getLength());

                    rec.parseRawMessageMDR(fixedArray);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {

            }
        }
    }

    public void setActive(boolean active) throws IOException {
        acceptPackets = active;
    }

    public void terminate() throws IOException {
        acceptPackets = false;

        mdrSocket.leave();
        mdrSocket.close();
    }
}
