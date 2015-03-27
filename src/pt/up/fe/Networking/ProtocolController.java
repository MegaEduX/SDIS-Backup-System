package pt.up.fe.Networking;

import java.io.IOException;

/**
 *      Watches over the protocol, controlling the sockets and providing them to the Message Sender and Receiver..
 */

public class ProtocolController {
    UDPMulticast MCSocket, MDBSocket, MDRSocket;

    public ProtocolController(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) throws IOException {
        MCSocket = new UDPMulticast(MCAddress, MCPort);
        MDBSocket = new UDPMulticast(MDBAddress, MDBPort);
        MDRSocket = new UDPMulticast(MDRAddress, MDRPort);
    }

    public UDPMulticast getMCSocket() {
        return MCSocket;
    }

    public UDPMulticast getMDBSocket() {
        return MDBSocket;
    }

    public UDPMulticast getMDRSocket() {
        return MDRSocket;
    }

    public void close() {
        MCSocket.close();
        MDBSocket.close();
        MDRSocket.close();
    }
}
