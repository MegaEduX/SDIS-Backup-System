package pt.up.fe.Networking;

import pt.up.fe.Messaging.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 *      Maintains the sending socket and sends messages to the multicast address.
 */

public class MessageSender {

    public class UnknownMessageException extends Exception {
        public UnknownMessageException() {
            super();
        }
    }

    ProtocolController pc;

    public MessageSender(ProtocolController controller) {
        pc = controller;
    }

    /*
     *      Parse the message and send it using the Protocol Controller.
     */

    public void sendMessage(Message m) throws IOException, UnknownMessageException {
        String messageType = m.getHeader().split(" ")[0];

        if (messageType.equals("PUTCHUNK"))
            pc.getMDBSocket().send(m.toString());

        else if (messageType.equals("STORED"))
            pc.getMCSocket().send(m.toString());

        else if (messageType.equals("GETCHUNK"))
            pc.getMCSocket().send(m.toString());

        else if (messageType.equals("CHUNK"))
            pc.getMDRSocket().send(m.toString());

        else if (messageType.equals("DELETE"))
            pc.getMCSocket().send(m.toString());

        else if (messageType.equals("REMOVED"))
            pc.getMCSocket().send(m.toString());

        else
            throw new UnknownMessageException();

    }
}
