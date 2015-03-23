package pt.up.fe.Networking;

import pt.up.fe.Messaging.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

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

    void sendMessage(Message m) throws IOException {
        if(m.getHeader().equals("PUTCHUNK")){
            pc.getMDBSocket().send(m.toString());
        }
        else if(m.getHeader().equals("STORED")){
            pc.getMCSocket().send(m.toString());
        }
        else if(m.getHeader().equals("GETCHUNK")){
            pc.getMCSocket().send(m.toString());
        }
        else if(m.getHeader().equals("CHUNK")){
            pc.getMDRSocket().send(m.toString());
        }
        else if(m.getHeader().equals("DELETE")){
            pc.getMCSocket().send(m.toString());
        }
        else if(m.getHeader().equals("REMOVED")){
            pc.getMCSocket().send(m.toString());
        }
    }
}
