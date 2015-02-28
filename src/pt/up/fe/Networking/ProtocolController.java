package pt.up.fe.Networking;

import java.io.IOException;

/**
 *      Watches over the protocol, controlling the sockets and providing them to the Message Sender and Receiver..
 */

public class ProtocolController {
    Socket MCSocket, MDBSocket, MDRSocket;

    ProtocolController(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) throws IOException {
        MCSocket = new Socket(MCAddress, MCPort);
        MDBSocket = new Socket(MDBAddress, MDBPort);
        MDRSocket = new Socket(MDRAddress, MDRPort);
    }

    Socket getMCSocket() {
        return MCSocket;
    }

    Socket getMDBSocket() {
        return MDBSocket;
    }

    Socket getMDRSocket() {
        return MDRSocket;
    }

    void close() {
        MCSocket.close();
        MDBSocket.close();
        MDRSocket.close();
    }
}
