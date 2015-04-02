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

        String outputMessage = new String(m.getMessageData(), "UTF-8");

        switch (messageType) {

            case "PUTCHUNK":
                pc.getMDBSocket().sendRaw(m.getMessageData());

                break;

            case "STORED":
                pc.getMCSocket().send(outputMessage);

                break;

            case "GETCHUNK":
                pc.getMCSocket().send(outputMessage);

                break;

            case "CHUNK":
                pc.getMDRSocket().send(outputMessage);

                break;

            case "DELETE":
                pc.getMCSocket().send(outputMessage);

                break;

            case "REMOVED":
                pc.getMCSocket().send(outputMessage);

                break;

            default:
                throw new UnknownMessageException();

        }
    }
}
