package pt.up.fe.Messaging;

public class ChunkBackupMessage extends Message {
    String makeHeader(String version, String fileId, int chunkNo, int replicationDeg) {
        return "PUTCHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " " + Integer.toString(replicationDeg) + " ";
    }

    byte[] makeMessage(String header, byte[] data) {
        header += "\r\n\r\n";   //  Append two <CR><LF>

        byte[] dataHeader = header.getBytes();

        return concatByteArrays(dataHeader, data);
    }
}
