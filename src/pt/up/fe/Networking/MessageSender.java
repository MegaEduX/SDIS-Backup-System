package pt.up.fe.Networking;

import pt.up.fe.Messaging.Message;

/**
 *      Maintains the sending socket and sends messages to the multicast address.
 */

public class MessageSender {
    ProtocolController pc;

    MessageSender(ProtocolController controller) {
        pc = controller;
    }

    /*
     *      Parse the message and send it using the Protocol Controller.
     */

    void sendMessage(Message m) {

    }
}
