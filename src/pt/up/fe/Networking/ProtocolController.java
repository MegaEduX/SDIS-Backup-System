package pt.up.fe.Networking;

import java.io.IOException;

/**
 *      Watches over the protocol, controlling the sockets and providing them to the Message Sender and Receiver..
 */

public class ProtocolController {
    UDPMulticast MCSocket, MDBSocket, MDRSocket;

    ProtocolController(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) throws IOException {
        MCSocket = new UDPMulticast(MCAddress, MCPort);
        MDBSocket = new UDPMulticast(MDBAddress, MDBPort);
        MDRSocket = new UDPMulticast(MDRAddress, MDRPort);
    }

    UDPMulticast getMCSocket() {
        return MCSocket;
    }

    UDPMulticast getMDBSocket() {
        return MDBSocket;
    }

    UDPMulticast getMDRSocket() {
        return MDRSocket;
    }

    void close() {
        MCSocket.close();
        MDBSocket.close();
        MDRSocket.close();
    }
}
