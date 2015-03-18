package pt.up.fe.Messaging;

public class SpaceReclaimMessage extends Message {
    String makeHeader(String version, String fileId, int chunkNo) {
        return "REMOVED " + version + " " + fileId + " " + Integer.toString(chunkNo) + " ";
    }

    byte[] makeMessage(String header, byte[] data) {
        header += "\r\n\r\n";   //  Append two <CR><LF>

        byte[] dataHeader = header.getBytes();

        return concatByteArrays(dataHeader, data);
    }
}

