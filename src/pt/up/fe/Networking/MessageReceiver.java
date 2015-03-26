package pt.up.fe.Networking;

import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Messaging.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 *      Maintains the receiving socket and parses incoming messages.
 */

public class MessageReceiver {
    ProtocolController pc;

    //  Variables

    private static final int PUTCHUNK_OK = 0;
    private static final int PUTCHUNK_FAIL = 1;
    private static final int STORED_OK = 2;
    private static final int CHUNK_OK = 3;
    private static final int DELETE_OK = 4;
    private static final int REMOVED_OK = 5;
    private static final int ERROR = 6;


    public MessageReceiver(ProtocolController controller) {
        pc = controller;
    }

    public int parseMessage(String Message) throws IOException {
        String MessageType[] = Message.split(" ");

        //  Chunk Backup Protocol - PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>

        if (MessageType[0].equals("PUTCHUNK")) {
            String body = MessageType[5];

            byte[] data = Arrays.copyOfRange(body.getBytes(Charset.forName("UTF-8")), 4, body.getBytes(Charset.forName("UTF-8")).length);

            if (DataStorage.getInstance().storeChunk(MessageType[1], Integer.parseInt(MessageType[2]),data)) {
                ChunkBackupAnswerMessage msg = new ChunkBackupAnswerMessage(MessageType[1], MessageType[2], Integer.parseInt(MessageType[3]));
                pc.getMCSocket().send(msg.getMessageData().toString());

                /*  ChunkBackupMessage msg = new ChunkBackupMessage();
                byte[] nothing = new byte[]{};
                String header = msg.confirmMessage(MessageType[1],MessageType[2],Integer.parseInt(MessageType[3]));
                msg.makeMessage(header, nothing);
                pc.getMCSocket().send(msg.toString());  */

                return PUTCHUNK_OK;
            } else {
                return PUTCHUNK_FAIL;
            }
        }

        if (MessageType[0].equals("STORED")) {
            System.out.println("STORED Confirmation Message Received.");
        }

        //Chunk Restore Protocol - GETCHUNK <Version> <FileId> <ChunkNo> <CRLF><CRLF>
        if (MessageType[0].equals("GETCHUNK")) {
            System.out.println("Chunk recover requested.");

            ChunkRestoreAnswerMessage msg = new ChunkRestoreAnswerMessage(MessageType[1],
                    MessageType[2],
                    Integer.parseInt(MessageType[3]),
                    DataStorage.getInstance().retrieveChunk(MessageType[2],
                            Integer.parseInt(MessageType[3])
                    )
            );

            pc.getMDRSocket().send(msg.getMessageData().toString());

            /*  ChunkRestoreMessage msg = new ChunkRestoreMessage();

            String header = msg.confirmMessage(MessageType[1],MessageType[2],Integer.parseInt(MessageType[3]));
            msg.makeMessage(header,DataStorage.getInstance().retrieveChunk(MessageType[2],Integer.parseInt(MessageType[3])));
            pc.getMCSocket().send(msg.toString());  */

            return CHUNK_OK;
        }

        if (MessageType.equals("CHUNK")) {
            System.out.println("Chunk recover done");
        }

        //  File deletion Protocol - DELETE <Version> <FileId> <CRLF><CRLF>

        if (MessageType.equals("DELETE")) {
            //  DataStorage.getInstance().deleteChunk(MessageType[2],Integer.parseInt(MessageType[3]));
            System.out.println("Delete chunk requested.");
            return DELETE_OK;
        }

        //  Space Reclaiming Protocol

        if (MessageType.equals("REMOVED")) {
            System.out.println("Updating local counter...");
        }

        return ERROR;
    }
}
