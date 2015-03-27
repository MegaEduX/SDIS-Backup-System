package pt.up.fe.Messaging;

public class ChunkRestoreMessage extends Message {
    public ChunkRestoreMessage(String version, String fileId, int chunkNo) {
        header = "GETCHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
        header +=  "\r\n\r\n";   //  Append two <CR><LF>

        messageData = header.getBytes();    //  GETCHUNK doesn't have a body.
    }
}
