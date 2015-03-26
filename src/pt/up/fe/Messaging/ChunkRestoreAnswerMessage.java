package pt.up.fe.Messaging;

public class ChunkRestoreAnswerMessage extends Message {
    public ChunkRestoreAnswerMessage(String version, String fileId, int chunkNo, byte[] data) {
        header = "CHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
        header +=  "\r\n\r\n";   //  Append two <CR><LF>

        byte[] dataHeader = header.getBytes();

        messageData = concatByteArrays(dataHeader, data);
    }
}
