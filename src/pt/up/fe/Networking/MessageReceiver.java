package pt.up.fe.Networking;


import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Messaging.ChunkRestoreMessage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 *      Maintains the receiving socket and parses incoming messages.
 */

public class MessageReceiver {
    ProtocolController pc;

    MessageReceiver(ProtocolController controller) {
        pc = controller;
    }

    public boolean parseMessage(String Message) throws IOException {
        String MessageType[] = Message.split(" ");

        //Chunk Backup Protocol - PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
        if(MessageType[0].equals("PUTCHUNK")){
            String body = MessageType[5];
            byte[] data;

            data = Arrays.copyOfRange(body.getBytes(Charset.forName("UTF-8")), 4, body.getBytes(Charset.forName("UTF-8")).length);

            return DataStorage.getInstance().storeChunk(MessageType[1], Integer.parseInt(MessageType[2]),data);
            }
        if(MessageType[0].equals("STORED")){
        }

        //Chunk Restore Protocol - GETCHUNK <Version> <FileId> <ChunkNo> <CRLF><CRLF>
        if(MessageType[0].equals("GETCHUNK")){
            DataStorage.getInstance().retrieveChunk(MessageType[2],Integer.parseInt(MessageType[3]));
            //Como dizer qual a data a enviar ??


        }
        if(MessageType.equals("CHUNK")){
        }

        //File deletion Protocol
        if(MessageType.equals("DELETE")){

        }

        //Space Reclaiming Protocol
        if(MessageType.equals("REMOVED")){

        }


    }
}
