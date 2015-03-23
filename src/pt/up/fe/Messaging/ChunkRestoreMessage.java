package pt.up.fe.Messaging;

public class ChunkRestoreMessage extends Message {
    public String makeHeader(String version, String fileId, int chunkNo) {
        return "GETCHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
    }

    public byte[] makeMessage(String header, byte[] data) {
        this.header = header;
        header += "\r\n\r\n";   //  Append two <CR><LF>

        byte[] dataHeader = header.getBytes();

        this.msg = concatByteArrays(dataHeader, data);
        return this.msg;
    }

    public String confirmMessage(String version, String fileId, int chunkNo) {
        return "CHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
    }



}
