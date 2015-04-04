package pt.up.fe.Messaging;

public class ChunkRestoreAnswerMessage extends Message {
    private String _fileId;
    private int _chunkNo;
    private byte[] _rawData;

    public ChunkRestoreAnswerMessage(String version, String fileId, int chunkNo, byte[] data) {
        _fileId = fileId;
        _chunkNo = chunkNo;
        _rawData = data;

        header = "CHUNK " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
        header +=  "\r\n\r\n";   //  Append two <CR><LF>

        messageData = concatByteArrays(header.getBytes(), data);
    }

    public String getId() {
        return _fileId;
    }

    public int getChunkNo() {
        return _chunkNo;
    }

    public byte[] getRawData() {
        return _rawData;
    }
}
