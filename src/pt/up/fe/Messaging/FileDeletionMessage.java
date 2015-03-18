package pt.up.fe.Messaging;

public class FileDeletionMessage extends Message {
    String makeHeader(String version, String fileId) {
        return "GETCHUNK " + version + " " + fileId + " ";
    }

    byte[] makeMessage(String header, byte[] data) {
        header += "\r\n\r\n";   //  Append two <CR><LF>

        byte[] dataHeader = header.getBytes();

        return concatByteArrays(dataHeader, data);
    }
}
