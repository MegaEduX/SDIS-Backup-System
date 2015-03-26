package pt.up.fe.Messaging;

public class ChunkBackupAnswerMessage extends Message {
    public ChunkBackupAnswerMessage(String version, String fileId, int chunkNo) {
        header = "STORED " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
        header +=  "\r\n\r\n";   //  Append two <CR><LF>

        messageData = header.getBytes();    //  STORED doesn't have a body.
    }
}
