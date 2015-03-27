package pt.up.fe.Messaging;

public class SpaceReclaimMessage extends Message {
    public SpaceReclaimMessage(String version, String fileId, int chunkNo) {
        header = "REMOVED " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
        header += "\r\n\r\n";   //  Append two <CR><LF>

        messageData = header.getBytes();    //  REMOVED doesn't have a body.
    }
}

