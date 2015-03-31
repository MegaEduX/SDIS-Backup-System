package pt.up.fe.Networking;

import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.StoredFile;
import pt.up.fe.Messaging.*;

import java.io.FileNotFoundException;
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

    private static final String kMessageTypePutChunk = "PUTCHUNK";
    private static final String kMessageTypeStored = "STORED";
    private static final String kMessageTypeGetChunk = "GETCHUNK";
    private static final String kMessageTypeChunk = "CHUNK";
    private static final String kMessageTypeDelete = "DELETE";
    private static final String kMessageTypeRemoved = "REMOVED";

    public MessageReceiver(ProtocolController controller) {
        pc = controller;
    }

    //  TODO: The return of this function isn't specified well.

    public int parseMessage(String Message) throws IOException {
        String parsedMessage[] = Message.split(" ");

        //  Chunk Backup Protocol - PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>

        if (parsedMessage[0].equals(kMessageTypePutChunk)) {
            String body = parsedMessage[5];

            byte[] data = Arrays.copyOfRange(body.getBytes(Charset.forName("UTF-8")), 4, body.getBytes(Charset.forName("UTF-8")).length);

            if (DataStorage.getInstance().storeChunk(parsedMessage[1], Integer.parseInt(parsedMessage[2]),data)) {
                ChunkBackupAnswerMessage msg = new ChunkBackupAnswerMessage(parsedMessage[1],
                        parsedMessage[2],
                        Integer.parseInt(parsedMessage[3]));

                pc.getMCSocket().send(new String(msg.getMessageData(), "UTF-8"));

                return PUTCHUNK_OK;
            } else {
                return PUTCHUNK_FAIL;
            }
        }

        //  STORED <Version> <FileId> <ChunkNo> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeStored)) {
            System.out.println("STORED Confirmation Message Received.");

            try {
                StoredFile f = DataStorage.getInstance().
                        getStoredDatabase().
                        getFileWithChunkId(parsedMessage[2]);

                f.increaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));
            } catch (FileNotFoundException e) {
                System.out.println("File not found, proceeding anyway...");
            }
        }

        //  Chunk Restore Protocol - GETCHUNK <Version> <FileId> <ChunkNo> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeGetChunk)) {
            System.out.println("Chunk recover requested.");

            ChunkRestoreAnswerMessage msg = new ChunkRestoreAnswerMessage(parsedMessage[1],
                    parsedMessage[2],
                    Integer.parseInt(parsedMessage[3]),
                    DataStorage.getInstance().retrieveChunk(parsedMessage[2],
                            Integer.parseInt(parsedMessage[3])
                    )
            );

            pc.getMDRSocket().send(new String(msg.getMessageData(), "UTF-8"));

            return CHUNK_OK;
        }

        if (parsedMessage[0].equals(kMessageTypeChunk)) {
            //  We should only receive this message if we are subscribed to the MDR.
            //  So, if we receive it, we want it.

            //  TODO: CHUNK - It would be useful if the return of this function returned the received chunk data... Or something.

            System.out.println("Chunk recover done");
        }

        //  File deletion Protocol - DELETE <Version> <FileId> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeDelete)) {
            try {
                DataStorage.getInstance().getStoredDatabase().removeFileWithChunkId(parsedMessage[2]);

                return DELETE_OK;
            } catch (FileNotFoundException e) {
                System.out.println("Chunk not found, ignoring...");
            }

            return ERROR;
        }

        //  Space Reclaiming Protocol - REMOVED <Version> <FileId> <ChunkNo> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeRemoved)) {
            boolean removed = false;

            System.out.println("Updating local counter...");

            for (StoredFile f : DataStorage.getInstance().getStoredDatabase().getStoredFiles()) {
                if (f.getId().equals(parsedMessage[2])) {
                    f.decreaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));

                    removed = true;

                    break;
                }
            }

            if (removed)
                System.out.println("Decreased replication count for " + parsedMessage[2] + ".");
            else
                System.out.println("Unable to decrease replication count for " + parsedMessage[2] + ".");
        }

        return ERROR;
    }
}
