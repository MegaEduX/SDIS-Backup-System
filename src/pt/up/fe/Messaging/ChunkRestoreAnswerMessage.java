package pt.up.fe.Messaging;

public class ChunkRestoreAnswerMessage extends Message {
    private String _fileId;
    private int _chunkNo;

    public ChunkRestoreAnswerMessage(String version, String fileId, int chunkNo, byte[] data) {
        _fileId = fileId;
        _chunkNo = chunkNo;

        header = "CHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
        header +=  "\r\n\r\n";   //  Append two <CR><LF>

        byte[] dataHeader = header.getBytes();

        messageData = concatByteArrays(dataHeader, data);
    }

    public String getId() {
        return _fileId;
    }

    public int getChunkNo() {
        return _chunkNo;
    }
}
