package pt.up.fe.Messaging;

public class FileDeletionMessage extends Message {
    public FileDeletionMessage(String version, String fileId) {
        header = "DELETE " + version + " " + fileId + " ";

        header += "\r\n\r\n";   //  Append two <CR><LF>

        messageData = header.getBytes();    //  DELETE doesn't have a body.
    }
}
