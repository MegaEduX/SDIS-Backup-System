package pt.up.fe.Messaging;

public class ChunkBackupMessage extends Message {
    public ChunkBackupMessage(String version, String fileId, int chunkNo, int replicationDeg, byte[] data) {
        header = "PUTCHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " " + Integer.toString(replicationDeg) + " ";
        header += "\r\n\r\n";   //  Append two <CR><LF>

        byte[] dataHeader = header.getBytes();

        messageData = concatByteArrays(dataHeader, data);
    }
}
