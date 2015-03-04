package pt.up.fe.Networking;

import java.io.IOException;

/**
 *      Watches over the protocol, controlling the sockets and providing them to the Message Sender and Receiver..
 */

public class ProtocolController {
    UDPUnicast MCSocket, MDBSocket, MDRSocket;

    ProtocolController(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) throws IOException {
        MCSocket = new UDPMulticast(MCAddress, MCPort);
        MDBSocket = new UDPUnicast(MDBAddress, MDBPort);
        MDRSocket = new UDPUnicast(MDRAddress, MDRPort);
    }

    UDPUnicast getMCSocket() {
        return MCSocket;
    }

    UDPUnicast getMDBSocket() {
        return MDBSocket;
    }

    UDPUnicast getMDRSocket() {
        return MDRSocket;
    }

    void close() {
        MCSocket.close();
        MDBSocket.close();
        MDRSocket.close();
    }
}
